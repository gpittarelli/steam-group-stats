(ns analytics.core
  (:require [clojure.data.json :as json]
            [ring.util.response :refer [response]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.transit :refer [wrap-transit-response
                                             wrap-transit-params]]
            [compojure.core :refer [GET defroutes context]]
            [compojure.route :as route]
            [org.httpkit.server :refer [run-server]])
  (:gen-class))

(defn- json-read [str]
  (json/read-str str :key-fn keyword))

(defn- analyze [data]
  (let [players (vals (:players data))]
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
     :games (->> players
                 (filter #(contains? % :gameName))
                 (map :gameName)
                 frequencies)
     :vac (count (filter :vacBanned players))
     :vacced (vec (filter :vacBanned players))}))

(def ^:private results (atom {}))

(defroutes app-routes
  (GET "/" [] (slurp "src/html/index.html"))
  (route/files "/" {:root "target/public"})
  (GET "/main.js" [] (slurp "target/public/javascript/main.js"))
  (GET "/main.js.map" [] (slurp "target/public/javascript/main.js.map"))
  (context "/api" []
    (GET "/data" [] (response @results)))
  (route/not-found "Not found"))

(def app
  (-> app-routes
      (wrap-transit-params {:opts {}})
      (wrap-transit-response {:encoding :json, :opts {}})))

(defn -main [& args]
  (let [data (map (comp json-read slurp) args)
        analysis (mapv analyze data)]
    (println "analysis of " (count analysis) "samples done.")
    (reset! results analysis)
    (run-server #'app {:port 8080})
    (println "listening on 8080")))
