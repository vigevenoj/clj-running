(ns running.views.goals
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe]]
            [running.routes :as routes]
            [running.subscriptions :as subs]))

(defn goal-index []
   [:div "goal index"])

(defn goal-page [id]
  (let [route-params @(subscribe [::subs/route-params])]
    [:div.col-sm-4
     [:div.card
      [:div.card-body
       [:h5.card-title.text-primary ;(:runid run)
        "this is where the goal name goes"
       [:ul.list-group.list-group-flush
        [:li.list-group-item
         "this is where the goal details go"
        [:li.list-group-item
         "this is where some other goal stuff goes"]]]]]]]))