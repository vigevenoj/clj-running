(ns running.routes.services.goals
  (:require [clojure.tools.logging :as log]
            [running.db.core :as db]
            [running.routes.service.runs :refer [DistanceUnits]]
            [running.routes.services.common :refer [handler]]
            [schema.core :as s]
            [schema.coerce :as coerce]))

(def goal-type (s/enum "distance+pace" "total-distance"))

(s/defschema distance-pace-goal
  {:type           goal-type
   :distance       s/Num
   :distance-units DistanceUnits
   :pace           java.time.Duration})

(s/defschema total-distance-goal
  {:type           goal-type
   :distance       s/Num
   :distance-units DistanceUnits})

(s/defschema Goal
  {(s/optional-key :goalid) (s/maybe s/Num)
   (:start-date java.time.LocalDate)
                            :end-date java.time.LocalDate
                            :is-met? s/Bool
                            :target (s/either distance-pace-goal total-distance-goal)})

(handler all-goals []
         (not-implemented))

(handler goal [goalid]
         (not-implemented))
