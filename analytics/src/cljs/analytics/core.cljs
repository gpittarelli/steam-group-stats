(ns analytics.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljsjs.d3]
            [cljsjs.dimple]
            [cljs-http.client :as http]
            [cljs.core.async :refer [put! chan <!]]
            [goog.events :as events]))

(enable-console-print!)

(defn listen [el type]
  (let [out (chan)]
    (events/listen el type
                   (fn [e] (put! out e)))
        out))

(defn- epoch->js-epoch [x] (* 1000 x))

(defn- add-series [chart out-axis basis-axis key title]
  (let [axis (.addMeasureAxis chart out-axis key)
        series (.addSeries chart title dimple.plot.line
                           #js [basis-axis axis])]
    (set! (.-title axis) title)
    (set! (.-lineMarkers series) true)
    axis))

(defn render [svg data']
  (let [data (map #(update-in % [:time] epoch->js-epoch) data')
        chart (dimple.chart. svg (clj->js data))
        legend (.addLegend chart 60 10 500 20 "right")
        time-axis (.addAxis chart "x" nil nil "time")
        ingame-axis (add-series chart "y" time-axis
                                "ingame" "Ingame Members")]

    (set! (.-tickFormat time-axis) "%b %d %H:%M")
    (set! (.-title time-axis) "Time")
    (set! (.-timePeriod time-axis) js/d3.time.hours)
    (set! (.-timeInterval time-axis) 6)

    (add-series chart ingame-axis time-axis
                "online" "Online Members")

    (add-series chart ingame-axis time-axis
                "total" "Total Members")

    (add-series chart ingame-axis time-axis
                "trade-bans" "Trade Banned Members")

    (add-series chart ingame-axis time-axis
                "vac" "VAC Banned Members")

    (add-series chart ingame-axis time-axis
                "limited_accounts" "Limited Account Members")

    (doto chart
      (.setMargins 50 40 10 100)
      (.draw))

    (let [resize (listen js/window "resize")]
      (go-loop []
        (<! resize)
        (.draw chart 0 true)
        (recur)))

    (-> (.-shapes legend)
        (.selectAll "rect")
        (.on "click"
             (fn [e]
               (let [el (this-as this (js/d3.select this))
                     series (-> e .-series .-shapes)
                     hidden? (= (.attr series "visibility") "hidden")]
                 (js/console.log "Event" e "el" el "series" series)
                 (.attr series "visibility" (if hidden? "visible" "hidden"))
                 ;;(.style el "opacity" (if hidden? 1 0.2))
                 ))))))

(go
  (let [svg (js/dimple.newSvg "body" "100%" 400)
        data (:body (<! (http/get "/api/data")))]
    (println (-> data first keys))
    (println (-> data :vacced))
    (render svg data)))
