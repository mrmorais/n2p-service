(ns n2p-service.controllers.episode
  (:require [schema.core :as schema]
            [n2p-service.protocols.database :as database]
            [n2p-service.controllers.content :as content-ctlr]
            [n2p-service.controllers.record :as record-ctlr]
            [ring.util.response :as ring-resp]))

(def episode-table-definition
  [:n2p-episode
   [:project-id :s]
   {:throughput {:read 1 :write 1}
    :range-keydef [:id :s]}])

(defn ^:private create-new-episode [project-id]
  {:status "Created"
   :project-id project-id
   :id (.toString (java.util.UUID/randomUUID))
   :title "Unamed episode"
   :content []})

(defn ^:private current-episode-lookup [dynamo-db project-id]
  (database/scan dynamo-db episode-table-definition {:project-id [:eq project-id] :status [:eq "Created"]}))

(defn ^:private ensure-current-episode [dynamo-db project-id]
  (let [current-ep (current-episode-lookup dynamo-db project-id)
        new-ep (create-new-episode project-id)]
    (if (= 0 (count current-ep))
      (do
        (database/put-item dynamo-db episode-table-definition new-ep)
        new-ep)
      (first current-ep))))

(defn get-current-episode [{{:keys [dynamo-db]} :components
                            {:keys [project-id]} :path-params
                            :as request}]
  (let [current-episode (ensure-current-episode dynamo-db project-id)
        contents (content-ctlr/get-content-subset dynamo-db (:content current-episode))
        episode (assoc current-episode :content contents)]
    (ring-resp/response episode)))

(def put-content-schema
  {:content [schema/Str]})

(defn ^:private conj-content [current-content new-content]
  (into [] (distinct (concat current-content new-content))))

(defn put-content! [{{:keys [dynamo-db]} :components
                    {:keys [project-id]} :path-params
                     :as request}]
  (let [content-req (schema/validate put-content-schema (:json-params request))
        content (:content content-req)
        current-episode (ensure-current-episode dynamo-db project-id)
        updated-episode (database/update-item dynamo-db episode-table-definition {:project-id project-id :id (:id current-episode)}
                                              "SET #content = :new_content"
                                              {"#content" "content"}
                                              {":new_content" (conj-content (:content current-episode) content)})]
    (ring-resp/response updated-episode)))

(def create-record-schema
  {:template schema/Str
   :template-config schema/Any})

(defn create-record! [{{:keys [dynamo-db rabbit-mq]} :components
                       {:keys [project-id]} :path-params
                       :as request}]
  (let [current-episode (ensure-current-episode dynamo-db project-id)
        content (content-ctlr/get-content-subset dynamo-db (:content current-episode))
        create-record-req (schema/validate create-record-schema (:json-params request))
        created-record (record-ctlr/create-record!
                        dynamo-db
                        rabbit-mq
                        current-episode
                        content
                        (:template create-record-req)
                        (:template-config create-record-req))]
    (ring-resp/response created-record)))

(defn get-all-records [{{:keys [dynamo-db]} :components
                        {:keys [project-id]} :path-params
                        :as request}]
  (let [episode (ensure-current-episode dynamo-db project-id)
        records (record-ctlr/get-records dynamo-db (:id episode))]
    (ring-resp/response records)))
