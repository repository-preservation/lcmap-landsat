(ns lcmap.aardvark.shared
  (:require [clojure.tools.logging :as log]
            [lcmap.aardvark.config :as config]
            [mount.core :as mount]
            [clojure.java.io :as io]))

(defmacro with-system
  "Start and stop the system, useful for integration tests."
  [[binding] & body]
  `(let [cfg (config/build {:edn (io/resource "lcmap-landsat-test.edn")})
         ~binding (mount/start-with {#'lcmap.aardvark.state/config cfg})]
     (log/debug "Starting test system with config:" cfg)
     (try
       (do ~@body)
       (finally
         (mount/stop ~binding)))))
