(ns running.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[running started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[running has shut down successfully]=-"))
   :middleware identity})
