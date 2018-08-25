(ns running.subscriptions
  (:require [re-frame.core :refer [reg-sub]]))

; Running data is loaded into this
(reg-sub
  :running-data
  (fn [db _]
    (:running-data db)))

; Running data sort field
(reg-sub
  :sort-order
  (fn [db _]
    (:sort-value db)))

; Should running data be sorted ascending or descending?
(reg-sub
  :sort-ascending
  (fn [db _]
    (:ascending db)))

