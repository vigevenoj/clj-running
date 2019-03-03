(ns running.routes.home
  (:require [running.layout :as layout]
            [running.db.core :as db]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]))

(defn home-page []
  (layout/render "home.html"))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/about" [] (home-page))
  (GET "/goals/*" [] (home-page))
  (GET "/shoes/*" [] (home-page))
  (GET "/runs/*" [] (home-page))
  (GET "/docs" []
       (-> (response/ok (-> "docs/docs.md" io/resource slurp))
           (response/header "Content-Type" "text/plain; charset=utf-8"))))

