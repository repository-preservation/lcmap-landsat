(ns curate
  "Provides data curator with tools to load data and check progress."
  (:require [lcmap.aardvark.config :as config]
            [lcmap.aardvark.event :as event]
            [lcmap.aardvark.server :as server]
            [lcmap.aardvark.source :as source]
            [lcmap.aardvark.tile :as tile]
            [lcmap.aardvark.tile-spec :as tile-spec]
            [lcmap.aardvark.util :as util]
            [lcmap.aardvark.worker :as worker]
            [mount.core :as mount]))

(defn save-tile-specs
  "Save all tile-specs in EDN at path."
  [path]
  (->> path util/read-edn (map tile-spec/save)))

(defn save-sources
  "Save all sources in EDN at path."
  ([path limit]
   (->> path util/read-edn (take limit) (map source/save)))
  ([path]
   (->> path util/read-edn (map source/save))))

(defn progress-report
  "Group sources together by most recent progress/state with count."
  [path]
  (->> (util/read-edn path)
       (map (comp :progress_name last source/search :id))
       (frequencies)))

(defn start
  "Start enough of the system to invoke ingest.

  Does not start the HTTP server or AMQP worker; see `dev/user.clj`
  if you want to start the full system with a REPL."
  []
  (->> (mount/with-args {:config (util/read-edn "lcmap.aardvark.edn")})
       (mount/start-without #'lcmap.aardvark.server/server
                            #'lcmap.aardvark.worker/worker
                            #'lcmap.aardvark.worker/worker-consumer
                            #'lcmap.aardvark.db/db-schema)))

(comment
  "Step 0: Configuration!
   Update `dev/resources/lcmap.aardvark.edn` to use desired Cassandra,
   Elasticsearch, and RabbitMQ instance. This is almost definitely *not*
   localhost if you are curating an operational system.
  "
  (-> config/config :database :cluster :contact-points)
  (-> config/config :event :host)
  (-> config/config :search :index-url)

  "Step 1: Start system."
  (start)

  "Step 2: Create tile-specs, otherwise no tiles will be saved."
  (save-tile-specs "tile-specs/L4.edn")
  (save-tile-specs "tile-specs/L5.edn")
  (save-tile-specs "tile-specs/L7.edn")
  (save-tile-specs "tile-specs/L8.edn")

  "Step 3: Enqueue one path-row from each mission. If the scene
  has already been processed, it will be processed again."
  (save-sources "sources/washington/LT04046026.edn" 1)
  (save-sources "sources/washington/LT05046026.edn" 1)
  (save-sources "sources/washington/LE07046026.edn" 1)
  (save-sources "sources/washington/LC8046026.edn"  1)

  "Step 5: See how things are going. This will get the most recent
   progress name for each source and group them together by count."
  (progress-report "sources/washington/LT04046026.edn")
  (progress-report "sources/washington/LT05046026.edn")
  (progress-report "sources/washington/LE07046026.edn")
  (progress-report "sources/washington/LC8046026.edn"))
