(ns lcmap.aardvark.shared
  (:require [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [lcmap.aardvark.config :as config]
            [lcmap.aardvark.server :as server]
            [lcmap.aardvark.worker :as worker]
            [lcmap.aardvark.util :as util]
            [mount.core :as mount]
            [org.httpkit.client :as http]))

(defmacro with-system
  "Start and stop the system, useful for integration tests.

  This will also execute a db-schema mount being; see the
  test/resources/schema.setup.cql and schema.teardown.cql
  for what will happen."

  ;; Dear Future Self,
  ;; At some point, it might make sense to use fixtures in order
  ;; to modify the internal state of backing services. Using mount
  ;; mount seems clever, complex, and potentially dangerous.

  [& body]
  `(let [cfg# (util/read-edn "lcmap.aardvark.edn")]
     (log/debugf "starting test system with config: %s" cfg#)
     (mount/start-without (mount/with-args {:config cfg#}))
     (try
       (do ~@body)
       (finally
         (log/debugf "stopping test system")
         (mount/stop)))))

(defn req
  "Convenience function for making HTTP requests."
  [method url & {:keys [headers query-params form-params body]
                 :as   opts}]
  (let [defaults {:headers {"Accept" "application/json"}}]
    @(http/request (merge {:url url :method method} defaults opts))))
