(ns lcmap.aardvark.core
  "Entrypoint for app."
  (:require [com.stuartsierra.component :as component]
            [lcmap.aardvark.config :as config]
            [lcmap.aardvark.system :as system]
            [clojure.tools.logging :as log])
  (:gen-class))

(defn handle-shutdown
  "Stop system during shutdown."
  [system]
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. #(component/stop system)))
  system)

(defn -main
  "Start the app."
  [& args]
  (try
    (log/debugf "Starting app with `lcmap-landsat.edn`")
    (-> args
        config/build
        system/system
        component/start
        handle-shutdown)
    (catch java.io.FileNotFoundException e
      (log/error "File not found, is `lcmap-landsat.edn` on the load path?"))))
