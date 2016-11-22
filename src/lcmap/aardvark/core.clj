(ns lcmap.aardvark.core
  "Entrypoint for app."
  (:require [mount.core :as mount]
            [clojure.tools.logging :as log])
  (:gen-class))

(defn handle-shutdown
  "Stop system during shutdown."
  [system]
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. #(mount/stop)))
  system)

(defn -main
  "Start the app."
  [& args]
  (try
    (log/debugf "Starting app with `lcmap-landsat.edn`")
    (-> args
        mount/start
        handle-shutdown)
    (catch java.io.FileNotFoundException e
      (log/error "File not found, is `lcmap-landsat.edn` on the load path?"))))
