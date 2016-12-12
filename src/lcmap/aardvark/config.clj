(ns lcmap.aardvark.config
  "Configuration!"
  (:require [uberconf.core :as uberconf]
            [clojure.tools.logging :as log]
            [mount.core :refer [defstate] :as mount]
            [schema.core :as schema]))

(def config-schema
  {:http     {:port schema/Num
              (schema/optional-key :join?) schema/Bool
              (schema/optional-key :daemon?) schema/Bool}
   :database {:contact-points [schema/Str]
              :default-keyspace schema/Str}
   :event    {:host schema/Str
              :port schema/Num
              :server-exchange schema/Str
              :server-queue schema/Str
              :worker-exchange schema/Str
              :worker-queue schema/Str}
   (schema/optional-key :server) schema/Bool
   (schema/optional-key :worker) schema/Bool
   schema/Keyword schema/Str})

(defn build [{:keys [edn cli env schema]
              :or {schema config-schema}
              :as args}]
  (log/debugf "using schema: '%s'" schema)
  (log/debugf "build config: '%s'" (uberconf/build-cfg args))
  (uberconf/init-cfg {:edn edn :cli cli :env env :schema schema}))

(defstate config
  :start (let [args ((mount/args) :config)]
           (log/debugf "starting config with: %s" args)
           (build args)))
