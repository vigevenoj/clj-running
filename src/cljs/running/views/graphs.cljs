(ns running.views.graphs
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [re-frame.core :refer [dispatch subscribe]]
            [running.subscriptions :as subscriptions]
            [running.util :as util]
            [running.events :as events]
            [goog.object :as gobj]
            [rid3.core :as rid3 :refer [rid3->]] ; todo: remove this dependency and use cljsjs d3 directly
            [cljs-time.format :as format]
            [cljs-time.core :refer [now year]]))

;  data is fetched and lives in app-db's :heatmap-data key
; equivalent to index.html's d3.range(2003, 2019) call is something like
; (-> js/d3 (.range 2003 2020)) to produce a js array [2003 2004 ... 2019]

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
(defn remove-viz-svg [year]
  (-> js/d3
      (.selectAll (str "#viz-" year "svg"))
      (.remove)))

; this returns nil, which means we can't use it as the thing that returns in a reagent component
(defn full-year-iterate [year]
  (let [days (generate-dates-for-year (js/parseInt year))
        node (js/d3.select (str "#viz-" year)) ; "#viz-2019"
        dataset (subscribe [::subscriptions/heatmap-data])]
    (or (get @dataset :dataset) (.log js/console "no data yet!")) ; this gets called twice on page load, but then the data is loaded
    (-> node
        (.append "svg")
        (.attr "height" year-height)
        (.attr "width" year-width)
        (.attr "class" (str "year year-" year " Blues")))
    (let [svg-element (js/d3.select (str "#viz-" year " svg"))]
      (goog.object/forEach days
                           (fn [day]
                             (let [formatted-day (-> day formatTime)
                                   distance (find-by-date (get @dataset :dataset) formatted-day)]
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
                                            (find-by-date (get @dataset :dataset) formatted-day)))))))))))

(defn get-new-heatmap-data [params]
  (let [{:keys [years]} @params]
    (dispatch [::events/get-heatmap-data years])
    (dispatch [::events/heatmap-update-years years])))

(defn year-list []
  (fn []
    (r/with-let [form-params (r/atom nil)
                 selected-years @(subscribe [::subscriptions/heatmap-years])]
              [:form {:on-submit (fn [e]
                                   (.preventDefault e)
                                   (get-new-heatmap-data form-params))}
               [:div#year-form.form-group
                [:label {:for "years"} "Years"]
                [:select.form-control {:id "year-selection"
                                       :name "year-selection"
                                       :multiple true
                                       :default-value (or selected-years (list (year (now))))
                                       :on-change #(swap! form-params assoc :years
                                                           (util/values-of-selected-options
                                                            (util/get-select-options "year-selection")))}
                 (map (fn [x] ^{:key x} [:option {:value x} x]) (range (year (now)) 2003 -1))]]
               [:button.btn.btn-primary {:type :submit} "Fetch"]])))

(defn render-year-viz [year]
  (full-year-iterate year)
  [:div {:id (str "viz-" year)}
   (str "Runs in " year ", darker is longer")])

; todo This renders the heatmap on load but seems slow
;(defn viz-component [year]
;  (let [year year]
;    (fn []
;      (do
;        (full-year-iterate year)
;        (render-year-viz year)))))

(defn year-viz [year]
  (r/create-class
   {:display-name (str "RunningYearViz" year)
    :component-did-mount (fn [this]  (do
                                       ;(full-year-iterate year)
                                       (render-year-viz year)))
    :component-did-update (fn [this] (do
;                                       (remove-viz-svg year)
                                       ;(full-year-iterate year)
                                       (render-year-viz year)))
    :reagent-render render-year-viz}))

; Have to stick both components into a parent component for both to render correctly
(defn input-and-graphs []
  (let [years (subscribe [::subscriptions/heatmap-years])]
    (fn []
      [:div.row
       [:div.col-md-2
        [year-list]]
       [:div.col-md-5
        (for [year @years]
          (do
;            (remove-viz-svg year)
;            ^{:key year} [viz-component year])
            ^{:key year} [year-viz year]))
        ]]))) ;

(defn graph-page []
  (input-and-graphs))