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
  ::route-params
 (fn [db _]
   (:route-params db)))

(reg-sub
  :latest-runs-data
 (fn [db _]
   (:latest-runs (:latest db))))

(reg-sub
  ::running-data
 (fn [db _]
   (:running-data db)))

(reg-sub
  ::recent-runs-data
 (fn [db _]
   (:recent-runs (:recent db))))

(reg-sub
  ::ytd-distance
 (fn [db _]
   (:ytd-distance (:statistics db))))

(reg-sub
  ::heatmap-data
 (fn [db _]
   (:heatmap-data db)))

(reg-sub
  ::heatmap-years
 (fn [db _]
   (:heatmap-years db)))