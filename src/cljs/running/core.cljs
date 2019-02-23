(ns running.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
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
;            [running.ajax :refer [load-interceptors!]]
            [running.routes :as routes]
            [running.views.core :as views]
            [ajax.core :refer [GET POST]]))

(defn nav-link [uri title page]
  [:li.nav-item
   {;:class (when (= page (:active-page @session)) "active")
    :key page}
   [:a.nav-link {:href uri} title]])

(defn logout-handler [_]
  (let [user (rf/subscribe [::subs/user])]
    (.log js/console (str "Logging user " (:user-id user) " out of the application")))
  ; todo it should clear the user from the db but i'm not doing that yet
  )

(defn logout-link []
  [:li.nav-item
   [:a.nav-link {:on-click
                       (fn [e]
                         (ajax.core/POST "/api/v1/logout"
                                         {:response-format :json
                                          :keywords? true
                                          :handler logout-handler}))
                 :href "#"} "Logout"]])

(defn navbar []
  [:nav.navbar.navbar-expand-lg
   [:a.navbar-brand  {:href (routes/url-for :home)} "Home"]
   [:div#navbarNav.collapse.navbar-collapse
    [:ul.navbar-nav
     [:li.nav-item.dropdown
      [:a.nav-link.dropdown-toggle {:href "#"
                                    :role "button"
                                    :data-toggle "dropdown"
                                    :aria-haspopup "true"
                                    :aria-expanded "false"} "Runs"]
      [:div.dropdown-menu
       [:a.dropdown-item {:href (routes/url-for :run-index)} "Index"]
       [:a.dropdown-item {:href (routes/url-for :recent-runs)} "Recent"]
       [:a.dropdown-item {:href (routes/url-for :latest-runs)} "Latest"]]]
     [:li.nav-item.dropdown
      [:a.nav-link {:href (routes/url-for :shoe-index)} "Shoes"]]
     [:li.nav-item
      [:a.nav-link {:href (routes/url-for :goal-index)} "Goals"]]
     [:li.nav-item
      [:a.nav-link {:href (routes/url-for :about)} "About"]]]]]
  )

(defn login-handler [response]
  (.log js/console (str response))
  ; this should be an effect handler that adds the user to the state db
  ;(swap! app-state assoc :user (:user response))
  )



(defn page []
  (let [active-page (rf/subscribe [:active-page])]
    (fn []
      [:div#wrapper
       [navbar]
       (views/active-panel @active-page)])))




;; -------------------------
;; Initialize app
(defn mount-components []
  (rf/clear-subscription-cache!)
  ;(r/render [#'navbar] (.getElementById js/document "navbar"))
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (routes/app-routes)
  (rf/dispatch-sync [::run-events/initialize-db])
;  (load-interceptors!)
  (mount-components))
