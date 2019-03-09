(ns running.views.runs
  (:require [reagent.core :as r]
            [ajax.core :refer [GET POST]]
            [re-frame.core :refer [dispatch subscribe]]
            [re-frame-datatable.core :as dt]
            [running.subscriptions :as subs]
            [running.routes :as routes]
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

(defn run-card-ui [run]
  [:div.col-sm-4
   [:div.card
    [:div.card-body
     [:h5.card-title.text-primary ;(:runid run)
      [:span.runcard-date {:style {:padding-right 2}} (:rdate run)]
      [:span.runcard-tod {:style {:padding-left 2}} (:timeofday run)]]
     [:ul.list-group.list-group-flush
      [:li.list-group-item
       [:span.runcard-distance {:style {:padding-right 2}} (:distance run)]
       [:span.runcard-distance-units {:style {:padding-left 2}} (:units run)]]
      [:li.list-group-item
       [:span.runcard-duration (format-duration (:elapsed run))]]]]]])


(defn run-page [id]
  (let [id (:id @(subscribe [::subs/route-params]))
        running-data @(subscribe [::subs/running-data])
        r            (->> running-data (filter #(= (str (:runid %)) id)) first)] ;(some #(if (= id (:runid %)) %) running-data)]
    (run-card-ui r)))

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

(defn runid-formatter [runid]
  [:a {:href (routes/url-for :run-page :id runid)} runid])

(defn run-datatable []
  [dt/datatable
   :runs
   [::subs/running-data]
   [{::dt/sorting {::dt/enabled? true}
     ::dt/column-key [:runid]
     ::dt/column-label "#"
     ::dt/render-fn runid-formatter}
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