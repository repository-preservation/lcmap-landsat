(ns curate
  "Provides data curators with data management tools.

  WARNING: PLEASE READ THIS ENTIRELY BEFORE USING THESE FUNCTIONS.

  These function can be used with local and operational environments;
  but exercise caution before using this with a production system.

  Make sure you have up-to-date code and submodules. If you don't know
  exactly how to do this, talk with someone that does! Loading stale
  data in a production system could cause subtle problems.

  This namespace contains a `start` function that will limit the mount
  beings to those needed to load data. It does not start a server or
  worker, and it does not execute DB schema setup or teardown logic.

  However, you are *strongly* advised to use credentials with minimal
  permissions required to curate data (selecting and inserting data)
  so that you you don't have to worry about performing schema changes.

  You will need to create and update a copy of `dev/local.lcmap.landsat.edn`
  named `curator.edn` and place it on the resource load path. This file
  is listed in .gitignore so you don't have to worry about sensitive
  data being added to the repository."
  (:require [clojure.java.io :as io]
            [lcmap.aardvark.config :as config]
            [lcmap.aardvark.db :as db]
            [lcmap.aardvark.event :as event]
            [lcmap.aardvark.server :as server]
            [lcmap.aardvark.source :as source]
            [lcmap.aardvark.tile :as tile]
            [lcmap.aardvark.tile-spec :as tile-spec]
            [lcmap.aardvark.util :as util]
            [lcmap.aardvark.worker :as worker]
            [mount.core :as mount]))


;; STEP 1. Start the system and make sure it references the
;; services you expect. If you didn't create `curator.edn`
;; then you didn't do the required reading. Go back to
;; the beginning of this file and read the documentation!

(defn start
  "Start enough of the system to invoke ingest."
  []
  (->> (mount/with-args {:config (util/read-edn "curator.edn")})
       (mount/start-without #'lcmap.aardvark.server/server
                            #'lcmap.aardvark.worker/worker)))

;; Verify that you are referencing the correct backing services.

(comment
  (start)
  (-> config/config :database :cluster :contact-points)
  (-> config/config :event :host)
  (-> config/config :search :index-url))

;; STEP 2. Create tile-specs. Without tile-specs, the application
;; won't know where to store tile data. Please confirm you have
;; the most recent `data` submodule before using this.

(defn save-tile-specs
  "Save all tile-specs in EDN at path."
  [path]
  (->> path util/read-edn (map tile-spec/save)))

(comment
  (save-tile-specs "tile-specs/L4.edn")
  (save-tile-specs "tile-specs/L5.edn")
  (save-tile-specs "tile-specs/L7.edn")
  (save-tile-specs "tile-specs/L8.edn"))

;; STEP 3. Create some sources. This will create data in Cassandra
;; and will send messages to an exchange that are routed to a queue
;; consumed by a worker. This does not prevent the same source from
;; being enqueued for ingest multiple times.

(defn save-sources
  "Save all sources in EDN at path."
  ([path limit]
   (->> path util/read-edn (take limit) (map source/save)))
  ([path]
   (->> path util/read-edn (map source/save))))

;; Notice that this will enque the first 25 sources listed in
;; each EDN file. For an operational system, it is safe to save
;; tens of thousands of sources. To do this, map over all of
;; the EDN files in a directory with `save-sources`.

(comment
  (save-sources "sources/california/H02V09-35.edn" 1)
  (save-sources "sources/california/H02V09-627.edn" 1)
  (save-sources "sources/california/H02V09-857.edn" 1))

;; Step 4: See how things are going. This will inspect the
;; sources table for activity and check the most recent
;; activity. It is advisable to use Kibana (centralized logging
;; in an operational environment) to get detailed information
;; about ingest activity.
;;
;; PLEASE NOTE:
;; IF NO WORKERS ARE RUNNING THEN THERE WILL BE NO PROGRESS.
;;

(defn progress-report
  "Group sources together by most recent progress/state with count."
  [path]
  (->> (util/read-edn path)
       (map (comp :progress_name last source/search :id))
       (frequencies)))

(comment
  (progress-report "sources/california/H02V09-35.edn")
  (progress-report "sources/california/H02V09-627.edn")
  (progress-report "sources/california/H02V09-857.edn"))

;; Create source EDN from files with checksum/source pairs.

(defn dir->files
  "Produce a list of files contained in directory at path"
  [path]
  (let [fs (file-seq (io/file path))
        tf (remove #(.isDirectory %))]
    (into [] tf fs)))

(defn pair->source
  "Create source"
  [base-uri [checksum source]]
  (let [source-id (last (re-find #"(.+)\.tar\.gz" source))
        source-uri (str base-uri "/" source)]
    {:id source-id
     :uri source-uri
     :checksum checksum}))

(defn file->sources
  "Create list of sources contained in a file."
  [base-uri file]
  (let [path-to-parent (.getParent file)]
  (->> (slurp file)
       (re-seq #"\S+")
       (partition 2)
       (map (partial pair->source base-uri)))))

(defn save-source-as-edn
  "Produce an EDN file containing sources (id, checksum, uri).

  Because the list of sources only contains the filename and checksum,
  an absolute URL need to be built. You can accomplish this two ways:

  Provide a base-uri that does not include any path or parent directories,
  then specify a source-file that is nested in directories. The parent
  directories will be used to form a path to the source file.

  Alternatively, specify a base-url including the path to the file
  name and a path to a source file that does not have any relative parent
  directories. Although this works, it is somewhat confusing.
  "
  [base-uri source-file out-dir]
  (let [tile-name (-> source-file (.getParentFile) (.getName))
        save-path (str out-dir "/" tile-name ".edn")
        tile-path (-> source-file (.getParentFile) (.getPath))
        base-uri (str base-uri "/" tile-path)]
    (->> (file->sources base-uri source-file)
         (util/save-edn save-path))))

(comment
  "This will recursively find *all* files in a directory and produce
  an EDN file for each input file. The name of the EDN file will match
  the name of the immediate parent dir of source file. The EDN fill
  is written to a directory.

  The input directory should only contain text files with a checksum
  and source file name on each line. These files are produced separately
  with a shell script.

  Notice that the nested paths are used to build an absolute URL to
  the actual file. This is needed because the input checksum/source
  does not have an absolute path already; it must be deduced.

  The list of sources should be in directories like this:
  - sites/california/H02V09-35/sources.txt
  - sites/california/H02V09-627/sources.txt
  - sites/california/H02V09-857/sources.txt
  - ...

  The list of outputs will be:
  - data/sources/california/H02V09-35.edn
  - data/sources/california/H02V09-627.edn
  - data/sources/california/H02V09-857.edn
  - ...
  "
  (def ^:dynamic *base-uri* "https://edclpdsftp.cr.usgs.gov/downloads/lcmap")
  (let [files (dir->files "sites/lakes")
        saver #(save-source-as-edn *base-uri* % "data/sources/lakes")]
    (dorun (pmap saver files))))
