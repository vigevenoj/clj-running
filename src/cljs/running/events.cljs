(ns running.events
  (:require [running.db :as db]
            [running.routes :as routes]
            [re-frame.core :refer [dispatch dispatch-sync reg-event-db reg-event-fx]]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [clojure.string :as string]))


;; establish our initial application state
(reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

;; Set which page is active
(reg-event-db
 :set-active-page
 (fn [db [_ new-active-page]]
   (.log js/console (str "setting new page to " new-active-page))
   (assoc db :active-page new-active-page)))

(reg-event-db
 :set-error
 (fn [db [_ error]]
   (assoc db :error error)))

(reg-event-fx
 :login
 (fn [_ [_ username password]]
   (if (or (string/blank? username) (string/blank? password))
     {:dispatch [:set-error "Username and password are required."]}
     {:http-xhrio {:method          :post
                   :uri             "/api/v1/login"
                   :params          {:username username :pass password}
                   :timeout         5000
                   :format          (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:handle-login]
                   :on-failure      [:handle-login-error]}})))

(reg-event-fx
 :handle-login
 (fn [{:keys [db]} [_ {:keys [user]}]]
   {:dispatch [:set-active-page :home]
    :db       (assoc db :user user)}))

(reg-event-fx
 :handle-login-error
 (fn [_ _]
   {:dispatch [:set-error "Invalid username or password"]}))

(reg-event-fx
 :handle-logout
 (fn [_ _]
   {:reload-page true}))

(reg-event-fx
 :logout
 (fn [_ _]
   {:http-xhrio {:method               :post
                 :url                  "/api/v1/logout"
                 :ignore-response-body true
                 ; might change this if we need the body
                 :timeout              5000
                 :format               (ajax/json-request-format)
                 :response-format      (ajax/json-response-format {:keywords? true})
                 :on-success           [:handle-logout]
                 :on-failure           [:handle-logout]}
    :db         db/default-db})) ; clear the database. memory-hole also sets the user to nil explicitly, not sure why

(reg-event-db
 :change-sort-field
 (fn [{:keys [db]} [_ field]]
   (if (= field (:sort-value db)) ; there is probably a more re-framey way to do this
     (swap! db update-in [:ascending] not)
     (swap! db assoc :ascending true))
   (swap! db assoc :sort-value field))) ; This should update the state db on success to set :running-data to the results of the request

(reg-event-fx
 :load-runs
 (fn [{db :db} _]
   {:http {:method          :get
           :uri             "/api/v1/running/runs"
           :timeout         5000
           :format          (ajax/json-request-format)
           :response-format (ajax/json-response-format {:keywords? true})
           :on-sucess       []
           ; this function is where the state db update should happen
           :on-failure      []}})) ; add a function to handle failure to load runs

;; -----
;; running events

; This should update the state db to set :recent :checked-recent to true, and load the runs into :recent :recent-runs
(reg-event-fx
 :get-recent-runs
 (fn [{db :db} _]
   {:http {:method          :get
           :uri             "/ai/v1/running/recent/90"
           :timeout         5000
           :format          (ajax/json-request-format)
           :response-format (ajax/json-response-format {:keywords? true})
           :on-success      []
           ; add function for where the update should happen
           :on-failure      []}})) ; add function to handle failure to load recent runs