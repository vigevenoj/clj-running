(ns running.views.shoes
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [re-frame.core :refer [subscribe]]
            [running.routes :as routes]
            [running.subscriptions :as subs]))

(defn shoe-index []
  [:div "shoe index"]
  [:div [:a {:href (routes/url-for :shoe-page :id 1)} "1"]])

(defn shoe-page [id]
  (let [route-params @(subscribe [::subs/route-params])]
    [:div
     (str "this div represents shoe id #"
          (:id route-params)
          " if it were really being rendered")]))