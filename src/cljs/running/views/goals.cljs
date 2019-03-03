(ns running.views.goals
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe]]
            [running.routes :as routes]
            [running.subscriptions :as subs]))

(defn goal-index []
   [:div "goal index"])

(defn shoe-page [id]
  (let [route-params @(subscribe [::subs/route-params])]
    [:div
     (str "this div represents shoe id #"
          (:id route-params)
          " if it were really being rendered")]))