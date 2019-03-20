(ns running.views.graphs
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [re-frame.core :refer [subscribe]]
            [running.subscriptions :as subscriptions]
            [rid3.core :as rid3 :refer [rid3->]]
            [goog.object :as gobj]
            [cljs-time.format :as format]))

; todo define a cursor or a subscription here

; todo define some colors

;  data is fetched and lives in app-db's :heatmap-data key

;
(def year-height 136) ; Height of the svg container for a year
(def year-width 900) ; Width of the svg container for a year
(def cell-size 16) ; Cell for each day is this big
(def date-format (format/formatters :date)) ; YYYY-MM-dd

(defn offset-x
  "Calculate x-offset (horizontal) for a given day. This is based on the week in which the day is found."
  [date-string]
  (let [date (format/parse date-format date-string)]
    (* cell-size (format/unparse (cljs-time.format/formatter "w") date))))

(defn offset-y
  "Calculate y-offset (vertical) for a given day. This is based on which day of the week the day is."
  [date-string]
  (let [date (format/parse date-format date-string)]
    (* cell-size (format/unparse (cljs-time.format/formatter "e") date))))


(defn find-max-distance
  "Find max of of a list"
  [elements]
  (apply max (map :distance elements)))

(def color
  (-> js/d3 .scaleQuantize ; todo domain should use maximum distance function above
      (.domain #js [0 45]) ; Domain: our runs are 0-45 miles long (todo adjust for km)
      (.range              ; define our output range
        (.map (.range js/d3 6) ; output range has six values: "q0-6" "q1-6" "q2-6" "q3-6" "q4-6" "q5-6"
              (fn [distance] (str "q" distance "-6")))))) ; these are the blue colors defined in the original js

(defn day-cell-did-mount
  [node ratom]
  (let [data (get @ratom :dataset)]
    (.log js/console "data is " data)
    (rid3-> node
            {:width cell-size
             :height cell-size
             :stroke "#ccc"
             :fill "#fff"
             :class (fn [d] (str "day " (color (goog.object/get d "distance"))))
             :x (fn [d] (offset-x (goog.object/get d "rdate")))
             :y (fn [d] (offset-y (goog.object/get d "rdate")))}
            (.text (fn [d] (goog.object/get d "distance"))))

;    (-> node
;        (.attr "width" cell-size)
;        (.attr "height" cell-size)
;        (.attr "stroke" "#ccc")
;        (.attr "fill" "#fff")
;        (.attr "class" (do (.log js/console "help" node)
;                         (fn [d] (str "day "))))
;        (.attr "x" (fn [d] (offset-x (goog.object/get d "rdate"))))
;        (.attr "y" (fn [d] (offset-y (goog.object/get d "rdate"))))
;        (.text (fn [d] (goog.object/get d "distance"))))
    ))

(defn bleep-boop
  "this is a logging method and will be removed when i'm done"
  []
  (let [data @(subscribe [::subscriptions/heatmap-data])]
    (.log js/console "there are " (count (get data :dataset)) " items in heatmap-data")
    (.log js/console "max in data is " (find-max-distance (get data :dataset)))))

(defn heatmap []
  (let [height year-height
        width year-width
        cell-size cell-size]
    (bleep-boop) ; logging
    (fn []
      [:h4 "text"]
      [rid3/viz
       {:id "heatmap"
        :ratom (subscribe [::subscriptions/heatmap-data])
        :svg {:did-mount (fn [node ratom]
                           (-> node
                               (.attr "width" width)
                               (.attr "height" height)))}
        :pieces [{:kind :elem-with-data
                  :class "day"
                  :tag "rect"
                  :did-mount day-cell-did-mount}]}])))

(defn graph-page []
  [:div "imagine a graph"]
  [heatmap])