(ns lcmap.aardvark.server
  "Aardvark HTTP server related functions and state.

  Very few functions are defined in this namespace; it brings all
  HTTP related functions together. Add new functions to one of the
  following namespaces:

  - lcmap.aardvark.landsat for resource definitions and routes.
  - lcmap.aardvark.middleware for request/response filter functions.
  - lcmap.aardvark.encoder for JSON encoders.
  "
  (:require [clojure.tools.logging :as log]
            [lcmap.aardvark.config :refer [config]]
            [lcmap.aardvark.encoder :as encoder]
            [lcmap.aardvark.landsat :as landsat]
            [lcmap.aardvark.middleware :refer [wrap-authenticate
                                               wrap-authorize
                                               wrap-exception
                                               wrap-request-debug]]
            [mount.core :refer [defstate] :as mount]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.accept :refer [wrap-accept]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]))

(defn make-handler
  "Build a middleware wrapped handler for app."
  []
  (-> (landsat/resource)
      (wrap-resource "public")
      (wrap-accept)
      (wrap-authorize)
      (wrap-authenticate)
      (wrap-keyword-params)
      (wrap-params)
      (wrap-request-debug)
      (wrap-exception)))

(defstate server
  :start (let [args (get-in config [:http])
               handler (make-handler)]
           (log/info "start server")
           (run-jetty handler args))
  :stop  (do
           (log/info "stop server")
           (.stop server)))
