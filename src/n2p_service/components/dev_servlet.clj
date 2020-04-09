(ns n2p-service.components.dev-servlet
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as bootstrap]
            ))

(defrecord DevServlet [service]
  component/Lifecycle
  (start [this]
    (assoc this :instance (-> service
                              :runnable-service
                              (assoc ::bootstrap/join? false)
                              bootstrap/create-server
                              bootstrap/start)))
  (stop [this]
    (bootstrap/stop (:instance this))
    (dissoc this :instance))
  Object
  (toString [_] "<DevServlet>"))

(defn new-servlet [] (map->DevServlet {}))

