(ns running.routes.services.auth
  (:require [running.db.core :as db]
            [running.config :refer [env]]
            [running.routes.services.common :refer [handler]]
            [running.validation :as v]
            [running.middleware :refer [secret]]
            [buddy.hashers :as hashers]
            [clojure.tools.logging :as log]
            [java-time :as jt]
            [schema.core :as s]
            [ring.util.http-response :refer :all]
            [buddy.sign.jwt :as jwt]
            [cheshire.core :as json]))


(defn authenticate-local [username pass]
  (when-let [user (db/get-user-by-name {:name username})]
    (when (hashers/check pass (:pass user))
      (dissoc user :pass))))

(def User
  {:id s/Int
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
      (-> {:user user}
          (ok)
          (assoc :session (assoc session :identity user))))
    (do
      (log/info "login failed for " username remote-addr server-name)
      (unauthorized {:error "The username or password was incorrect."}))))

(handler logout []
         (assoc (ok {:result "ok"}) :session nil))

(handler get-token [username, pass {:keys [session]}]
         (if-let [user (authenticate-local username pass)]
           (let [user (-> user
                          (dissoc :pass))]
             (log/info "user " username " authenticated for token")
             ; todo token expiration and signing like
             ; https://github.com/JarrodCTaylor/authenticated-compojure-api/blob/master/src/leiningen/new/authenticated_compojure_api/src/authenticated_compojure_api/general_functions/user/create_token.clj
             (ok {:token (jwt/sign {:user-id (:user-id user)} secret)}))
           (do
             (log/info "Token request could not be validated")
             (unauthorized {:error "The username or password was incorrect"}))))

(handler find-users [username]
         (ok
           {:users
            (db/get-users-by-name (str "%" username "%"))}))

(handler create-user! [user]
         (if-let [errors (v/validate-create-user user)]
           (do
             (log/error "Error creating user: " errors)
             (bad-request {:error "invalid user"}))
           (db/create-user!
             (-> user
                 (dissoc :pass-confirm)
                 (update-in [:pass] hashers/derive)))))

(handler update-user! [{:keys [pass] :as user}]
         (if-let [errors (v/validate-update-user user)]
           (do
             (log/error "Error updating user: " errors)
             (bad-request {:error "Error updating user"}))
           (ok
             {:user
              (db/update-user! (cond-> user
                                       pass (update :pass hashers/derive)
                                       pass (assoc :update-password? true)))})))