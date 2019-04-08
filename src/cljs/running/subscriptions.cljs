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

; This is a flag, initially false, used to indicate if running data
;has been loaded from the server, in order to discriminate between
; waiting for results and empty results
(reg-sub
  ::running-data-loaded
 (fn [db _]
   (:running-data-loaded db)))

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