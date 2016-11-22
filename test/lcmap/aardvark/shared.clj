(ns lcmap.aardvark.shared
  (:require [clojure.tools.logging :as log]
            [lcmap.aardvark.config :as config]
            [mount.core :as mount]
            [clojure.java.io :as io]))

(defn test-config
  "Start the app."
  ([]
   (test-system "lcmap-landsat-test.edn"))
  ([cfg-path]
   (try
     (log/debugf "Starting app with `%s`" cfg-path)
     (let [cfg (config/build {:edn cfg-path})]
       (mount/start-with {#'lcmap.aardvark.state/config cfg}))
     (catch java.io.FileNotFoundException e
       (log/error "File not found, is `lcmap-landsat-test.edn` on the load path?")))))

(defmacro with-system
  "Start and stop the system, useful for integration tests."
  [[binding] & body]
  `(let [cfg (config/build {:edn (io/resource "lcmap-landsat-test.edn")})
         ~binding (mount/start-with {#'lcmap.aardvark.state/config cfg})]
     (try
       (do ~@body)
       (finally
         (mount/stop ~binding)))))
