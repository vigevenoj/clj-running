(ns running.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [running.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[running started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[running has shut down successfully]=-"))
   :middleware wrap-dev})
