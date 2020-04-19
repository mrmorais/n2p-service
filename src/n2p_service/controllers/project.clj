(ns n2p-service.controllers.project
  (:require [schema.core :as schema]
            [n2p-service.protocols.database :as database]
            [ring.util.response :as ring-resp]))

(def project-table-definition
  [:n2p-project
   [:id :s]
   {:throughput {:read 1 :write 1}}])

(def new-project-schema
  {:title schema/Str
   :description schema/Str})

(defn ^:private project-request->project-item [project-req]
  (assoc project-req :id (.toString (java.util.UUID/randomUUID))))

(defn create-project! [{{:keys [dynamo-db]} :components
                        :as request}]
  (let [project-req (schema/validate new-project-schema (:json-params request))
        project (project-request->project-item project-req)]
    (database/put-item dynamo-db project-table-definition project)
    (ring-resp/created (str "/project/" (:id project)) project)))

(defn get-project [{{:keys [dynamo-db]} :components
                    {:keys [project-id]} :path-params
                    :as request}]
  (let [project (database/get-item dynamo-db project-table-definition {:id project-id})]
    (if (= project nil)
      (ring-resp/not-found {})
      (ring-resp/response project))))
