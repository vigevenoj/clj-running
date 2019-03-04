(ns running.views.runs
  (:require [reagent.core :as r]
            [ajax.core :refer [GET POST]]
            [re-frame.core :refer [dispatch subscribe]]
            [re-frame-datatable.core :as dt]
            [running.subscriptions :as subs]
            [running.util :refer [format-date format-duration]]))

(defn run-form [id]
  (let [value (atom nil)]
    [:div.runform
     [:form {:on-submit (fn [e]
                          (.preventDefault e)
                          (.log js/console "run form submitted"))}
      [:span.rundate
       [:input {:type "text" :placeholder "Date"}]]
      [:span
       [:select
        [:option "am"]
        [:option "pm"]
        [:option "noon"]
        [:option "night"]]]
      [:span
       [:input {:type "text" :placeholder "Distance"}]]
      [:span
       [:select {:default-value "miles"}
        [:option "km"]
        [:option "m"]
        [:option "miles"]
        ]]
      [:span
       [:input {:type "text" :placeholder "Elapsed time"}]]
      [:span
       [:input {:type "text" :placeholder "Comments"}]]
      [:button {:type :submit} "Save"]]]))

(defn run-row-ui
  "Display a single run"
  [{:keys [runid rdate timeofday distance units elapsed comment effort] :as run}]
  [:tr {:key runid}
   [:td (format-date rdate)]
   [:td timeofday]
   [:td (if (not (nil? distance))
          (str distance))]
   [:td units]
   [:td.duration (format-duration elapsed)]
   [:td comment]
   [:td effort]])

(defn run-display-table-ui
  "Render a table of runs"
  [data]
  (r/with-let [sort-order @(subscribe [:sort-value])
               sort-ascending @(subscribe [:ascending])]
              [:table.runningData
               [:thead
                [:tr
                 [:th {:width "200" :on-click #(dispatch [:change-sort-field :rdate])
                       } "Date"]
                 [:th {:width "200" } "Time of Day"]
                 [:th {:width "200" } "Distance"]
                 [:th "Units"]
                 [:th {:width "200" :on-click #(dispatch [:change-sort-field :elapsed])
                       } "Elapsed"]
                 [:th {:width "200" } "Comment"]
                 [:th {:width "200" } "Effort"]]]
               [:tbody
                (when (seq data)
                  (for [r data] ; removed [r (sorted-data data)] because we don't have a sorted-data fn
                    ^{:key (:runid r)}
                    [run-row-ui r]))]]))

(defn run-card-ui
  [data]
  (let [run data] ; for now, at least
    (fn [run]
      [:div.runcard
       {:id (:runid run)
        :style {:border "1xp solid black"
                :padding 20
                :margin 10
                :display "inline-block"
                :max-width "50%"}}
       [:span.runcard-title
        [:span.runcard-date {:style {:padding-right 2}} (:rdate run)]
        [:span.runcard-tod {:style {:padding-left 2}} (:timeofday run)]]
       [:span.runcard-distance {:style {:display "block"}}
        [:span {:style {:padding-right 2}} (:distance run)]
        [:span {:style {:padding-left 2}} (:units run)]]
       [:span.runcard-duration (format-duration (:elapsed run))]])))

(defn mock-card-ui []
  (let [run {:runid 1
             :rdate "2019-02-23"
             :timeofday "pm"
             :distance "16.4"
             :units "miles"
             :elapsed "PT2H30M6S"}]
    [run-card-ui run]))

(defn latest-run-card []
  (let [latest-run @(subscribe [:latest-runs-data])]
    (if (empty? latest-run)
      (re-frame.core/dispatch [:get-latest-run])
      (.log js/console (str "No fetch required for latest run, " latest-run)))
    [run-card-ui (first latest-run)]))

(defn recent-runs-table []
  (let [data @(subscribe [::subs/recent-runs-data])]
    (.log js/console data))) ; <-- todo: see note one line down for why this just logs for now
    ;run-display-table-ui data)) ; <-- todo: this doesn't handle empty collections well



;(defn latest-run-card [data]
;  []
;  (let [run (first data)]
;    (fn []
;      (when
;;      (when (and (empty? data) (not (:checked-latest @app-state)))
;        (do
;          (get-latest-runs 1)
;          (swap! app-state assoc :checked-latest true)))
;      [:div.runcard
;       {:id (:runid run)
;        :style  {:border "1px solid black"
;                 :padding 20
;                 :margin 10
;                 :display "inline-block"
;                 :max-width "50%"
;                 }}
;       [:span.runcard-title
;        [:span.runcard-date {:style {:padding-right 2}} (:rdate run)]
;        [:span.runcard-tod  {:style {:padding-left 2}}(:timeofday run)]]
;       [:span.runcard-distance {:style {:display "block"}}
;        [:span {:style {:padding-right 2}} (:distance run)]
;        [:span {:style {:padding-left 2}} (:units run)]]
;       [:span (format-duration (:elapsed run))]])))

(defn run-datatable []
  [dt/datatable
   :runs
   [::subs/running-data]
   [{::dt/sorting {::dt/enabled? true}
     ::dt/column-key [:runid]
     ::dt/column-label "#"}
    {::dt/sorting {::dt/enabled? true}
     ::dt/column-key [:rdate]
     ::dt/column-label "Date"}
    {::dt/column-key [:timeofday]
     ::dt/column-label "Time of day"}
    {::dt/column-key [:distance]
     ::dt/column-label "Distance"}
    {::dt/column-key [:units]
     ::dt/column-label "Units"}
    {::dt/column-key [:elapsed]
     ::dt/column-label "Elapsed"
     ::dt/render-fn (fn [val]
                      (format-duration val))}
    {::dt/column-key [:comment]
     ::dt/column-label "Comment"}
    {::dt/column-key [:effort]
     ::dt/column-label "Effort"}]
   {::dt/table-classes ["ui" "celled" "stripped" "table"]}])

(defn run-index []
   [:div "run index"]
  [run-datatable []])