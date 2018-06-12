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
                           :description "Running Services"}}}}
  
  (context "/api" []
    :tags ["An API for running data"]

    (GET "/runs" []
      :summary "Return all runs"
      (ok (db/get-runs)))
    (GET "/runs" [runid]
      :summary "Return a run by ID"
      (ok (db/get-run runid)))

    ; These are sligtly different and maybe belong in a different context?
    (GET "/bydate" [rdate]
      :summary "Return runs that occurred on a given date"
      (ok (db/get-by-date rdate)))))
