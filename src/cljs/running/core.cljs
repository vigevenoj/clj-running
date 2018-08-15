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
  (:import goog.History))

(defonce session (r/atom {:page :home}))
(defonce app-state (r/atom {:running-data []
                            :recent-runs []
                            :sort-val :runid :ascending true
                            :user {}})) ; user { :token token :identity session }

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
     ; todo show only home/about in nav when not logged in
     ; todo show logout link in nav when logged in
     [nav-link "#/" "Home" :home]
     [nav-link "#/about" "About" :about]
     (when (seq (:user @app-state))
       (list ; this warns about unique keys (javascript console error every page view, so
         ; look at https://stackoverflow.com/questions/38242295/how-to-conditionally-unroll-hiccup-data-structures-in-clojure
         ; for alternatives
       [nav-link "#/running" "Runs" :running-page]
       [nav-link "#/recent" "Recent" :running-recent]
       [nav-link "#/graphs" "Graphs" :running-graph]
       [nav-link "#/logout" "Logout" :about]))]]])

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:img {:src "/img/warning_clojure.png"}]]]])

(defn login-handler [response]
  (.log js/console (str response))
  (swap! app-state assoc :user (:user response)))

(defn logout-handler [_]
  (.log js/console ("Logging user " (:user @app-state) "out of the application"))
  (swap! app-state assoc :user {}))

(defn login-form []
  ; need to watch (:user @app-state), if present change navbar to include logout link
  [:form
   {:on-submit
          (fn [e]
            (.preventDefault e)
            (ajax.core/POST "/api/v1/login"
                            {:response-format :json
                             :keywords? true
                             :params {
                                      :username (.. e -target -elements -username -value)
                                      :pass (.. e -target -elements -password -value)}
                             :handler login-handler}))}
   [:label "Username: "]
   [:input {:type "text"
            :name "username"
            :id "username"
            :placeholder "Username"}]
   [:label "Password: "]
   [:input {:type "password"
            :name "password"
            :id "password"}]
   [:button {:type :submit} "Login"]])

(defn run-form [id]
  (let [value (atom nil)]
  [:div.runform
   [:span.rundate
   [:input {:type "text" :placeholder "Date"}]]
   [:span
    [:select
     [:option "am"]
     [:option "pm"]
     [:option "noon"]
     [:option "night"]]]
    [:span
     [:input {:type "text" :placeholder "Distance"}]]
    [:span
     [:select {:default-value "miles"}
      [:option "km"]
      [:option "m"]
      [:option "miles"]
      ]]
    [:span
     [:input {:type "text" :placeholder "Elapsed time"}]]
    [:span
     [:input {:type "text" :placeholder "Comments"}]]
   [:button {:type :submit} "Save"]]))

(defn home-page []
  [:div.container
   (if (not (seq (:user @app-state)))
            (login-form)
            (run-form "runform"))])

(defn format-date [date]
  ;(format/unparse (format/formatter "yyyy-MM-dd")
  ;           (c/from-date date))
  (str date))

(defn format-duration
  "Format an ISO-8601 style duration into something more familiar"
  [duration]
  (let [duration-regex (re-pattern "^P(?!$)([0-9]+Y)?([0-9]+M)?([0-9]+W)?([0-9]+D)?(T(?=[0-9])([0-9]+H)?([0-9]+M)?([0-9]+S)?)?$")]
    ; The 6th through 9th elements are hours, minutes, and seconds.
    ; I don't expect any of our durations to be longer than that but if they are we can test for it
    (clojure.string/join " " (subvec (re-find duration-regex duration) 6 9))))

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

(defn run-row-ui
  "Display a single run"
  [{:keys [runid rdate timeofday distance units elapsed comment effort] :as run}]
  [:tr {:key runid}
   [:td (format-date rdate)]
   [:td timeofday]
   [:td (if (not (nil? distance))
          (str distance))]
   [:td units]
   [:td.duration (format-duration elapsed)]
   [:td comment]
   [:td effort]])

(defn run-display-table-ui
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
        [run-row-ui r]))]])

(defn get-runs
  "Get all runs"
  []
  (GET "/api/v1/running/runs/"
       {:response-format :json
        :keywords? true
        :handler #(swap! app-state assoc :running-data %)}))

;(defn recent-handler [response]
;  (.log js/console (str response))
;  (.log js/console (t/read (t/reader :json) response))
;  (swap! app-state assoc :recent-runs (t/read (t/reader :json) response)))


(defn get-recent-runs
  "Get recent runs"
  []
  (GET "/api/v1/running/recent/90"
       {:response-format :json
        :keywords? true
        :handler #(swap! app-state assoc :recent-runs  % )}))


(defn recent-runs-page []
   (if (empty? (:recent-runs @app-state))
     (get-recent-runs))
     [:div
      (run-display-table-ui (:recent-runs @app-state))])

(defn running-page []
  (if (empty? (:running-data @app-state))
    (get-runs))
  [:button
   {:type "button"}]
  [:div
   (run-display-table-ui (:running-data @app-state))])

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
;(defn fetch-docs! []
;  (GET "/docs" {:handler #(swap! session assoc :docs %)}))

(defn mount-components []
  (r/render [#'navbar] (.getElementById js/document "navbar"))
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (load-interceptors!)
  ;(fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
