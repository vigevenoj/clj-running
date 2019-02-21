(ns running.views.login
  (:require [reagent.core :as r]
            [re-frame.core :refer [dispatch subscribe]]
            [running.subscriptions :as subs]))

(defn login [params]
  (let [{:keys [username password]} @params]
    (dispatch [:login username password])))

(defn login-form []
  (r/with-let [user (subscribe [::subs/user])
               params (r/atom nil)]
              [:form
               {:on-submit
                (fn [e]
                  (.preventDefault e)
                  (login params))}
               [:label "Username: "]
               [:input {:type "text"
                        :name "username"
                        :id "username"
                        :placeholder "Username"
                        :on-change #(swap! params assoc :username (-> % .-target .-value))}]
               [:label "Password: "]
               [:input {:type "password"
                        :name "password"
                        :id "password"
                        :placeholder "Password"
                        :on-change #(swap! params assoc :password (-> % .-target .-value))}]
               [:button {:type :submit} "Login"]]))

(defn login-page []
  )