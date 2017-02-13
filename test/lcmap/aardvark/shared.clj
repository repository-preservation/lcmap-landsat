(ns lcmap.aardvark.shared
  (:require [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [lcmap.aardvark.config :as config]
            [lcmap.aardvark.util :as util]
            [mount.core :as mount]
            [org.httpkit.client :as http]))

(defmacro with-system
  "Start and stop the system, useful for integration tests."
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
