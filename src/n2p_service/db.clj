(ns n2p-service.db
  (:require [taoensso.faraday :as faraday]))

(def client-ops
  {:access-key "fake-access-key"
   :secret-key "fake-secret-key"
   :endpoint "http://localhost:8000"})

(defn create-table!
  [definition]
  (or (some #(= (first definition) %) (faraday/list-tables client-ops))
      (apply (partial faraday/create-table client-ops) definition)))

(defn put-item! [table-definition item]
  (create-table! table-definition)
  (faraday/put-item client-ops (first table-definition) item))

(defn get-item [table-definition id]
  (create-table! table-definition)
  (faraday/get-item client-ops (first table-definition) {:id id}))
