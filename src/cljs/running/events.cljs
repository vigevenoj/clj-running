(ns running.events
  (:require [running.db :as db]
            [running.routes :as routes]
            [re-frame.core :refer [dispatch dispatch-sync reg-event-db reg-event-fx]]
            [ajax.core :as ajax]
            [clojure.string :as string]))


;; establish our initial application state
(reg-event-fx
  :initialize-db
  (fn [_ _]
    db/default-db))

;; Set which page is active
;(reg-event-fx
;  :set-active-page
;  (fn [db [_ page]]
;    (assoc db :active-page page)))
(reg-event-db
  :change-page
 (fn [db [_ new-active-page]]
   (assoc db :active-page new-active-page)))

(reg-event-fx
  :login
  (fn [_ [_ username password]]
       (if (or (string/blank? username) (string/blank? password))
         {:dispatch [:set-error "Username and password are required."]}
         {:http {:method ajax/POST
                 :url "/api/v1/login"
                 :ajax-map {:params {:username username :pass password}}
                 :success-event [:handle-login]
                 :error-event [:handle-login-error]}})))

(reg-event-fx
  :handle-login
  (fn [{:keys [db]} [_ {:keys [user]}]]
    {:dispatch [:set-active-page :home]
     :db (assoc db :user user)}))

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
    {:http {:method ajax/POST
            :url "/api/v1/logout"
            :ignore-response-body true ; might change this if we need the body
            :success-event [:handle-logout]
            :error-handler [:handle-logout]}
     :db db/default-db})) ; clear the database. memory-hole also sets the user to nil explicitly, not sure why

(reg-event-fx
  :change-sort-field
  (fn [{:keys [db]} [_ field]]
    (if (= field (:sort-value db)) ; there is probably a more re-framey way to do this
      (swap! db update-in [:ascending] not)
      (swap! db assoc :ascending true))
    (swap! db assoc :sort-value field)))

(reg-event-fx
  :load-runs
  (fn [{db :db} _]
    {:http {:method :get
            :uri "/api/v1/running/runs"
            :response-format (ajax/json-response-format {:keywords? true})
            :on-sucess []
            :on-failure []}}))

;; -----
;; running events
