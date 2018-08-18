(ns running.events
  (:require [running.db :refer [default-db]]
            [re-frame.core :refer [reg-event-db reg-event-fx inject-cofx path after]]))


;; establish our initial application state
(reg-event-fx
  :initialize-db
  (fn [_ _]
    default-db))

;; Set which page is active
(reg-event-fx
  :set-active-page
  (fn [db [_ page]]
    (assoc db :page page)))