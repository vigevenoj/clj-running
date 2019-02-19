(ns running.routes
  (:require [re-frame.core :as re-frame]
            [bidi.bidi :as bidi]
            [pushy.core :as pushy]))


;; -------------------------
;; Routes
(def routes ["/" {"" :home
                  "about" :about
                  "runs/" {"" :run-index
                           "new" :run-form
                           "recent" :recent-runs
                           "latest" :latest-runs
                           [:id] :run-page}
                  "shoes/" {"" :shoe-index
                            [:id] :shoe-page}
                  "goals/" {"" :goal-index
                            [:id] :goal-page}}])

(def preload {:home :runs :run-form :recent-runs :latest-runs :run-page :shoe-index :shoe-page :goal-index :goal-page})

; turn url into data structure
(defn- parse-url [url]
  (bidi/match-route routes url))

; fire an event to change pages
(defn- dispatch-route [matched-route]
  (let [handler (:handler matched-route)
        params (:route-params matched-route)]
    (re-frame/dispatch [:change-page handler params])))

; add history
(def history (pushy/pushy dispatch-route parse-url))

(defn app-routes []
  (pushy/start! history))

(defn change-route [route]
  (pushy/set-token! history route))

(def url-for (partial bidi/path-for routes))

(defn path-for-run [runid]
  (bidi/path-for routes :runs :id runid))