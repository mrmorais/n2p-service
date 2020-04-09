(ns n2p-service.components.rabbit-mq
  (:require [com.stuartsierra.component :as component]
            [langohr.core :as mq]
            [langohr.basic :as mq-basic]
            [langohr.channel :as mq-channel]
            [langohr.queue :as mq-queue]
            [langohr.consumers :as mq-consumers]
            [clojure.data.json :as json]
            [io.pedestal.log :as log]
            [n2p-service.protocols.mq-broker :as mq-broker])
  (:import (java.net ConnectException)))


(defrecord RabbitMQ [config]
  component/Lifecycle
  (start [this]
    (assoc
     this
     :mq-connection
     (try
       (mq/connect {:uri (or (get (System/getenv) "RABBIT_MQ_URI") (get-in config [:config :rabbit-mq-uri]))})
       (catch ConnectException ex
         (log/error ::log/error "Connection refused")
         (throw (ex-info "System shutdown" {:cause ex}))))))
  (stop [this]
    (mq/close (:mq-connection this))
    (dissoc this :mq-connection))

  mq-broker/MqBroker
  (publish
    [this queue_name content]
    (let [conn (:mq-connection this)
          channel (mq-channel/open conn)]
      (mq-queue/declare channel queue_name {:exclusive false :auto-delete false})
      (mq-basic/publish channel "" queue_name content {:content-type "application/json"})
      (mq-channel/close channel)))
  (consume
    [this queue_name handler]
    (let [conn (:mq-connection this)
          channel (mq-channel/open conn)
          consumer (mq-consumers/create-default channel {:handle-delivery-fn handler})]
      (mq-basic/consume channel queue_name consumer))))

(defn new-rabbit-mq [] (map->RabbitMQ {}))
