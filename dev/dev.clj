(ns dev
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [feline.config]
            [feline.system]
            [uberconf.core :as uberconf]))

(def system "A Var containing the application" nil)

(defn init
  "Prepare a system, in the Var #'system"
  []
  (alter-var-root #'system (fn [_] (feline.system/system (feline.config/build {})))))

(defn start
  "Start components of system and update Var #'system"
  []
  (if #'system
    (alter-var-root #'system component/start)))

(defn stop
  "Stop components of system and update Var #'system"
  []
  (if #'system
    (alter-var-root #'system component/stop)))

(defn void
  "Return system to uninitialized state"
  []
  (if #'system
    (alter-var-root #'system (fn [_] nil))))

(defn go
  "Prepare and start a system"
  []
  (init)
  (start)
  :ready)

(defn reset
  "Stop, refresh, and start a system."
  []
  (stop)
  (void)
  (refresh :after `go))
