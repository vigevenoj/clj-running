(ns running.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.json-schema :as json-schema]
            [running.db.core :as db]
            [schema.coerce :as coerce]
            [java-time :as jt]
            [running.routes.services.runs :as runs]
            [running.routes.services.auth :as auth]
            [clojure.tools.logging :as log]
            [muuntaja.core :as muuntaja]
            [cognitect.transit :as transit]
            [compojure.api.meta :refer [restructure-param]]
            [buddy.auth.accessrules :refer [restrict]]
            [buddy.auth :refer [authenticated?]]))

(defn access-error [_ _]
  (unauthorized {:error "unauthorized"}))

(defn wrap-restricted [handler rule]
  (restrict handler {:handler rule
                     :on-error access-error}))

(defmethod restructure-param :auth-rules
  [_ rule acc]
  (update-in acc [:midleware] conj [wrap-restricted rule]))

(defmethod restructure-param :current-user
  [_ binding acc]
  (update-in acc [:letks] into [binding `(:identity ~'+compojure-api-request+)]))

(defmethod json-schema/convert-class java.time.Duration [_ _] {:type "string"})

(def DurationSchema java.time.Duration)
(s/defschema myDuration
  {:duration (s/either DurationSchema s/Str)})

(defn elapsed-duration-matcher [schema]
  (when (= myDuration schema)
    (coerce/safe
      (fn [x]
        (if (and (string? x) (or
                               (re-matches runs/iso-duration-regex x)
                               (re-matches runs/hhmmss-regex x)))
          db/string-duration-to-duration x)))))

(def java-time-localdate-handler
  (transit/write-handler
    (constantly "LocalDate")
    (fn [v] (-> v .toString))))

(def java-time-duration-handler
  (transit/write-handler
    (constantly "Duration")
    (fn [v] (-> v .toString))))

(def write-handlers
  {java.time.LocalDate java-time-localdate-handler
   java.time.Duration java-time-duration-handler})

; probably going to need a bridge for https://github.com/FasterXML/jackson-modules-java8
; in order to serialize java.time LocalDate and Duration to json
; but that doesn't solve the other problem, with cljs not reading our transit response right

(def m
  (muuntaja/create
   (update-in
     muuntaja/default-options
     [:formats "application/transit+json"]
     merge {:decoder-opts {}
            :encoder-opts {:handlers write-handlers}})))

(def service-routes (api
                      {:swagger {:ui   "/swagger-ui"
                                 :spec "/swagger.json"
                                 :data {:info {:version     "1.0.0"
                                               :title       "Running API"
                                               :description "Running Services"}
                                        :tags [{:name "runs" :description "Runs"}
                                               {:name "statistics" :description "Statistics about runs"}]}
                                 }
                       :formats m}

                      (GET "/authenticated" []
                        :auth-rules authenticated?
                        :current-user user
                        (ok {:user user}))

  (context "/api/v1" []
    :tags ["Running data"]
    ; /api/v1/running/
    (POST "/login" req
      :return auth/LoginResponse
      :body-params [username :- s/Str
                    pass :- s/Str]
      :summary "User login endpoint"
      (auth/login username pass req))
    (context "/running" []
      (GET "/bydate" []
        :query-params [rdate :- s/Any]
        :return [runs/Run]
        :summary "Return runs that occurred on a given date"
        (runs/runs-by-date rdate))
      (GET "/recent/:days" []
        :path-params [days :- s/Int]
        :return [runs/Run]
        :summary "Return runs from the past [days] days"
        (runs/recent-runs [days]))
      (GET "/filter" []
        :query-params [{before :- (describe String "Runs before this date") ""}
                       {after :- (describe String "Runs after this date") ""}
                       {longerThan :- (describe s/Num "Runs longer than this") 0}
                       {shorterThan :- (describe s/Num "Runs shorter than this") 100}
                       {fasterThan :- (describe s/Any "Runs faster than this pace") nil}
                       {slowerThan :- (describe s/Any "Runs slower than this pace") nil}]
        :return runs/RunsResult
        :summary "Returns runs that match the filters provided"
        (ok (db/get-filtered-runs {:before-date  before
                                   :after-date   after
                                   :min-distance longerThan
                                   :max-distance shorterThan})))

      ; This context is about runs themselves
      ; /api/v1/running/runs
      (context "/runs" []
        :tags ["runs"]
        (GET "/" []
          :return [runs/Run]
          :summary "Return all runs"
          (runs/all-runs))
        (GET "/:runid" []
          :return runs/Run
          :path-params [runid :- Long]
          :summary "Return a run by ID"
          (runs/run runid))
        ; todo need to add authentication for POST/PUT/DELTE
        (POST "/" []
          :body [run runs/Run]
          ;:body-params [rdate :- s/Any
          ;              timeofday :- runs/TimeOfDay
          ;              distance :- s/Num
          ;              units :- runs/DistanceUnits
          ;              elapsed :- s/Any
          ;              comment :- s/Any
          ;              effort :- s/Any
          ;              shoeid :- s/Int]
          :return runs/Run
          :summary "Add a new run"
          (ok run))
        (PUT "/" []
          :body-params [runid :- s/Int
                        rdate :- s/Any
                        timeofday :- runs/TimeOfDay
                        distance :- s/Num
                        units :- runs/DistanceUnits
                        elapsed :- s/Any
                        comment :- s/Any
                        effort :- s/Any
                        shoeid :- s/Int]
          :return s/Int
          :summary "Update an existing run"
          (not-implemented "not implemented"))
        (DELETE "/:runid" []
          :path-params [runid :- s/Int]
          :return s/Int
          :summary "Delete a run"
          (not-implemented "not implemented")))

      ; This context is for statistics about runs
      ; todo: need to scope out what I want to include in the response
      ; I think I want: total distance, units, total elapsed time, fastest pace per unit distance, avg pace per unit distance
      (context "/statistics" []
        :tags ["statistics"]
        (GET "/current" []
          :path-params [period]
          :summary "Get information about runs during current [week|month|year]"
          (ok "not implemented"))
        (GET "/rolling" []
          :path-params [period]
          :summary "Get information about runs during the past [week|month|90 days|180 days|year]"
          (ok "not implemented")))))))
