(ns running.db
  (:require [cljs.reader]
            [cljs.spec.alpha :as s]
            [re-frame.core :as rf]))

(def default-db
  {:page :home
   :running-data []
   :sort-value :runid
   :ascending true
   :recent {:checked-recent false
            :recent-runs []}
   :latest {:checked-latest false
            :latest-runs []}
   :user {}})
