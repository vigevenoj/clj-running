(ns running.subscriptions
  (:require [re-frame.core :refer [reg-sub]]))

; logged-in user
(reg-sub
  ::user
 (fn [db]
   (:user db)))

(reg-sub
  :active-page
 (fn [db _]
   (:active-page db)))

(reg-sub
  :latest-runs-data
 (fn [db _]
   (:latest-runs (:latest db))))

(reg-sub
  :running-data
 (fn [db _]
   (:running-data db)))