(ns running.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [day8.re-frame.http-fx]
            [reagent-modals.modals :as modals]
            [running.util :refer [format-date format-duration]]
            [running.events] ; Needed so the closure compiler loads it
            [running.subscriptions] ; Needed so the closure compiler loads it
            [clojure.string :as string]
            [cognitect.transit :as t]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [running.ajax :refer [load-interceptors!]]
            [running.routes :as routes]
            [ajax.core :refer [GET POST]])

(defonce session (r/atom {:page :home}))
(defonce app-state (r/atom {:running-data []
                            :recent-runs []
                            :checked-recent false
                            :checked-latest false
                            :sort-val :runid :ascending true
                            :user {}})) ; user { :token token :identity session }



(defn nav-link [uri title page]
  [:li.nav-item
   {:class (when (= page (:page @session)) "active")
    :key page}
   [:a.nav-link {:href uri} title]])

(defn logout-handler [_]
  (.log js/console (str "Logging user " (:user-id (:user @app-state)) " out of the application"))
  (swap! app-state assoc :user {}))

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
     (when (seq (:user @app-state))
       (for [nav-item (list [nav-link "#/running" "Runs" :running-page]
                            ;[nav-link "#/recent" "Recent" :running-recent]
                            ;[nav-link "#/graphs" "Graphs" :running-graph]
                            [nav-link "#/latest" "Latest" :latest]
                            [logout-link])]
         ^{:key (str nav-item)} nav-item))]]])

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:img {:src "/img/warning_clojure.png"}]]]])

(defn login-handler [response]
  (.log js/console (str response))
  (swap! app-state assoc :user (:user response)))

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
            :id "password"
            :placeholder "Password"}]
   [:button {:type :submit} "Login"]])

(defn run-form [id]
  (let [value (atom nil)]
    [:div.runform
     [:form {:on-submit (fn [e]
                          (.preventDefault e)
                          (.log js/console "run form submitted"))}
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
      [:button {:type :submit} "Save"]]]))

(defn get-latest-runs
  "Get the latest [count] runs"
  [count]
  ;
  (GET "/api/v1/running/latest" ; default limit is 1
       {:response-format :json
        :keywords? true?
        :handler #(swap! app-state assoc :latest-runs %)}))

(defn latest-run-card
  []
  (let [run (first  (:latest-runs @app-state))]
    (fn []
      (when (and (empty? (:latest-runs @app-state)) (not (:checked-latest @app-state)))
        (do
          (get-latest-runs 1)
          (swap! app-state assoc :checked-latest true)))
      [:div.runcard
       {:id (:runid run)
        :style  {:border "1px solid black"
                 :padding 20
                 :margin 10
                 :display "inline-block"
                 :max-width "50%"
                 }}
       [:span.runcard-title
        [:span.runcard-date {:style {:padding-right 2}} (:rdate run)]
        [:span.runcard-tod  {:style {:padding-left 2}}(:timeofday run)]]
       [:span.runcard-distance {:style {:display "block"}}
        [:span {:style {:padding-right 2}} (:distance run)]
        [:span {:style {:padding-left 2}} (:units run)]]
       [:span (format-duration (:elapsed run))]])))

(defn home-page []
  [:div.container
   [modals/modal-window]
   (if (not (seq (:user @app-state)))
     (login-form)
     (run-form "runform"))])

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
  (r/with-let [data (r/atom data)]
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
                    [run-row-ui r]))]]))


(defn get-runs
  "Get all runs"
  []
  (GET "/api/v1/running/runs/"
       {:response-format :json
        :keywords? true
        :handler #(swap! app-state assoc :running-data %)}))

(defn get-recent-runs
  "Get recent runs"
  []
  (GET "/api/v1/running/recent/90"
       {:response-format :json
        :keywords? true
        :handler #(do (swap! app-state assoc :recent-runs  % )
                      (swap! app-state assoc :checked-recent true))}))


(defn recent-runs-page []
   ; if the response is empty, this keeps being true and we keep making requests
   (when (and (empty? (:recent-runs @app-state)) (not (:checked-recent @app-state)))
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

(defn latest-page []
  [:div "latest page"]
  (latest-run-card))

(def pages
  {:home #'home-page
   :about #'about-page
   :running-page #'running-page
   :running-recent #'recent-runs-page
   :running-graph #'running-graph-page
   :latest #'latest-page})

(defn page []
  [(pages (:page @session))])



;; -------------------------
;; Initialize app
;(defn fetch-docs! []
;  (GET "/docs" {:handler #(swap! session assoc :docs %)}))

(defn mount-components []
  ;(r/render [#'navbar] (.getElementById js/document "navbar"))
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (load-interceptors!)
  ;(fetch-docs!)
  (routes/hook-browser-navigation!)
  (mount-components))
