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
 (fn [{:keys [db]} [_ _]]
   (.log js/console "handling logout")
   {:db       db/default-db
    :dispatch [:set-active-page :home]}
   (aset js/document "location" "/")))

(reg-event-fx
 :logout
 (fn [_ _]
   (.log js/console "post to logout" )
   {:http-xhrio {:method               :post
                 :uri                  "/api/v1/logout"
                 :ignore-response-body true
                 ; might change this if we need the body
                 :timeout              5000
                 :format               (ajax/json-request-format)
                 :response-format      (ajax/json-response-format {:keywords? true})
                 :on-success           [:handle-logout]
                 :on-failure           [:handle-logout]}})) ; clear the database. memory-hole also sets the user to nil explicitly, not sure why

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
   {:http-xhrio {:method          :get
           :uri             "/api/v1/running/runs"
           :timeout         5000
           :format          (ajax/json-request-format)
           :response-format (ajax/json-response-format {:keywords? true})
           :on-success       [::runs-fetched]
           :on-failure      [::failed-remote-request]}})) ; add a function to handle failure to load runs

; todo: this and :recent-90-loaded duplicate a lot of code. extract it out to something sensible
(reg-event-db
 ::runs-fetched
 (fn [db [_ response]]
   (merge db {:running-data response})))

;; -----
;; running events

; This should update the state db to set :recent :checked-recent to true, and load the runs into :recent :recent-runs
(reg-event-fx
 :get-recent-runs
 (fn [{db :db} _]
   {:http-xhrio {:method          :get
           :uri             "/api/v1/running/recent/90"
           :timeout         5000
           :format          (ajax/json-request-format)
           :response-format (ajax/json-response-format {:keywords? true})
           :on-success      [::recent-90-loaded]
           :on-failure      [::failed-remote-request]}})) ; add function to handle failure to load recent runs

; todo: this and :runs-fetched duplicate a lot of code. extract it out to something sensible
(reg-event-db
  ::recent-90-loaded
 (fn [db [_ response]]
   (merge db {:running-data response})))

; Load the latest run (just the one most-recent run)
(reg-event-fx
  :get-latest-run
 (fn [{db :db} _]
   {:http-xhrio {:method :get
           :uri "/api/v1/running/latest?limit=1"
           :timeout 5000
           :format (ajax/json-request-format)
           :response-format (ajax/json-response-format {:keywords? true})
           :on-success [::latest-run-success]
           :on-failure [::failed-remote-request]}}))


(reg-event-db
  ::latest-run-success
 (fn [db [_ response]]
   (merge db {:latest {:latest-runs response} })))

(reg-event-db
  ::failed-remote-request
 (fn [db [_ result]]
   (.log js/console "failed to query remote api")))

