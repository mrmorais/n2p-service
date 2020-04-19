(ns n2p-service.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.interceptor.error :as error-int]
            [ring.util.response :as ring-resp]
            [clojure.data.json :as json]
            [n2p-service.controllers.record :as record-ctlr]
            [n2p-service.controllers.project :as project-ctlr]
            [n2p-service.controllers.content :as content-ctlr]
            [n2p-service.controllers.episode :as episode-ctlr]))

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
         (assoc-in [:response :headers "Content-Type"] "application/json")))
   })

(def common-interceptors [
                          json-responder
                          request-identifier
                          service-error
                          (body-params/body-params) http/html-body
                          ])

;; Tabular routes
(def routes #{["/" :get (conj common-interceptors `home-page)]
              ["/project" :post (conj common-interceptors `project-ctlr/create-project!)]
              ["/project/:project-id" :get (conj common-interceptors `project-ctlr/get-project)]
              ["/project/:project-id/content" :post (conj common-interceptors `content-ctlr/push-content!)]
              ["/project/:project-id/content" :get (conj common-interceptors `content-ctlr/get-all-content)]
              ["/project/:project-id/content/:content-id" :get (conj common-interceptors `content-ctlr/get-content)]
              ["/project/:project-id/content/:content-id" :put (conj common-interceptors `content-ctlr/update-content!)]
              ["/project/:project-id/episode" :get (conj common-interceptors `episode-ctlr/get-current-episode)]
              ["/project/:project-id/episode/content" :put (conj common-interceptors `episode-ctlr/put-content!)]
              ["/project/:project-id/episode/record" :post (conj common-interceptors `episode-ctlr/create-record!)]
              ["/project/:project-id/episode/record" :get (conj common-interceptors `episode-ctlr/get-all-records)]})
