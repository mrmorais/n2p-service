(ns n2p-service.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.interceptor.error :as error-int]
            [ring.util.response :as ring-resp]
            [clojure.data.json :as json]
            [n2p-service.controllers.user :as user-ctlr]
            [n2p-service.controllers.record :as record-ctlr]))

(defn home-page
  [request]
  (ring-resp/response {:version "0.0.1"}))

; Handle exception error
(def service-error
  (error-int/error-dispatch [context exception]
                            [{:exception-type :clojure.lang.ExceptionInfo}]
                            (assoc context :response {:status 400 :body {:error (.getMessage exception)} })))

; Append a request ID to the request
(def request-identifier
  {:name ::request-identifier
   :enter
   (fn [context]
     (let [req-id (.toString (java.util.UUID/randomUUID))]
       (assoc context :req-id req-id)))
   :leave
   (fn [context]
     (assoc-in context [:response :body]
               {:request-id (:req-id context)
                :data (get-in context [:response :body])}))
   })

; Interceptor that converts any output to JSON data
(def json-responder
  {:name ::json-responder
   :leave
   (fn [context]
     (-> context
         (assoc-in [:response :body] (json/write-str (get-in context [:response :body])))
         (assoc-in [:response :headers "Content-Type"] "text/json")))
   })

(def common-interceptors [
                          json-responder
                          request-identifier
                          service-error
                          (body-params/body-params) http/html-body
                          ])

;; Tabular routes
(def routes #{["/" :get (conj common-interceptors `home-page)]
              ;["/about" :get (conj common-interceptors `about-page)]
              ;["/user" :post (conj common-interceptors `user-ctlr/create-user!)]
              ;["/user/:id" :get (conj common-interceptors `user-ctlr/get-user)]
              ;["/record" :post (conj common-interceptors `record-ctlr/create-record!)]
              })
