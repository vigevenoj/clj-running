(ns running.views.graphs
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [re-frame.core :refer [subscribe]]
            [running.subscriptions :as subscriptions]
            [rid3.core :as rid3 :refer [rid3->]]
            [goog.object :as gobj]))

; todo define a cursor or a subscription here

; todo define some colors

; todo fetch data or use locally-cached data (ie, running-data in the app db)

;

(defn heatmap []
  (let [height 136
        width 900
        cell-size 16]
    (fn []
      [:h4 "text"]
      [rid3/viz
       {:id "heatmap"
        :ratom (subscribe [::subscriptions/heatmap-data])
        :svg {:did-mount (fn [node ratom]
                           (rid3-> node
                                   {:height height
                                    :width width}))}
        :pieces
        [{:kind      :elem
          :class     "some-element"
          :tag       "rect"
          :did-mount (fn [node ratom]
                       (rid3-> node
                               {:x            0
                                :y            0
                                :height       height
                                :width        width
                                ;; You can add arbitrary attributes to
                                ;; your element
                                :fill         "lightgrey"}))}]}])))

(defn graph-page []
  [:div "imagine a graph"]
  [heatmap])