(ns running.core
  (:require [reagent.core :as r]
            [clojure.string :as string]
            [cljs-time.format :as format]
            [cljs-time.coerce :as c]
            [cognitect.transit :as t]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [running.ajax :refer [load-interceptors!]]
            [ajax.core :refer [GET POST]]
            [secretary.core :as secretary :include-macros true])
  ;(:use [hiccup form])
  (:import goog.History))

(defonce session (r/atom {:page :home}))
(defonce app-state (r/atom {:running-data []
                            :recent-runs []
                            :sort-val :runid :ascending true}))

(defn nav-link [uri title page]
  [:li.nav-item
   {:class (when (= page (:page @session)) "active")}
   [:a.nav-link {:href uri} title]])

(defn navbar []
  [:nav.navbar.navbar-dark.bg-primary.navbar-expand-md
   {:role "navigation"}
   [:button.navbar-toggler.hidden-sm-up
    {:type "button"
     :data-toggle "collapse"
     :data-target "#collapsing-navbar"}
    [:span.navbar-toggler-icon]]
   [:a.navbar-brand {:href "#/"} "running"]
   [:div#collapsing-navbar.collapse.navbar-collapse
    [:ul.nav.navbar-nav.mr-auto
     [nav-link "#/" "Home" :home]
     [nav-link "#/about" "About" :about]
     [nav-link "#/running" "Runs" :running-page]
     [nav-link "#/recent" "Recent" :running-recent]
     [nav-link "#/graphs" "Graphs" :running-graph]]]])

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:img {:src "/img/warning_clojure.png"}]]]])

(defn login-form []
  [:form {:action "/login", :method "post"}
   [:label "Username: "]
   [:input {:name "username", :id "username", :placeholder "Username"}]
   [:label "Password: "]
   [:input {:type "password", :name "password", :id "password"}]])

(defn home-page []
  [:div.container
   (login-form)
   (when-let [docs (:docs @session)]
     [:div.row>div.col-sm-12
      [:div {:dangerouslySetInnerHTML
             {:__html (md->html docs)}}]])])

(defn format-date [date]
  (format/unparse (format/formatter "yyyy-MM-dd")
             (c/from-date date)))

(defn update-sort-value [new-val]
  (if (= new-val (:sort-val @app-state))
    (swap! app-state update-in [:ascending] not)
    (swap! app-state assoc :ascending true))
  (swap! app-state assoc :sort-val new-val))

(defn sorted-runs [data]
  (let [sorted-runs (sort-by (:sort-val @app-state) data)]
    (if (:ascending @app-state)
      sorted-runs
      (rseq sorted-runs))))

(defn run-row
  "Display a single run"
  [{:keys [runid rdate timeofday distance units elapsed comment effort] :as run}]
  [:tr {:key runid}
   [:td (format-date rdate)]
   [:td timeofday]
   [:td (if (not (nil? distance))
          (.-rep distance))]
   [:td units]
   [:td.duration elapsed]
   [:td comment]
   [:td effort]])

(defn run-display-table
  "Render a table of runs"
  [data]
  [:table.runningData
   [:thead
    [:tr
     [:th {:width "200" :on-click #(update-sort-value :rdate) } "Date"]
     [:th {:width "200" } "Time of Day"]
     [:th {:width "200" } "Distance"]
     [:th "Units"]
     [:th {:width "200" :on-click #(update-sort-value :elapsed) } "Elapsed"]
     [:th {:width "200" } "Comment"]
     [:th {:width "200" } "Effort"]]]
   [:tbody
    (when (seq data)
      (for [r (sorted-runs data)]
        ^{:key (:runid r)}
        [run-row r]))]])

(defn get-runs
  "Get all runs"
  []
  (GET "/api/v1/running/runs/"
       {;:response-format :json
        :handler #(swap! app-state assoc :running-data %)}))

(defn get-recent-runs
  "Get recent runs"
  []
  (GET "/api/v1/running/recent/90"
       {:handler #(swap! app-state assoc :recent-runs %)}))


(defn recent-runs-page []
   (if (empty? (:recent-runs @app-state))
     (get-recent-runs))
     [:div
      (run-display-table (:recent-runs @app-state))])

(defn running-page []
  (if (empty? (:running-data @app-state))
    (get-runs))
  [:button
   {:type "button"}]
  [:div
   (run-display-table (:running-data @app-state))])

(defn running-graph-page []
  [:div])

(def pages
  {:home #'home-page
   :about #'about-page
   :running-page #'running-page
   :running-recent #'recent-runs-page
   :running-graph #'running-graph-page})

(defn page []
  [(pages (:page @session))])

;; -------------------------
;; Routes

(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (swap! session assoc :page :home))

(secretary/defroute "/about" []
  (swap! session assoc :page :about))

(secretary/defroute "/running" []
  (swap! session assoc :page :running-page))

(secretary/defroute "/recent" []
  (swap! session assoc :page :running-recent))

(secretary/defroute "/graph" []
  (swap! session assoc :page :running-graph))

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

;; -------------------------
;; Initialize app
(defn fetch-docs! []
  (GET "/docs" {:handler #(swap! session assoc :docs %)}))

(defn mount-components []
  (r/render [#'navbar] (.getElementById js/document "navbar"))
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (load-interceptors!)
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
