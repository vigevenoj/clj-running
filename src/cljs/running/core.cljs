(ns running.core
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [day8.re-frame.http-fx]
            [reagent-modals.modals :as modals]
            [running.util :refer [format-date format-duration]]
            [running.events :as run-events] ; Needed so the closure compiler loads it
            [running.subscriptions :as subs] ; Needed so the closure compiler loads it
            [running.views.runs :as runs]
            [running.views.login :refer [login-form]]
            [clojure.string :as string]
            [cognitect.transit :as t]
            [markdown.core :refer [md->html]]
            [running.routes :as routes]
            [running.views.core :as views]
            [ajax.core :refer [GET POST]]))

(defn page []
  (let [active-page (re-frame/subscribe [:active-page])]
    (fn []
      [:div#wrapper
       (views/navbar)
       [:div.container
        (views/active-panel @active-page)]])))




;; -------------------------
;; Initialize app
(defn mount-components []
  (re-frame/clear-subscription-cache!)
  ;(r/render [#'navbar] (.getElementById js/document "navbar"))
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (routes/app-routes)
  (re-frame/dispatch-sync [::run-events/initialize-db])
;  (load-interceptors!)
  (mount-components))
