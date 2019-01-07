(ns running.routes.services.goals
  (:require [clojure.tools.logging :as log]
            [java-time :as jt]
            [ring.util.http-response :refer :all]
            [running.db.core :as db]
            [running.routes.services.runs :refer [DistanceUnits ElapsedDuration]]
            [running.routes.services.common :refer [handler]]
            [schema.core :as s]
            [schema.coerce :as coerce]))

(s/defschema Goal
  {(s/optional-key :goalid) (s/maybe s/Num)
   :start-date              java.time.LocalDate
   :end-date                java.time.LocalDate
   :is-met?                 s/Bool
   :target                  (s/conditional #(= (:type %) "distance+pace") {:type           (s/eq "distance+pace")
                                                                           :distance       s/Num
                                                                           :distance-units DistanceUnits
                                                                           :pace           ElapsedDuration}
                                           #(= (:type %) "total-distance") {:type           (s/eq "total-distance")
                                                                            :distance       s/Num
                                                                            :distance-units DistanceUnits})})

(s/defschema GoalsResult
  {(s/optional-key :goals) [Goal]
   (s/optional-key :error) s/Str})

(s/defschema GoalResult
  {(s/optional-key :goal)  Goal
   (s/optional-key :error) s/Str})

(def test-distance-goal
  {:start-date (jt/local-date "2019-01-01")
   :end-date   (jt/local-date "2019-12-31")
   :is-met?    false
   :target     {:type           "total-distance"
                :distance       2000
                :distance-units "miles"}})

(def test-pace-goal
  {:start-date (jt/local-date "2019-01-01")
   :end-date   (jt/local-date "2019-12-31")
   :is-met?    false
   :target     {:type           "distance+pace"
                :distance       10
                :distance-units "miles"
                :pace           (jt/duration "PT59M59S")}})

(handler all-goals []
         [test-distance-goal test-pace-goal])

(handler goal-by-id [goalid]
         {:goal test-distance-goal})
