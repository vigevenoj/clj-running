(ns running.core
  (:require [reagent.core :as r]
            [cljs-time.format :as format]
            [cljs-time.coerce :as c]
            [cognitect.transit :as t]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [running.ajax :refer [load-interceptors!]]
            [ajax.core :refer [GET POST]]
            [secretary.core :as secretary :include-macros true])
  (:import goog.History))

(defonce session (r/atom {:page :home}))
(defonce app-state (r/atom {:running-data []
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
     [nav-link "#/running" "Running" :running-page]]]])

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:img {:src "/img/warning_clojure.png"}]]]])

(defn home-page []
  [:div.container
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

(defn sorted-runs []
  (let [sorted-runs (sort-by (:sort-val @app-state) (:running-data @app-state))]
    (if (:ascending @app-state)
      sorted-runs
      (rseq sorted-runs))))

(defn run-row
  "Display a single run"
  [{:keys [runid rdate timeofday distance units elapsed comment effort shoeid] :as run}]
  [:tr {:key runid}
   [:td runid]
   [:td (format-date rdate)]
   [:td timeofday]
   [:td (if (true? distance)
          (.-rep distance))]
   [:td units]
   [:td elapsed]
   [:td comment]
   [:td effort]
   [:td shoeid]])

(defn run-display-table
  "Render a table of runs"
  []
  [:table
   [:thead
    [:tr
     [:th {:on-click #(update-sort-value :runid)} "ID"]
     [:th {:on-click #(update-sort-value :rdate)} "Date"]
     [:th "Time of Day"]
     [:th {:on-click #(update-sort-value :distance)} "Distance"]
     [:th "Units"]
     [:th {:on-click #(update-sort-value :elapsed)} "Elapsed"]
     [:th "Comment"]
     [:th "Effort"]
     [:th "Shoes"]]]
   [:tbody
    (when (seq (:running-data @app-state))
      (for [r (:running-data @app-state)]
        ^{:key (:runid r)}
        [run-row r]))]])

(defn get-runs
  "Get all runs"
  []
  (GET "/api/v1/runs/"
       {;:response-format :json
        :handler #(swap! app-state assoc :running-data %)}))

(defn running-page []
  [:div "text about running"]
  (if (empty? (:running-data @app-state))
    (get-runs))
  [:div
   (run-display-table)
   ])

(def pages
  {:home #'home-page
   :about #'about-page
   :running-page #'running-page})

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
