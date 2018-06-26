(ns running.test.handler
  (:require [clojure.test :refer :all]
            [ring.mock.request :refer :all]
            [running.handler :refer :all]
            [mount.core :as mount]
            [schema.coerce :as coerce]
            [schema.core :as s]
            [running.routes.services.runs :as runs]
            [clj-time.coerce :as c]))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'running.config/env
                 #'running.handler/app)
    (f)))

(deftest test-app
  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= 404 (:status response))))))

;(deftest test-runs
;  (testing "add a run"
;    (let [response (app (request :post "/api/v1/running/runs"))])))

;(deftest test-validation
;  (s/validate runs/Run
;              {:runid     0
;               :rdate     (c/to-sql-date "2018-05-16")
;               :timeofday "noon"
;               :distance  10.2
;               :units     "miles"
;               :elapsed   "PT1H30M6S"
;               :comment   nil
;               :effort    nil
;               :shoeid    nil})
;  (s/validate runs/Run
;              {:runid     0
;               :rdate     (c/to-sql-date "2018-05-16")
;               :timeofday "noon"
;               :distance  10.2
;               :units     "miles"
;               :elapsed   "01:30:06"
;               :comment   nil
;               :effort    nil
;               :shoeid    nil})
;  )