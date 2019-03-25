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
; equivalent to index.html's d3.range(2003, 2019) call is something like
; (-> js/d3 (.range 2003 2020)) to produce a js array [2003 2004 ... 2019]


; this gives a selection that contains something maybe useful, (it's rather long)
; (.datum (.attr (.attr (.attr (.attr (.attr (.append (.enter (.data (js/d3.selectAll ".day") (fn [d] (graphs/generate-dates-for-year d)))) "rect") "class" "day") "width" 16) "height" 16) "x" (fn [d] (graphs/offset-x d))) "y" (fn [d] (graphs/offset-y d))) (js/d3.timeFormat "%Y-%m-%d"))
; but i don't know how to use that selection

;
(def year-height 136) ; Height of the svg container for a year
(def year-width 900) ; Width of the svg container for a year
(def cell-size 16) ; Cell for each day is this big
(def date-format (format/formatters :date)) ; YYYY-MM-dd
(def formatTime (.timeFormat js/d3 "%Y-%m-%d")) ; convert from js Date to YYYY-MM-dd

(defn generate-dates-for-year
  "Generate a js array of dates for an entire year"
  [year]
  (js/d3.timeDays (js/Date. year 0 1) (js/Date. (+ year 1) 0 1)))

(defn year-range
  "Generate a range of years. Begin is inclusive, end is exclusive."
  [begin end]
  (-> js/d3 (.range begin end)))

; this is the equivalent of lines 83-86-ish:
; (ugly-svg (year-range 2003 2020))
(defn create-svg-containers-for-years [years]
  (.attr
    (.append
      (.enter
        (.data
          (js/d3.selectAll "svg" (js/d3.select "body"))
          years)) "svg")
    "class" "Blues")
  ; this then adds the correct attributes to the svgs with class "Blues"
  (-> (.filter (js/d3.selectAll "svg") ".Blues")
      (.attr "width" 900)
      (.attr "height" 136))
  ; todo should be able to add the transform/translate attribute here as well
  ; todo should be able to append the text node here
  ; todo and we should be able to follow a similar pattern for day rects as well
  )



(defn offset-x
  "Calculate x-offset (horizontal) for a given day. This is based on the week in which the day is found."
  [date-string]
  (let [date (format/parse date-format date-string)
        weekyear (format/unparse (cljs-time.format/formatter "x") date)
        year (format/unparse (cljs-time.format/formatter "Y") date)
        year-diff (- weekyear year)]
    (+ (* cell-size (format/unparse (cljs-time.format/formatter "w") date)) (* 52 cell-size year-diff))))

(defn offset-y
  "Calculate y-offset (vertical) for a given day. This is based on which day of the week the day is."
  [date-string]
  (let [date (format/parse date-format date-string)]
    (* cell-size (format/unparse (cljs-time.format/formatter "e") date))))


(defn find-max-distance
  "Find max of of a list"
  [elements]
  (apply max (map :distance elements)))

(defn find-first-year
  "Return first year (yyyy) in the results"
  [elements]
  ; todo probably worth the cost of parsing/unparsing dates here to be sure about this
  (subs (str (apply min (map :rdate elements))) 0 4))

(defn find-last-year
  "Return the last year in the results"
  [elements]
  ; todo probably worth the cost of parsing/unparsing dates here to be sure about this
  (subs (str (apply max (map :rdate elements))) 0 4))


(defn find-by-date
  "Find distance for a given date. Returns () if that date does not have distance present in the elements provided"
  [elements date]
  (:distance (first (->> elements (filter #(= (:rdate %) date))))))

(def color
  (-> js/d3 .scaleQuantize ; todo domain should use maximum distance function above
      (.domain #js [0 45]) ; Domain: our runs are 0-45 miles long (todo adjust for km)
      (.range              ; define our output range
        (.map (.range js/d3 7) ; output range has six values: "q0-6" "q1-6" "q2-6" "q3-6" "q4-6" "q5-6"
              (fn [distance] (str "q" distance "-6")))))) ; these are the blue colors defined in the original js

; This method is used in testing to remove the svg from the viz element to try out different things
(defn remove-viz-svg []
  (-> js/d3
      (.selectAll "#viz svg")
      (.remove)))

; I think I want to either take the elements of the dataset and merge them into the list of dates
; so that every date has distance data (or nil, if no distance measured). Not sure which way the merge
; would need to go (dataset info merged into list of dates, or dates inserted into dataset with nil distance)
; or if I should just look in the dataset for each day
(defn full-year-iterate [year]
  (let [days (generate-dates-for-year year)
        node (js/d3.select "#viz")
        data (get @(subscribe [::subscriptions/heatmap-data]) :dataset)]
    (.log js/console "There are " (count days) "elements")
    (.log js/console "adding svg to #viz element")
    (-> node
        (.append "svg")
        (.attr "height" year-height)
        (.attr "width" year-width)
        (.attr "class" (str "year year-" year " Blues")))
    ; what i want here is for the days data to be appended to the selection
    ; so i can append them all as rects to the svg
    (let [svg-element (js/d3.select "#viz svg")]
      (goog.object/forEach days
                           (fn [day]
                             (let [formatted-day (-> day formatTime)
                                   distance (find-by-date data formatted-day)]
;                               (.log js/console formatted-day ":" distance)
                               (-> svg-element
                                   (.append "rect")
                                   (.attr "height" cell-size)
                                   (.attr "width" cell-size)
                                   (.attr "x" (fn [d] (offset-x formatted-day)))
                                   (.attr "y" (fn [d] (offset-y formatted-day)))
                                   (.attr "fill" "#fff")
                                   (.attr "stroke" "#ccc")
                                   (.attr "class"
                                          (fn [d] ; todo extract this out, or move to color function
                                            (if (< 0 distance)
                                              (str "day " (color distance))
                                              (str "day"))))
                                   (.text
                                     (fn [d]
                                       (str formatted-day " "
                                            (find-by-date data formatted-day)))))))))))

(defn year-viz [year]
  (r/create-class
   {:display-name "year-viz"
    :component-did-mount (fn [this] (full-year-iterate 2019))
    :reagent-render (fn [] [:div#viz "imagine a graph"])
    }))


(defn day-cell-did-mount
  [node ratom]
  (let [data (get @(subscribe [::subscriptions/heatmap-data]) :dataset)]
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


(defn heatmap []
  (let [height year-height
        width year-width
        cell-size cell-size
        data (subscribe [::subscriptions/heatmap-data])]
    (fn []
      (when (not-empty @data) ; If this is empty, then things explode
        [rid3/viz
         {:id "heatmap"
          :ratom data
          :svg {:did-mount (fn [node ratom]
                             (-> node
                                 (.attr "width" width)
                                 (.attr "height" height)))}
          :pieces [{:kind :elem-with-data
                    :class "day"
                    :tag "rect"
                    :did-mount day-cell-did-mount}]}]))))

(defn graph-page []
  (year-viz 2019)
;  [heatmap]
  )