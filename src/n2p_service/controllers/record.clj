(ns n2p-service.controllers.record
  (:require [n2p-service.db :as db]
            [n2p-service.protocols.mq-broker :as mq-broker]
            [ring.util.response :as ring-resp]
            [schema.core :as schema]
            [langohr.core :as mq]
            [langohr.basic :as mq-basic]
            [langohr.channel :as mq-channel]
            [langohr.queue :as mq-queue]
            [clojure.data.json :as json]))

(def mq-url (get (System/getenv) "AMQP_URL" "amqp://guest:guest@192.168.99.100"))

(defonce mq-connection (atom nil))

(defn get-mq-conn []
  (or @mq-connection
      (reset! mq-connection (mq/connect {:uri mq-url}))))

(def record-table-definition
  [:n2p-records
   [:id :s]
   {:throughput {:read 1 :write 1}}])

(def record-req-schema
  {:title schema/Str
   :template schema/Str
   :config schema/Any
   :data {
          :news [{
                  :headline schema/Str
                  :description schema/Str
                  }]
          }
   })

(defn ^:private record-request->record-item [record-req]
  (-> record-req
      (assoc :id (.toString (java.util.UUID/randomUUID)))
      (assoc :status "Created")))

(defn publish-record-create! [record]
  (let [conn (get-mq-conn)
        channel (mq-channel/open conn)
        qname "n2p.service.records.create"]
    (mq-queue/declare channel qname {:exclusive false :auto-delete false})
    (mq-basic/publish channel "" qname (json/write-str record) {:content-type "application/json"})
    (mq-channel/close channel)))

;; (publish-record-create! {:oi "oi"})

(defn create-record! [{{:keys [rabbit-mq]} :components
                       :as request}]
  (let [record-req (schema/validate record-req-schema (:json-params request))
        record (record-request->record-item record-req)]
    (db/put-item! record-table-definition record)
    (mq-broker/publish rabbit-mq "n2p.service.records.create" (json/write-str record))
    (publish-record-create! record)
    (ring-resp/created (str "/record/" (:id record)) record)))
