(ns running.views.graphs
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [rid3.core :as rid3 :refer [rid3->]]
            [goog.object :as gobj]))

; todo define a cursor or a subscription here

; todo define some colors

; todo fetch data or use locally-cached data (ie, running-data in the app db)

;

(defn graph-page []
  [:div "imagine a graph"])