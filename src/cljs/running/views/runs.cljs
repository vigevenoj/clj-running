(ns running.views.runs
  (:require [reagent.core :as r]
            [re-frame.core :refer [dispatch subscribe]]
            [running.util :refer [format-date format-duration]]))

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
  (r/with-let [data @(subscribe [:running-data])
               sort-order @(subscribe [:sort-value])
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