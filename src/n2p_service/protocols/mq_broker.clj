(ns n2p-service.protocols.mq-broker)

(defprotocol MqBroker
  (publish [this queue_name content])
  (consume [this queue_name handler]))
