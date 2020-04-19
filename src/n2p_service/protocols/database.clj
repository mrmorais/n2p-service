(ns n2p-service.protocols.database)

(defprotocol Database
  (put-item [this table-definition item])
  (get-item [this table-definition pr-keys])
  (update-item [this table-definition prim-keys update-expr expr-attr-names expr-attr-vals])
  (query [this table-definition prim-key-conds])
  (scan [this table-definition attr-conds]))
