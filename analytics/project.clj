(defproject analytics "0.1.0-SNAPSHOT"
  :description "Analytics for the steam group statistics"
  :url "http://group.tf2stadium.gcommer.com"
  :license {:name "GPL - v 3.0"
            :url "http://www.gnu.org/licenses/gpl-3.0.en.html"
            :distribution :repo}
  :main analytics.core
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [http-kit "2.1.18"]
                 [ring "1.4.0"]
                 [ring-transit "0.1.4"]
                 [compojure "1.4.0"]
                 [incanter "1.5.6"]
                 [org.clojure/data.json "0.2.6"]
                 [me.raynes/fs "1.4.6"]

                 [org.clojure/clojurescript "1.7.170"]
                 [org.clojure/core.async "0.2.374"]
                 [cljs-http "0.1.39"]
                 [cljsjs/d3 "3.5.7-1"]
                 [cljsjs/dimple "2.1.2-0"]]

  :source-paths ["src/clj"]

  :plugins [[lein-cljsbuild "1.1.2"]]

  :prep-tasks ["compile" ["cljsbuild" "once"]]
  :cljsbuild
  {:builds
   {:dev
    {:source-paths ["src/cljs"]
     :jar true
     :compiler
     {:output-to "target/public/javascript/main-debug.js"
      :output-dir "target/public/javascript/out-debug/"
      :source-map "target/public/javascript/main-debug.js.map"
      :optimizations :whitespace}}

    :prod
    {:source-paths ["src/cljs"]
     :jar true
     :compiler
     {:output-to "target/public/javascript/main.js"
      :output-dir "target/public/javascript/out/"
      :source-map "target/public/javascript/main.js.map"
      :optimizations :advanced}}}}


  :profiles {:uberjar {:aot :all}})
