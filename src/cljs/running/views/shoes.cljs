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
     [:div.col-sm-4
      [:div.card
       [:div.card-body
        [:h5.card-title.text-primary
         "this is where the shoe name goes" ]
         [:ul.list-group.list-group-flush
          [:li.list-group-item
           "this is where the shoe details go"]
           [:li.list-group-item
            "this is where some other shoe stuff goes"]]]]]))