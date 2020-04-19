(ns n2p-service.controllers.record
  (:require [n2p-service.protocols.mq-broker :as mq-broker]
            [n2p-service.protocols.database :as database]
            [ring.util.response :as ring-resp]
            [schema.core :as schema]
            [clojure.data.json :as json]))

(def record-table-definition
  [:n2p-records
   [:id :s]
   {:throughput {:read 1 :write 1}}])

(defn ^:private mount-record [episode content template template-config]
  (-> {}
      (assoc :id (.toString (java.util.UUID/randomUUID)))
      (assoc :status "Created")
      (assoc :episode-id (:id episode))
      (assoc :title (:title episode))
      (assoc :template template)
      (assoc :config template-config)
      (assoc :data {:news (mapv (fn [item] {:headline (:title item) :description (:body item)}) content)})))

(defn create-record! [dynamo-db rabbit-mq episode content template template-config]
  (let [record (mount-record episode content template template-config)]
    (database/put-item dynamo-db record-table-definition record)
    (mq-broker/publish rabbit-mq "n2p.service.records.create" (json/write-str record))
    record))

(defn get-records [dynamo-db episode-id]
  (let [records (database/scan dynamo-db record-table-definition {:episode-id [:eq episode-id]})]
    records))
