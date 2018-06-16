(ns running.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [running.db.core :as db]
            [schema.coerce :as coerce]
            [java-time :as jt]
            [clj-time.format :as f]
            [running.routes.services.runs :as runs]
    ;[clj-time.core :as t]
            [clojure.tools.logging :as log])
  (:import java.text.SimpleDateFormat))


(defapi service-routes
  {:swagger {:ui   "/swagger-ui"
             :spec "/swagger.json"
             :data {:info {:version     "1.0.0"
                           :title       "Running API"
                           :description "Running Services"}
                    :tags [{:name "runs" :description "Runs"}
                           {:name "statistics" :description "Statistics about runs"}]}}}

  (context "/api/v1" []
    :tags ["Running data"]
    ; /api/v1/running/
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
          :body-params [rdate :- s/Any
                        timeofday :- runs/TimeOfDay
                        distance :- s/Num
                        units :- runs/DistanceUnits
                        elapsed :- s/Any
                        comment :- s/Any
                        effort :- s/Any
                        shoeid :- s/Int]
          :return s/Int
          :summary "Add a new run"
          (not-implemented "not implemented"))
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
          (ok "not implemented"))))))
