(ns running.handler
  (:require 
            [running.layout :refer [error-page]]
            [running.routes.home :refer [home-routes]]
            [running.routes.services :refer [service-routes]]
            [compojure.core :refer [routes wrap-routes]]
            [compojure.route :as route]
            [running.env :refer [defaults]]
            [mount.core :as mount]
            [running.middleware :as middleware]))

(mount/defstate init-app
  :start ((or (:init defaults) identity))
  :stop  ((or (:stop defaults) identity)))

(mount/defstate app
  :start
  (middleware/wrap-base
    (routes
      (-> #'home-routes
          (wrap-routes middleware/wrap-csrf)
          (wrap-routes middleware/wrap-formats))
          #'service-routes
          (route/not-found
             (:body
               (error-page {:status 404
                            :title "page not found"}))))))

