(ns lcmap.aardvark.state
  "Stateful system components are defined here (for convienience.)
To use, simply (require :refer [item]) from other namespaces.  Stateful items
must be started prior to using with mount.core/start.  Individual
states may be started/stopped as well with (mount/start #'the.namespace/item).
At dev/test time, namespaces can be replaced by using
(mount/start-with {#'the.namespace/item replacement_item}) instead of plain old
mount/start.  See https://github.com/tolitius/mount/blob/master/README.md"
  (:require [clojure.tools.logging :as log]
            [mount.core :refer [defstate] :as mount]
            [gdal.core :as gdal]))

(defstate graph
  :start 33)

(defstate tile-search
  :start 33
  :stop  34)

(defstate hook
  :start (do
           (log/debugf "registering shutdown handler")
           (.addShutdownHook (Runtime/getRuntime)
                             (Thread. #(mount/stop) "shutdown-handler"))))
