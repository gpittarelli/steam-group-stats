(ns analytics.core
  (:require [clojure.data.json :as json]
            [ring.util.response :refer [response]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.transit :refer [wrap-transit-response
                                             wrap-transit-params]]
            [compojure.core :refer [GET defroutes context]]
            [compojure.route :as route]
            [org.httpkit.server :refer [run-server]]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [clojure-watch.core :refer [start-watch]])
  (:gen-class))

(defn- json-read [str]
  (json/read-str str :key-fn keyword))

(defn- analyze [data]
  (let [players (vals (:players data))
        games (->> players
                   (filter #(contains? % :gameName))
                   (map :gameName)
                   frequencies)]
    {:online (:membersOnline data)
     :total (:memberCount data)
     :ingame (:membersInGame data)
     :time (:time data)
     :player-states (frequencies (map :onlineState players))
     :profile-states (frequencies (map :privacyState players))
     :visibility-states (frequencies (map :visibilityState players))
     :limited-accounts (frequencies (map :isLimitedAccount players))
     :trade-bans (frequencies (map :tradeBanState players))
     :primary-groups (->> players
                          (filter #(contains? % :groupName))
                          (map :groupName)
                          frequencies)
     :games games
     :intf2 (get games "Team Fortress 2")
     :vac (count (filter :vacBanned players))
     :vacced (vec (filter :vacBanned players))}))

(def ^:private results (atom {}))

(defroutes app-routes
  (GET "/" [] (slurp "src/html/index.html"))
  (route/files "/" {:root "target/public"})
  (GET "/main.js" [] (slurp "target/public/javascript/main.js"))
  (GET "/main.js.map" [] (slurp "target/public/javascript/main.js.map"))
  (context "/api" []
    (GET "/data" [] (response (vals @results))))
  (route/not-found "Not found"))

(def ^:private app
  (-> app-routes
      (wrap-transit-params {:opts {}})
      (wrap-transit-response {:encoding :json, :opts {}})))

(defn- usage []
  (println "Expected Arguments: <data-dir>"))

(defn -main [& [data-dir-path & args]]
  (when-not data-dir-path
    (usage)
    (System/exit 1))

  (let [data-dir (fs/file data-dir-path)]
    (when-not (fs/directory? data-dir)
      (usage)
      (println "Error:" data-dir-path "is not a directory")
      (System/exit 2))

    (let [analyze-file (comp analyze json-read slurp)
          analysis (->> (fs/glob data-dir "*.json")
                        (map analyze-file)
                        (map #(-> [(:time %) %]))
                        (into {}))]
      (println "analysis of" (count (vals analysis)) "samples done.")
      (reset! results analysis)

      (start-watch [{:path data-dir-path
                     :event-types [:create :modify]
                     :callback
                     (fn [event filename]
                       (when (.endsWith filename ".json")
                         (let [new-data (analyze-file filename)]
                           (println "Load new sample data from" filename
                                    "time:" (:time new-data))
                           (swap! results assoc (:time new-data) new-data))))}])

      (run-server #'app {:port 8080})
      (println "listening on 8080"))))
