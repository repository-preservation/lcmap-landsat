(ns curate
  "Provides data curator with tools to load data and check progress."
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [lcmap.aardvark.event :as event]
            [lcmap.aardvark.server :as server]
            [lcmap.aardvark.source :as source]
            [lcmap.aardvark.tile :as tile]
            [lcmap.aardvark.tile-spec :as tile-spec]
            [lcmap.aardvark.util :as util]
            [lcmap.aardvark.worker :as worker]
            [mount.core :as mount]))

(defn read-edn
  "Read EDN at path. Useful for loading tile-specs and sources."
  [path]
  (-> path io/file slurp edn/read-string))

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
  "Start enough of the system to work with Cassandra, Elasticsearch,
  and RabbitMQ without consuming messages or handling HTTP requests."
  []
  (->> (mount/with-args {:config (util/read-edn "lcmap.aardvark.edn")})
       (mount/start-without #'lcmap.aardvark.worker/worker-consumer
                            #_#'lcmap.aardvark.server/server
                            #'lcmap.aardvark.db/db-schema)))

(comment
  (start)
  (mount/stop))

(comment
  (save-tile-specs "tile-specs/L4.edn")
  (save-tile-specs "tile-specs/L5.edn")
  (save-tile-specs "tile-specs/L7.edn")
  (save-tile-specs "tile-specs/L8.edn"))

(comment
  (save-sources "sources/washington/LT04046026.edn" 1)
  (save-sources "sources/washington/LT05046026.edn" 1)
  (save-sources "sources/washington/LE07046026.edn" 1)
  (save-sources "sources/washington/LC8046026.edn"  1))

(comment
  (progress-report "sources/washington/LT04046026.edn")
  (progress-report "sources/washington/LT05046026.edn")
  (progress-report "sources/washington/LE07046026.edn")
  (progress-report "sources/washington/LC8046026.edn"))
