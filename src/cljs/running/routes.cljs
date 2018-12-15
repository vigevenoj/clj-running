(ns running.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:import goog.History)
  (:require [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [re-frame.core :as re-frame]))


;; -------------------------
;; Routes
(defn app-routes []
  (secretary/set-config! :prefix "#")

  (defroute "/" []
            (re-frame/dispatch [:set-active-page :home]))
  (defroute "/about" []
            (re-frame/dispatch [:set-active-page :about]))
  (defroute "/running" []
            (re-frame/dispatch [:set-active-page :running-page]))
  (defroute "/recent" []
            (re-frame/dispatch [:set-active-page :running-recent]))
  (defroute "/latest" []
            (re-frame/dispatch [:set-active-page :latest]))
  )

;(secretary/defroute "/" []
;                    (swap! session assoc :page :home))
;
;(secretary/defroute "/about" []
;                    (swap! session assoc :page :about))
;
;(secretary/defroute "/running" []
;                    (swap! session assoc :page :running-page))
;
;(secretary/defroute "/recent" []
;                    (swap! session assoc :page :running-recent))
;
;(secretary/defroute "/graph" []
;                    (swap! session assoc :page :running-graph))
;
;(secretary/defroute "/latest" []
;                    (swap! session assoc :page :latest))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      HistoryEventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))