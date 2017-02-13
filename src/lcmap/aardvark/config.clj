(ns lcmap.aardvark.config
  "Configuration schema and state.

  This namespace does not address the subtle complexity posed by
  retrieving configuration from various sources (e.g. stdin, files,
  environment variables). It strives for simplicity, expecting only
  a `:config` mount argument, a deeply nested map.

  See `src/lcmap/core.clj` to see how environment variables are
  used in an operational mode. See `dev/user.clj` to see how an
  EDN file is used in a development mode.

  Schema is used to coerce string values into expected types and
  ensure the overall config structure and presence of values.

  To retrieve configuration values, require this namespace and
  use `(get-in)` to retrieve expected values. Care is taken to
  provide data in the shape expected by libraries, for example,
  the [:database :cluster] value is a map that can be used with
  `alia` to connect to a Cassandra cluster.
  "
  (:require [uberconf.core :as uberconf]
            [clojure.tools.logging :as log]
            [clojure.walk :as walk]
            [mount.core :refer [defstate] :as mount]
            [schema.core :as schema]))

;; Used by states defined in lcmap.server
(def http {(schema/optional-key :port) schema/Num
           (schema/optional-key :join?) schema/Bool
           (schema/optional-key :daemon?) schema/Bool
           schema/Keyword schema/Str})

;; Used by states defined in lcmap.event
(def event {:host schema/Str
            :port schema/Num
            schema/Keyword schema/Str})

;; Used by states defined in lcmap.db
(def database {:cluster {:contact-points [schema/Str]
                         schema/Keyword schema/Any}
               :default-keyspace schema/Str
               schema/Keyword schema/Any})

;; These specify the names of exchanges and queues from which
;; the server publishes and consumes messages.
(def server {:exchange schema/Str
             :queue schema/Str})

;; These specify the names of exchanges and queues from which
;; the server publishes and consumes messages.
(def worker {:exchange schema/Str
             :queue schema/Str})

;; URLs to elasticsearch resources and related config.
(def search {:index-url schema/Str
             :refresh-url schema/Str
             :bulk-api-url schema/Str
             :search-api-url schema/Str
             :max-result-size schema/Num})

(def root-cfg
  {:database database
   :event event
   :search search
   (schema/optional-key :http) http
   (schema/optional-key :server) server
   (schema/optional-key :worker) worker
   schema/Keyword schema/Str})

(defn init
  "Coerce config and check schema."
  [cfg]
  (->> cfg
       (uberconf.core/coerce-cfg root-cfg)
       (uberconf.core/check-cfg root-cfg)))

(defstate config
  :start (let [cfg ((mount/args) :config)]
           (log/debugf "starting config: %s" cfg)
           (init cfg)))
