(ns running.routes.services.auth
  (:require [running.db.core :as db]
            [running.config :refer [env]]
            [running.routes.services.common :refer [handler]]
            [buddy.hashers :as hashers]
            [clojure.tools.logging :as log]
            [schema.core :as s]
            [ring.util.http-response :refer :all]))


(defn authenticate-local [username pass]
  (when-let [user (db/get-user-by-name {:name username})]
    (when (hashers/check pass (:pass user))
      (dissoc user :pass))))

(def User
  {:user-id s/Int
   :name (s/maybe s/Str)
   :email (s/maybe s/Str)
   :admin s/Bool
   :is-active s/Bool
   :last-login s/Any
   (s/optional-key :belongs-to) [(s/maybe s/Str)]
   (s/optional-key :member-of) [(s/maybe s/Str)]
   (s/optional-key :account-name) (s/maybe s/Str)
   (s/optional-key :client-ip) s/Str
   (s/optional-key :source-address) s/Str})

(def LoginResponse
  {(s/optional-key :user) User
   (s/optional-key :error) s/Str})

(def LogoutResponse
  {:result s/Str})

(defn local-login [username pass]
  (when-let [user (authenticate-local username pass)]
    (-> user
        (merge {:member-of []
                :account-name username}))))

(defn login [username, pass {:keys [remote-addr server-name session]}]
  (if-let [user (local-login username pass)]
    (let [user (-> user
                   (dissoc :pass)
                   (merge
                     {:client-ip remote-addr
                      :source-address server-name}))]
      (log/info "user:" username "successfully logged in from " remote-addr server-name)
      (log/info ":user is " user)
      (-> {:user user}
          (ok)
          (assoc :session (assoc session :identity user))))
    (do
      (log/info "login failed for " username remote-addr server-name)
      (unauthorized {:error "The username or password was incorrect."}))))

(handler logout []
         (assoc (ok {:result "ok"}) :session nil))