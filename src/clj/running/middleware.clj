(ns running.middleware
  (:require [running.env :refer [defaults]]
            [cognitect.transit :as transit]
            [clojure.tools.logging :as log]
            [running.layout :refer [error-page]]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [ring.middleware.webjars :refer [wrap-webjars]]
            [muuntaja.core :as muuntaja]
            [muuntaja.format.json :as json-format]
            [muuntaja.format.transit :as transit-format]
            [muuntaja.middleware :refer [wrap-format wrap-params]]
            [running.config :refer [env]]
            [ring.middleware.flash :refer [wrap-flash]]
            [immutant.web.middleware :refer [wrap-session]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.auth.accessrules :refer [restrict wrap-access-rules]]
            [buddy.auth :refer [authenticated?]]
            [buddy.auth.backends.session :refer [session-backend]]
            [buddy.auth.backends :as backends]))


(def rules
  [{:uri "restricted"
   :handler authenticated?}])

(def secret (-> env :application-secret))
(def token-backend (backends/jws {:secret secret
                                  ;:authfn (fn [token-data]
                                  ;          token-data)
                                  }))

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

(extend-protocol cheshire.generate/JSONable
  java.time.Duration
  (to-json [d gen]
    (cheshire.generate/write-string gen (str d))))

(extend-protocol cheshire.generate/JSONable
  java.time.LocalDate
  (to-json [ld gen]
    (cheshire.generate/write-string gen (str ld))))

(extend-protocol cheshire.generate/JSONable
  java.time.LocalDateTime
  (to-json [ldt gen]
    (cheshire.generate/write-string gen (str ldt))))

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

(defn on-error [request response]
  (error-page
    {:status 403
     :body "Not Authorized"}))

(defn wrap-rule [handler rule]
  (-> handler
      (wrap-access-rules {:rules [{:pattern #".*"
                                   :handler rule}]
                          :on-error on-error})))

(defn wrap-auth [handler]
  (let [auth-backend token-backend
        session-backend (session-backend)]
    (-> handler
        ; I had trouble applying multiple backends functionally but
        ; applying them one by one works fine
        (wrap-authorization auth-backend)
        (wrap-authorization session-backend)
        (wrap-authentication auth-backend)
        (wrap-authentication session-backend))))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      wrap-auth
      wrap-webjars
      wrap-flash
      (wrap-session {:cookie-attrs {:http-only true}})
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:security :anti-forgery] false)
            (dissoc :session)))
      wrap-internal-error
      ))
