(ns n2p-service.controllers.content
  (:require [schema.core :as schema]
            [n2p-service.protocols.database :as database]
            [ring.util.response :as ring-resp]))

(def content-table-definition
  [:n2p-content2
   [:project-id :s]
   {:throughput {:read 1 :write 1}
    :range-keydef [:id :s]}])

(def new-content-schema
  {:content
   [{:title schema/Str
     :body schema/Str}]})

(def upt-content-schema
  {:title schema/Str
   :body schema/Str})

(defn ^:private content-request->content-item [content-req project-id]
  (-> content-req
      (assoc :id (.toString (java.util.UUID/randomUUID)))
      (assoc :project-id project-id)
      (assoc :last-update (.toString (java.time.LocalDateTime/now)))))

(defn push-content! [{{:keys [dynamo-db]} :components
                      {:keys [project-id]} :path-params
                      :as request}]
  (let [new-content-req (schema/validate new-content-schema (:json-params request))
        content (:content new-content-req)
        content-items (map (fn [cont-req]
                             (let [cont-item (content-request->content-item cont-req project-id)]
                               (database/put-item dynamo-db content-table-definition cont-item)
                               cont-item)) content)]
    (ring-resp/response content-items)))

(defn get-all-content [{{:keys [dynamo-db]} :components
                        {:keys [project-id]} :path-params
                        :as request}]
  (let [all-content (database/query dynamo-db content-table-definition {:project-id [:eq project-id]})]
    (ring-resp/response all-content)))

(defn get-content [{{:keys [dynamo-db]} :components
                    {:keys [project-id content-id]} :path-params
                    :as request}]
  (let [content (database/get-item dynamo-db content-table-definition {:project-id project-id :id content-id})]
    (ring-resp/response content)))

(defn get-content-subset [dynamo-db ids]
  (database/scan dynamo-db content-table-definition {:id [:in ids]}))

(defn update-content! [{{:keys [dynamo-db]} :components
                       {:keys [project-id content-id]} :path-params
                       :as request}]
  (let [upt-content-request (schema/validate upt-content-schema (:json-params request))
        new-content (database/update-item dynamo-db content-table-definition {:project-id project-id :id content-id}
                                          "SET #title = :title, #body = :body, #last_update = :last_update"
                                          {"#title" "title" "#body" "body" "#last_update" "last-update"}
                                          {":title" (:title upt-content-request)
                                           ":body" (:body upt-content-request)
                                           ":last_update" (.toString (java.time.LocalDateTime/now))})]
    (ring-resp/response new-content)))
