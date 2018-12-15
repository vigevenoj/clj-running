(ns running.handlers
  (require [re-frame.core :refer [dispatch dispatch-sync reg-event-db reg-event-fx]]
           [running.db :as db]))

(reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

(reg-event-db
  :set-active-page
  (fn [db [_ page]]
    (assoc db :page page)))

(reg-event-fx
  :navigate-to
  (fn [_ [_ url]]
    {:navigate url}))

