(ns running.views.core
  (:require [re-frame.core :as re-frame]
            [running.views.runs :as run-views]
            [running.views.login :refer [login-form]]
            [running.subscriptions :as subs]
            [running.events :as events]
            [running.routes :as routes]
            ))


(defn home-page []
  [:div.container
;   [modals/modal-window]
   (let [user (re-frame/subscribe [::subs/user])]
     (if (not (seq @user))
       (login-form)
       [:div "home page"]))])

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:img {:src "/img/warning_clojure.png"}]]]])

; https://pupeno.com/2015/08/26/no-hashes-bidirectional-routing-in-re-frame-with-bidi-and-pushy/
; uses this technique to dispatch their routes so the right view is returned
(defmulti active-panel identity)
(defmethod active-panel :home [] [home-page])
(defmethod active-panel :about [] [about-page])
;(defmethod active-panel :run-index [] nil) ; maybe change that to params?
(defmethod active-panel :default [] [:div "default text"])