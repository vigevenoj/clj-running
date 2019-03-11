(ns running.views.shoes
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [re-frame.core :refer [subscribe]]
            [running.routes :as routes]
            [running.subscriptions :as subs]))

(defn shoe-form []
  [:form
   [:div.form-group
    [:label {:for "name"} "Shoe name"]
    [:input.form-control {:type "text"
                          :name "name"
                          :id "name"
                          :placeholder "Shoe name"}]
    [:label {:for "escription"} "Description"]
    [:input.form-control {:type "text"
                          :name "description"
                          :id "description"
                          :placeholder "Description"}]
    [:label {:for "distance-expiration"} "Maximum Distance"]
    [:input.form-control {:type "number"
                          :name "distance-expiration"
                          :id "distance-expiration"
                          :placeholder ""}]
    [:label {:for "distance-expiration-units"}]
    [:select.form-control {:id "distance-expiration-units"
                           :name "distance-expiration-units"}
     [:option {:value "miles"} "Miles"]
     [:option {:value "km"} "km"]
     [:option {:value "m"} "m"]]]
   [:button.btn.btn-primary {:type :submit} "Submit"]])

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