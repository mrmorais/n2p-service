(ns n2p-service.components
  (:require [com.stuartsierra.component :as component]
            [n2p-service.components.dummy-config :as config]
            [n2p-service.components.service :as service]
            [n2p-service.service :as n2p-service.service]
            [n2p-service.components.dev-servlet :as servlet]
            [n2p-service.components.system-utils :as system-utils]
            [n2p-service.components.routes :as routes]
            [n2p-service.components.rabbit-mq :as rabbit-mq]
            [schema.core :as s]))

(def base-config-map {:environment :prod
                      :dev-port 8080
                      :rabbit-mq-uri "amqp://guest:guest@192.168.99.100"})

(def local-config-map {:environment :dev
                       :dev-port 8080
                       :rabbit-mq-uri "amqp://guest:guest@192.168.99.100"})

;; all the components that will be available in the pedestal http request map
(def web-app-deps
  [:config :routes])

(defn base []
  (component/system-map
   :config (config/new-config base-config-map)
   :routes (routes/new-routes n2p-service.service/routes)
  ; :rabbit-mq (component/using (rabbit-mq/new-rabbit-mq) [:config])
   :service (component/using (service/new-service) web-app-deps)
   :servlet (component/using (servlet/new-servlet) [:service])))

(def systems-map
  {:base-system base})

(defn create-and-start-system!
  ([] (create-and-start-system! :base-system))
  ([env]
   (system-utils/bootstrap! systems-map env)))

(defn ensure-system-up! [env]
  (or (deref system-utils/system)
      (create-and-start-system! env)))

(defn stop-system! [] (system-utils/stop-components!))
