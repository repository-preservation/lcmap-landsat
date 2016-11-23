(ns lcmap.aardvark.handler
  "Build app from middleware and handlers."
  (:require [compojure.core :refer :all]
            [clojure.tools.logging :as log]
            [ring.middleware.accept :refer [wrap-accept]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.problem :refer [wrap-problem]]
            [lcmap.aardvark.landsat :as landsat]
            [lcmap.aardvark.middleware :refer [wrap-authenticate
                                               wrap-authorize]]
            [lcmap.aardvark.problem :as problem]))


(defn landsat
  "Build a middleware wrapped handler for app. This approach makes
  dependencies (components) available to handling functions."
  [request]
  (context "/" request
    (-> (routes (landsat/resource request)
                (problem/resource request))
        (wrap-accept)
        (wrap-authorize)
        (wrap-authenticate)
        (wrap-keyword-params)
        (wrap-params)
        (wrap-problem #(problem/transformer request)))))
