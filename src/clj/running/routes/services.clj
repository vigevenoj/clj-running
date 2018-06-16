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


; This information is used for  coercion from #inst to LocalDate
;(def date-regex #"\d{4}-\d{2}-\d{2}")
;(defn date-matcher [schema]
;  (when (= java.time.LocalDate schema)
;    (coerce/safe
;      (fn [x]
;        (if (and (string? x) (re-matches date-regex x))
;          (t/local-date x)
;          x)))))
;
;(defn coerce-and-validate [schema matcher data]
;  (let [coercer (coerce/coercer schema matcher)
;        result (coercer data)]
;    (if (schema.utils/error? result)
;      (throw (Exception. (format "Value does not match schema: %s"
;                                 (schema.utils/error-val result))))
;      result)))





;(def run-matcher (coerce/first-matcher [date-matcher coerce/json-coercion-matcher]))


(defapi service-routes
  {:swagger {:ui   "/swagger-ui"
             :spec "/swagger.json"
             :data {:info {:version     "1.0.0"
                           :title       "Running API"
                           :description "Running Services"}
                    :tags [{:name "runs" :description "Runs"}
                           {:name "statistics" :description "Statistics about runs"}]}}}

  (context "/api/v1" []
    :tags ["An API for running data"]

    (context "/running" []
             ; This context is about runs themselves
             (context "/runs" []
               :tags ["runs"]
               (GET "/" []
                 :return [runs/Run]
                 :summary "Return all runs"
                 (runs/all-runs))
               (GET "/bydate" []
                 :query-params [rdate :- s/Any]
                 :return [runs/Run]
                 :summary "Return runs that occurred on a given date"
                 (runs/runs-by-date rdate))

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
                 (not-implemented "not implemented"))




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
                                            :max-distance shorterThan}))))

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
