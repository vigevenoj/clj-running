(ns running.routes.services.shoes
  (:require [clojure.tools.logging :as log]
            [ring.util.http-response :refer :all]
            [running.db.core :as db]
            [running.routes.services.runs :refer [DistanceUnits]]
            [running.routes.services.common :refer [handler]]
            [schema.core :as s]
            [schema.coerce :as coerce]))


; A schema for shoes
(s/defschema Shoe
  {(s/optional-key :shoeid)   (s/maybe s/Num)
   :name                      s/Str
   :description               s/Str
   :cumulative-distance       s/Num
   :cumulative-distance-units DistanceUnits
   :distance-expiration       s/Num
   :distance-expiration-units DistanceUnits
   :is-active                    s/Bool})

(s/defschema ShoeResult
  {(s/optional-key :shoe)  Shoe
   (s/optional-key :error) s/Str})

(s/defschema ShoesResult
  {(s/optional-key :shoes) [Shoe]
   (s/optional-key :error) s/Str})

(handler all-shoes []
         (db/get-all-shoes))

(handler shoe [shoeid]
         (db/get-shoe {:shoeid shoeid}))
