(ns n2p-service.controllers.user
  (:require [n2p-service.db.user :as user-db]
            [ring.util.response :as ring-resp]
            [schema.core :as schema]))

(def user-schema
  {:name schema/Str
   :email schema/Str})

(defn create-user! [request]
  (let [data (schema/validate user-schema (:json-params request))
        user (assoc data :id (.toString (java.util.UUID/randomUUID)))]
    (user-db/put-user! user)
    (ring-resp/created (str "/user/" (:id user)) user)))

(defn get-user [request]
  (let [user-id (get-in request [:path-params :id])
        user (user-db/get-user user-id)]
    (if (nil? user) (ring-resp/not-found {:error "User not found"}) (ring-resp/response user))))
