(ns curate
  "Provides data curator with tools to load data and check progress."
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [lcmap.aardvark.event :as event]
            [lcmap.aardvark.source :as source]
            [lcmap.aardvark.tile :as tile]
            [lcmap.aardvark.tile-spec :as tile-spec]
            [mount.core :as mount]))

(defn read-edn
  "Read EDN at path. Useful for loading tile-specs and sources."
  [path]
  (-> path io/file slurp edn/read-string))

(defn save-tile-specs
  "Save all tile-specs in EDN at path."
  [path]
  (->> path read-edn (map tile-spec/save)))

(defn save-sources
  "Save all sources in EDN at path."
  ([path limit]
   (->> path read-edn (take limit) (map source/save)))
  ([path]
   (->> path read-edn (map source/save))))

(defn progress-report
  "Group sources together by most recent progress/state with count."
  [path]
  (->> (read-edn path)
       (map (comp :progress_name last source/search :id))
       (frequencies)))

(defn start
  "Start enough of the system to work with Cassandra, Elasticsearch,
  and RabbitMQ without consuming messages or handling HTTP requests."
  []
  (->> (mount/with-args {:config (read-edn "lcmap-landsat.edn")})
       (mount/start-without #'lcmap.aardvark.worker/worker-consumer
                            #'lcmap.aardvark.server/server
                            #'lcmap.aardvark.db/db-schema)))

(comment
  (start))

(comment
  (save-tile-specs "data/tile-specs/L4.edn")
  (save-tile-specs "data/tile-specs/L5.edn")
  (save-tile-specs "data/tile-specs/L7.edn")
  (save-tile-specs "data/tile-specs/L8.edn"))

(comment
  (save-sources "data/sources/washington/LT04046026.edn" 1)
  (save-sources "data/sources/washington/LT05046026.edn" 1)
  (save-sources "data/sources/washington/LE07046026.edn" 1)
  (save-sources "data/sources/washington/LC8046026.edn"  1))

(comment
  (progress-report "data/sources/washington/LT04046026.edn")
  (progress-report "data/sources/washington/LT05046026.edn")
  (progress-report "data/sources/washington/LE07046026.edn")
  (progress-report "data/sources/washington/LC8046026.edn"))
