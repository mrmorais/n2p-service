(ns n2p-service.db.user
  (:require [n2p-service.db :as db]
            [taoensso.faraday :as faraday]))

(def user-table-definition
  [:n2p-users
   [:id :s]
   {:throughput {:read 1 :write 1}}
   ])

(concat [db/client-ops] user-table-definition)

(defn create-user-table!
  []
  (or (some #(= :n2p-users %) (faraday/list-tables db/client-ops))
      (apply faraday/create-table (concat [db/client-ops] user-table-definition))))

(defn put-user! [user]
  (create-user-table!)
  (faraday/put-item db/client-ops :n2p-users user))

(defn get-user [id]
  (create-user-table!)
  (faraday/get-item db/client-ops :n2p-users {:id id}))
