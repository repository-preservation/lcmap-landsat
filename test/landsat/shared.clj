(ns landsat.shared
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [landsat.config]
            [landsat.system]))

(defn test-system
  "Start the app."
  ([]
   (test-system "test/lcmap-landsat.ini"))
  ([cfg-path]
   (try
     (log/debugf "Starting app with `%s`" cfg-path)
     (log/debugf "Configuration: %s" (landsat.config/build {:ini cfg-path}))
     (-> {:ini cfg-path}
         landsat.config/build
         landsat.system/system)
     (catch java.io.FileNotFoundException e
       (log/error "File not found, is `test/lcmap-landsat.ini` on the load path?")))))

(defmacro with-system
  "Start and stop the system, useful for integration tests."
  [[binding] & body]
  `(let [~binding (component/start (test-system "test/lcmap-landsat.ini"))]
     (try
       (do ~@body)
       (finally
         (component/stop ~binding)))))
