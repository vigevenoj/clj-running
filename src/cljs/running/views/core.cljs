(ns running.views.core
  (:require [re-frame.core :as re-frame]
            [running.views.runs :as run-views]
            [running.views.shoes :as shoe-views]
            [running.views.goals :as goal-views]
            [running.views.login :refer [login-form]]
            [running.subscriptions :as subs]
            [running.events :as events]
            [running.routes :as routes]
            [bidi.bidi :as bidi]
            ))

; todo: this should either dispatch the event for :logout itself, or not make the ajax request
(defn logout-link []
  [:li.nav-item
   [:a.nav-link {:on-click
                 #(re-frame/dispatch [:logout])
                 :href "#"} "Logout"]])

(defn navbar []
  (let [user (re-frame/subscribe [::subs/user])]
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
         [:a.dropdown-item {:href (routes/url-for :run-page :id 1)} "1"]
         [:a.dropdown-item {:href (routes/url-for :recent-runs)} "Recent"]
         [:a.dropdown-item {:href (routes/url-for :latest-runs)} "Latest"]]]
       [:li.nav-item.dropdown
        [:a.nav-link {:href (routes/url-for :shoe-index)} "Shoes"]]
       [:li.nav-item
        [:a.nav-link {:href (routes/url-for :goal-index)} "Goals"]]
       (when (seq @user)
         (logout-link))
       [:li.nav-item
        [:a.nav-link {:href (routes/url-for :about)} "About"]]]]]))

(defn home-page []
;   [modals/modal-window]
   (let [user (re-frame/subscribe [::subs/user])]
     (if (not (seq @user))
       (login-form)
       [:div "home page"])))

(defn about-page []
   [:div.row
    [:div.col-md-12
     [:img {:src "/img/warning_clojure.png"}]]])

; https://pupeno.com/2015/08/26/no-hashes-bidirectional-routing-in-re-frame-with-bidi-and-pushy/
; uses this technique to dispatch their routes so the right view is returned
(defmulti active-panel identity)
(defmethod active-panel :home [] (home-page))
(defmethod active-panel :about [] (about-page))
(defmethod active-panel :run-index [] (do
                                        (re-frame/dispatch [:load-runs])
                                        (run-views/run-index)))
(defmethod active-panel :recent-runs [] (do
                                          (re-frame/dispatch [:get-recent-runs])
                                          (run-views/recent-runs-table)))
(defmethod active-panel :latest-runs [] (run-views/latest-run-card))
(defmethod active-panel :run-page [] (run-views/mock-card-ui))
(defmethod active-panel :shoe-index [] (shoe-views/shoe-index))
(defmethod active-panel :shoe-page [id] (.log js/console "setting active panel to" (bidi/match-route routes/routes "/shoes/1") "with ")
  [shoe-views/shoe-page (bidi/path-for routes/routes :shoe-page :id id)])
(defmethod active-panel :goal-index [] (goal-views/goal-index))
(defmethod active-panel :default [] [:div "default text"])