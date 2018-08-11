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

(defn admin?
  [request]
  (:admin (:identity request)))


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

  (context "/api/v1" []
    :tags []

    ;; Admin context
    (context "/admin" []
      :auth-rules admin?
      :tags ["Administrative actions"]

      ;; User operations
      (GET "/user" []
        :summary "Get users, filtered by name"
        :query-params [{name :- s/Str ""}]
        (ok
          {:users
           (db/get-users-by-name
             {:name (str "%" name "%")})}))

      (GET "/user/:name" []
        :path-params [name]
        :summary "Get a user by name"
        (let [user (db/get-user-by-name {:name name})]
          (if (nil? user)
            (not-found)
            (ok (dissoc user :pass)))))

      (POST "/user" []
        :body-params [name :- s/Str
                      email :- s/Str
                      pass :- s/Str
                      pass-confirm :- s/Str
                      admin :- s/Bool
                      is-active :- s/Bool]
        :summary "Create a new user"
        (auth/create-user! {:name name
                            :email email
                            :pass pass
                            :pass-confirm pass-confirm
                            :admin admin
                            :is-active is-active}))

      (PUT "/user" []
        :body-params [user-id :- s/Int
                      name :- s/Str
                      email :- s/Str
                      pass :- (s/maybe s/Str)
                      pass-confirm :- (s/maybe s/Str)
                      admin :- s/Bool
                      is-active :- s/Bool]
        :summary "Update a user"
        :return auth/LoginResponse
        (auth/update-user! {:user-id user-id
                            :name name
                            :pass pass
                            :pass-confirm pass-confirm
                            :admin admin
                            :is-active is-active}))

      (DELETE "/user/:name" []
        :path-params [name]
        :summary "Delete a user"
        (db/delete-user! {:name name})))

    ; /api/v1/running/
    (POST "/login" req
      :return auth/LoginResponse
      :body-params [username :- s/Str
                    pass :- s/Str]
      :summary "User login endpoint"
      (auth/login username pass req))
    (POST "/logout" []
      :return auth/LogoutResponse
      :summary "Remove the user from the session and invalidate their authentication tokens"
      (auth/logout))

    (context "/running" []
      :tags ["Running data"]
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
          :auth-rules authenticated?
          :current-user user ; todo add column for userid into run table
          (ok run))
        (PUT "/" []
          :body [run runs/Run]
          ;:body-params [runid :- s/Int
          ;              rdate :- s/Any
          ;              timeofday :- runs/TimeOfDay
          ;              distance :- s/Num
          ;              units :- runs/DistanceUnits
          ;              elapsed :- s/Any
          ;              comment :- s/Any
          ;              effort :- s/Any
          ;              shoeid :- s/Int]
          :return runs/Run
          :summary "Update an existing run"
          :auth-rules authenticated?
          :current-user user ; todo add column for userid into run table
          (ok run))
        (DELETE "/:runid" []
          :path-params [runid :- s/Int]
          :return s/Int
          :summary "Delete a run"
          :auth-rules authenticated?
          :current-user user ; todo add column for userid into run table
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
