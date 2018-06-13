(ns running.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [running.db.core :as db]))

; define some schemas
(def TimeOfDay (s/enum "am" "pm" "noon" "night"))

(def DistanceUnits (s/enum "m" "km" "mile" "miles" "meters"))

(s/defschema Run
  {:runid s/Num
   :rdate s/Inst
   :timeofday TimeOfDay
   (s/optional-key :distance) s/Num
   (s/optional-key :units) DistanceUnits
   (s/optional-key :elapsed) s/Any
   (s/optional-key :comment) s/Str
   (s/optional-key :effort) s/Str
   (s/optional-key :shoeid) (s/maybe s/Num)})

(s/defschema RunResult
  {(s/optional-key :run) Run
   (s/optional-key :error) s/Str})

(s/defschema RunsResult
  {(s/optional-key :runs) [Run]
  (s/optional-key :error) s/Str})

(defapi service-routes
  {:swagger {:ui "/swagger-ui"
             :spec "/swagger.json"
             :data {:info {:version "1.0.0"
                           :title "Running API"
                           :description "Running Services"}
                    :tags [{:name "runs" :description "Runs"}
                           {:name "statistics" :description "Statistics about runs"}]}}}
  
  (context "/api/v1" []
    :tags ["An API for running data"]
    ; This context is about runs themselves
    (context "/runs" []
      :tags ["runs"]
      (GET "/" []
        :return RunsResult
        :summary "Return all runs"
        (ok (db/get-runs)))
      (GET "/:runid" []
        :return RunResult
        :path-params [runid :- Long]
        :summary "Return a run by ID"
        (ok (db/get-run {:runid runid})))
      ; todo need to add authentication for POST/PUT/DELTE
      (POST "/" []
        :body-params [rdate
                      timeofday :- TimeOfDay
                      distance :- s/Num
                      units :- DistanceUnits
                      elapsed :- s/Any
                      comment :- s/Any
                      effort :- s/Any
                      shoeid :- s/Int]
        :return s/Int
        :summary "Add a new run"
        (ok "not implemented"))
      (PUT "/" []
        :body-params [runid :- s/Int
                      rdate
                      timeofday :- TimeOfDay
                      distance :- s/Num
                      units :- DistanceUnits
                      elapsed :- s/Any
                      comment :- s/Any
                      effort :- s/Any
                      shoeid :- s/Int]
        :return s/Int
        :summary "Update an existing run"
        (ok "not implemented"))
      (DELETE "/:runid" []
        :path-params [runid :- s/Int]
        :return s/Int
        :summary "Delete a run"
        (ok "not implemented"))

      (GET "/bydate" []
        :query-params [rdate]
        :return RunsResult
        :summary "Return runs that occurred on a given date"
        (ok (db/get-by-date {:rdate rdate}))))

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
        (ok "not implemented")))))
