(ns lcmap.aardvark.shared
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [lcmap.aardvark.config :as config]
            [lcmap.aardvark.system :as system]
            [clojure.java.io :as io]))

(defn test-system
  "Start the app."
  ([]
   (test-system "lcmap-landsat-test.edn"))
  ([cfg-path]
   (try
     (log/debugf "Starting app with `%s`" cfg-path)
     (log/debugf "Configuration: %s" (config/build {:edn cfg-path}))
     (-> {:edn cfg-path}
         config/build
         system/system)
     (catch java.io.FileNotFoundException e
       (log/error "File not found, is `lcmap-landsat-test.edn` on the load path?")))))

(defmacro with-system
  "Start and stop the system, useful for integration tests."
  [[binding] & body]
  `(let [~binding (component/start (test-system
                                     (io/resource "lcmap-landsat-test.edn")))]
     (try
       (do ~@body)
       (finally
         (component/stop ~binding)))))
