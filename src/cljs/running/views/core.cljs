(ns running.views.core
  (:require [re-frame.core :as re-frame]
            [running.views.runs :as run-views]
            [running.views.shoes :as shoe-views]
            [running.views.goals :as goal-views]
            [running.views.graphs :as graphs]
            [running.views.login :refer [login-form]]
            [running.subscriptions :as subs]
            [running.events :as events]
            [running.routes :as routes]
            [bidi.bidi :as bidi]
            [goog.string :as gstr]
            ))

; todo: this should either dispatch the event for :logout itself, or not make the ajax request
(defn logout-link []
  [:li.nav-item
   [:a.nav-link {:on-click
                 #(re-frame/dispatch [:logout])
                 :href (routes/url-for :logout)} "Logout"]])

(defn logged-out-navbar []
  [:nav.navbar.navbar-expand-lg
   [:a.navbar-brand {:href (routes/url-for :home)} "Home"]
   [:div#navbarNav.collapse.navbar-collapse
    [:ul.navbar-nav
     [:li.nav-item.nav-link "Runs"]
     [:li.nav-item.nav-link "Shoes"]
     [:li.nav-item.nav-link "Goals"]
     [:li.nav-item
      [:a.nav-link {:href (routes/url-for :about)} "About"]]
;     [:li.nav-item "Log in"]
     ]]])

(defn logged-in-navbar []
  (let [user (re-frame/subscribe [::subs/user])]
    [:nav.navbar.navbar-expand-lg
     [:a.navbar-brand {:href (routes/url-for :home)} "Home"]
     [:div#navbarNav.collapse.navbar-collapse
      [:ul.navbar-nav
       [:li.nav-item.dropdown
        [:a.nav-link.dropdown-toggle
         {:href          "#"
          :role          "button"
          :data-toggle   "dropdown"
          :aria-haspopup "true"
          :aria-expanded "false"}
         "Runs"]
        [:div.dropdown-menu
         [:a.dropdown-item {:href (routes/url-for :run-index)} "Index"]
         [:a.dropdown-item {:href (routes/url-for :recent-runs)} "Recent"]
         [:a.dropdown-item {:href (routes/url-for :latest-runs)} "Latest"]
         [:a.dropdown-item {:href (routes/url-for :run-form)} "New"]]]
       [:li.nav-item.dropdown
        [:a.nav-link.dropdown-toggle
         {:href          "#"
          :role          "button"
          :data-toggle   "dropdown"
          :aria-haspopup "true"
          :aria-expanded "false"}
         "Shoes"]
        [:div.dropdown-menu
         [:a.dropdown-item {:href (routes/url-for :shoe-index)} "Index"]
         [:a.dropdown-item {:href (routes/url-for :shoe-form)} "New"]]]
       [:li.nav-item
        [:a.nav-link {:href (routes/url-for :goal-index)} "Goals"]]
       [:li.nav-item
        [:a.nav-link {:href (routes/url-for :graph-page)} "Graphs"]]
       (when (seq @user)
         (logout-link))
       [:li.nav-item
        [:a.nav-link {:href (routes/url-for :about)} "About"]]]]]))

(defn navbar []
  (let [user (re-frame/subscribe [::subs/user])]
    (if (not (seq @user))
      (logged-out-navbar)
      (logged-in-navbar))
    ))

; Display a card containing the YTD mileage
(defn ytd-card [data]
  (fn [data]
    [:div.card
     [:div.card-body
      [:h5.card-title.text-primary "YTD Distance"]
      [:p.card-text
       (str "ytd distance is " (gstr/format "%.1f" (:distance data))
            " " (:units data))]]]))

(defn home-page []
  (let [user         (re-frame/subscribe [::subs/user])
        ytd-distance @(re-frame/subscribe [::subs/ytd-distance])]
    (if (not (seq @user))
      (login-form)
      (do
        (re-frame/dispatch [:get-ytd-distance])
        (when (not (nil? ytd-distance))
          [:div.col-sm-4
           [ytd-card ytd-distance]])))))

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
                                        ; same as above, should dispatch this event only if logged in
                                        (re-frame/dispatch [:load-runs])
                                        (run-views/run-index)))
(defmethod active-panel :recent-runs [] (do
                                          (re-frame/dispatch [:get-recent-runs])
                                          (run-views/recent-runs-table)))
(defmethod active-panel :latest-runs [] (run-views/latest-run-card))
(defmethod active-panel :run-page [id]
  [run-views/run-page (bidi/path-for routes/routes :run-page :id id)])
(defmethod active-panel :run-form [] (run-views/run-form ""))
(defmethod active-panel :shoe-index [] (shoe-views/shoe-index))
(defmethod active-panel :shoe-page [id]
  [shoe-views/shoe-page (bidi/path-for routes/routes :shoe-page :id id)])
(defmethod active-panel :shoe-form [] (shoe-views/shoe-form))
(defmethod active-panel :goal-index [] (goal-views/goal-index))
(defmethod active-panel :goal-page [id]
  [goal-views/goal-page (bidi/path-for routes/routes :goal-page :id id)])
(defmethod active-panel :graph-page [] [graphs/graph-page])
(defmethod active-panel :logout [] [:div "You're logged out"])
(defmethod active-panel :default [] [:div "default text"])