(ns n2p-service.components.dynamo-db
  (:require [taoensso.faraday :as far]
            [com.stuartsierra.component :as component]
            [io.pedestal.log :as log]
            [n2p-service.protocols.database :as database]))

(defn ^:private create-table
  [definition client-ops]
  (or (some #(= (first definition) %) (far/list-tables client-ops))
      (apply (partial far/create-table client-ops) definition)))

(defrecord DynamoDB [config]
  component/Lifecycle
  (start [this]
    (assoc
     this
     :client-ops
     {
      :access-key (or (get (System/getenv) "AWS_ACCESS_KEY")
                      (get-in config [:config :aws-access-key]))
      :secret-key (or (get (System/getenv) "AWS_SECRET_KEY")
                      (get-in config [:config :aws-secret-key]))
      :endpoint (or (get (System/getenv) "AWS_ENDPOINT")
                    (get-in config [:config :aws-endpoint]))
      }))
  (stop [this]
    (dissoc
     this
     :client-ops))

  database/Database
  (put-item [this table-definition item]
    (create-table table-definition (:client-ops this))
    (far/put-item (:client-ops this) (first table-definition) item))
  (get-item [this table-definition pr-keys]
    (create-table table-definition (:client-ops this))
    (far/get-item (:client-ops this) (first table-definition) pr-keys))
  (update-item [this table-definition prim-keys update-expr expr-attr-names expr-attr-vals]
    (create-table table-definition (:client-ops this))
    (far/update-item
     (:client-ops this)
     (first table-definition)
     prim-keys
     {:update-expr update-expr :expr-attr-names expr-attr-names :expr-attr-vals expr-attr-vals :return :updated-new}))
  (query [this table-definition prim-key-conds]
    (create-table table-definition (:client-ops this))
    (far/query (:client-ops this) (first table-definition) prim-key-conds))
  (scan [this table-definition attr-conds]
    (create-table table-definition (:client-ops this))
    (far/scan (:client-ops this) (first table-definition) {:attr-conds attr-conds})))

(defn new-dynamo-db [] (map->DynamoDB {}))
