(ns running.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [running.db.core :as db]))

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
        :summary "Return all runs"
        (ok (db/get-runs)))
      (GET "/:runid" []
        :path-params [runid :- Long]
        :summary "Return a run by ID"
        (ok (db/get-run {:runid runid})))
      ; todo need to add authentication for POST/PUT/DELTE
      (POST "/" []
        :summary "Add a new run"
        (ok "not implemented"))
      (PUT "/" []
        :summary "Update an existing run"
        (ok "not implemented"))
      (DELETE "/:runid" []
        :summary "Delete a run"
        (ok "not implemented"))

      (GET "/bydate" []
        :query-params [rdate]
        :summary "Return runs that occurred on a given date"
        (ok (db/get-by-date {:rdate rdate}))))

    ; This context is for statistics about runs
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
