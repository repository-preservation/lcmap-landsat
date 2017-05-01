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
            [lcmap.aardvark.chip :as chip]
            [lcmap.aardvark.chip-spec :as chip-spec]
            [lcmap.aardvark.ard :as ard]
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

;; STEP 2. Create chip-specs. Without chip-specs, the application
;; won't know where to store chip data. Please confirm you have
;; the most recent `data` submodule before using this.

(defn save-chip-specs
  "Save all chip-specs in EDN at path."
  [path]
  (->> path util/read-edn (map chip-spec/save)))

(comment
  (save-chip-specs "chip-specs/L4.edn")
  (save-chip-specs "chip-specs/L5.edn")
  (save-chip-specs "chip-specs/L7.edn")
  (save-chip-specs "chip-specs/L8.edn"))

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
  (save-sources "sources/washington/h05v02.edn" 25))

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
  (progress-report "sources/washington/h05v02.edn"))

;; These functions are useful when an EDN file with sources has not been created.

(defn to-source
  ""
  [[checksum filename] base-url]
  (let [uri (str base-url filename)]
    {:id filename :checksum checksum :uri uri}))

(defn manifest-to-sources
  ""
  [url base-url]
  (->> (slurp url)
       (re-seq #"\S+")
       (partition 2)
       (map #(to-source % base-url))))

(comment
  "Create an EDN file that contains pre-made sources."
  (let [tile-id      "h05v02"
        manifest-url (format "https://edclpdsftp.cr.usgs.gov/downloads/collections/tiles-l2-20170427/%s/%s.md5_list" tile-id tile-id)
        base-url     (format "https://edclpdsftp.cr.usgs.gov/downloads/collections/tiles-l2-20170427/%s/" tile-id)]
    #_(util/save-edn (format "data/%s.edn" tile-id) (manifest-to-sources manifest-url base-url))))

;; These are helper functions used to build sample data. These are only needed
;; when creating new test data.

(comment
  "Create chip-specs from ARD source data."
  (let [L4-path "data/ARD/LT04_CU_005001_19821120_20170425_C01_V01.xml"
        L5-path "data/ARD/LT05_CU_005001_19840416_20170425_C01_V01.xml"
        L7-path "data/ARD/LE07_CU_014007_20150223_20170330_C01_V01.xml"
        L8-path "data/ARD/LC08_CU_005002_20130320_20170426_C01_V01.xml"
        opts {:name    "conus"
              :chip_x   3000
              :chip_y  -3000
              :pixel_x  30.0
              :pixel_y -30.0
              :shift_x  2415.0
              :shift_y -195.0
              :data_shape [100 100]
              :wkt "PROJCS[\"Albers\",GEOGCS[\"NAD83\",DATUM[\"North_American_Datum_1983\",SPHEROID[\"GRS 1980\",6378137,298.2572221010042,AUTHORITY[\"EPSG\",\"7019\"]],AUTHORITY[\"EPSG\",\"6269\"]],PRIMEM[\"Greenwich\",0],UNIT[\"degree\",0.0174532925199433],AUTHORITY[\"EPSG\",\"4269\"]],PROJECTION[\"Albers_Conic_Equal_Area\"],PARAMETER[\"standard_parallel_1\",29.5],PARAMETER[\"standard_parallel_2\",45.5],PARAMETER[\"latitude_of_center\",23],PARAMETER[\"longitude_of_center\",-96],PARAMETER[\"false_easting\",0],PARAMETER[\"false_northing\",0],UNIT[\"metre\",1,AUTHORITY[\"EPSG\",\"9001\"]]]"}]
    (util/save-edn "L4.edn" (ard/build-chip-specs L4-path opts))
    (util/save-edn "L5.edn" (ard/build-chip-specs L5-path opts))
    (util/save-edn "L7.edn" (ard/build-chip-specs L7-path opts))
    (util/save-edn "L8.edn" (ard/build-chip-specs L8-path opts))))

(comment
  "Create chips from ARD source data."
  (let [path "ARD/LE07_CU_014007_20150223_20170330_C01_V01_BT.tar"
        source {:id "LE07_CU_014007_20150223_20170330_C01_V01_BT"
                :uri (-> path io/resource io/as-url str)
                :checksum "93ab262902e3199e1372b6f5e2491a98"}]
    (chip/process source))
  (let [path "ARD/LE07_CU_014007_20150223_20170330_C01_V01_QA.tar"
        source {:id "LE07_CU_014007_20150223_20170330_C01_V01_QA"
                :uri (-> path io/resource io/as-url str)
                :checksum "829d66729ec1651713ffb092795bb46e"}]
    (chip/process source))
  (let [path "ARD/LE07_CU_014007_20150223_20170330_C01_V01_SR.tar"
        source {:id "LE07_CU_014007_20150223_20170330_C01_V01_SR"
                :uri (-> path io/resource io/as-url str)
                :checksum "6c06e8b4ce5e8bafb1fe02c26c704237"}]
    (chip/process source))
  (let [path "ARD/LE07_CU_014007_20150223_20170330_C01_V01_TA.tar"
        source {:id "LE07_CU_014007_20150223_20170330_C01_V01_TA"
                :uri (-> path io/resource io/as-url str)
                :checksum "22b221a14196b318acb7b00999a44c8a"}]
    (chip/process source)))
