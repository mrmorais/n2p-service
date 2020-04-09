(ns n2p-service.protocols.mq-broker)

(defprotocol MqBroker
  (publish
    [this queue_name content]))
