(ns lcmap.aardvark.handler
  "Build app from middleware and handlers."
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [ring.middleware.accept :refer [wrap-accept]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.problem :refer [wrap-problem]]
            [lcmap.aardvark.landsat :as landsat]
            [lcmap.aardvark.middleware :refer [wrap-content-type wrap-authenticate wrap-authorize]]
            [lcmap.aardvark.problem :as problem]))


(defn landsat
  "Build a middleware wrapped handler for app. This approach makes
  dependencies (components) available to handling functions."
  [db]
  (log/debug "creating app handler")
  (context "/" req
    (-> (routes (landsat/resource db)
                (problem/resource db))
        (wrap-authorize)
        (wrap-authenticate)
        (wrap-accept)
        (wrap-content-type)
        (wrap-keyword-params)
        (wrap-json-params)
        (wrap-json-response)
        (wrap-params)
        (wrap-problem (problem/transformer req db)))))
