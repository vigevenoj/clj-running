(ns running.middleware
  (:require [running.env :refer [defaults]]
            [cheshire.generate :as cheshire]
            [cognitect.transit :as transit]
            [clojure.tools.logging :as log]
            [running.layout :refer [error-page]]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [ring.middleware.webjars :refer [wrap-webjars]]
            [muuntaja.core :as muuntaja]
            [muuntaja.format.json :refer [json-format]]
            [muuntaja.format.transit :as transit-format]
            [muuntaja.middleware :refer [wrap-format wrap-params]]
            [running.config :refer [env]]
            [ring.middleware.flash :refer [wrap-flash]]
            [immutant.web.middleware :refer [wrap-session]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]))

(defn wrap-internal-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t (.getMessage t))
        (error-page {:status 500
                     :title "Something very bad has happened!"
                     :message "We've dispatched a team of highly trained gnomes to take care of the problem."})))))

(defn wrap-csrf [handler]
  (wrap-anti-forgery
    handler
    {:error-response
     (error-page
       {:status 403
        :title "Invalid anti-forgery token"})}))

(def java-time-localdate-handler
  (transit/write-handler
    (constantly "LocalDate")
    (fn [v] (-> v .toString))))

(def java-time-duration-handler
  (transit/write-handler
    (constantly "Duration")
    (fn [v] (-> v .toString))))

(def write-handlers
  {java.time.LocalDate java-time-localdate-handler
   java.time.Duration java-time-duration-handler})

(def m
  (muuntaja/create
    (update-in
      muuntaja/default-options
      [:formats "application/transit+json"]
      merge {:decoder-opts {}
             :encoder-opts {:handlers write-handlers}})))

(defn wrap-formats [handler]
  (let [wrapped (-> handler wrap-params (wrap-format m))]
    (fn [request]
      ;; disable wrap-formats for websockets
      ;; since they're not compatible with this middleware
      ((if (:websocket? request) handler wrapped) request))))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      wrap-webjars
      wrap-flash
      (wrap-session {:cookie-attrs {:http-only true}})
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:security :anti-forgery] false)
            (dissoc :session)))
      wrap-internal-error))
