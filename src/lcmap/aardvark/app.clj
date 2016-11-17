(ns lcmap.aardvark.app
  "Build app from middleware and handlers."
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [ring.middleware.accept :refer [wrap-accept]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.problem :refer [wrap-problem]]
            [lcmap.aardvark.landsat :as landsat]
            [lcmap.aardvark.middleware :refer [wrap-authenticate wrap-authorize]]
            [lcmap.aardvark.problem :as problem]))


(defn new-handler
  "Build a middleware wrapped handler for app. This approach makes
  dependencies (components) available to handling functions."
  [db msg]
  (log/debug "creating app handler")
  (context "/" req
    (-> (routes (landsat/resource db msg)
                (problem/resource db msg))
        (wrap-authorize)
        (wrap-authenticate)
        (wrap-accept)
        (wrap-keyword-params)
        (wrap-json-params)
        (wrap-json-response)
        (wrap-params)
        (wrap-problem (problem/transformer req db msg)))))
