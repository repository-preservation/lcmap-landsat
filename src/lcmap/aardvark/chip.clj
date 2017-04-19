(ns lcmap.aardvark.chip
  "Functions for producing and retrieving chip data."
  (:require [clojure.java.io :as io]
            [clojure.core.memoize :as memoize]
            [clojure.set]
            [clojure.tools.logging :as log]
            [clj-time.format :as time-fmt]
            [dire.core :as dire]
            [gdal.core]
            [gdal.band]
            [gdal.dataset]
            [lcmap.aardvark.config :refer [config]]
            [lcmap.aardvark.db :as db]
            [lcmap.aardvark.espa :as espa]
            [lcmap.aardvark.event :as event]
            [lcmap.aardvark.source :as source :refer [progress]]
            [lcmap.aardvark.chip-spec :as chip-spec]
            [lcmap.aardvark.util :as util]
            [lcmap.aardvark.middleware :refer [wrap-handler]]
            [lcmap.commons.numbers :refer [numberize]]
            [lcmap.commons.collections :refer [vectorize]]
            [lcmap.commons.chip :refer [snap]]
            [langohr.basic :as lb]
            [me.raynes.fs :as fs]
            [mount.core :as mount :refer [defstate]]
            [qbits.hayt :as hayt]
            [schema.core :as schema])
  (:refer-clojure :exclude [find time]))

;; use this to turn assertions off when not needed (post troubleshooting)
;; (set! *assert* true)

;;; GDAL requires initialization in order to read files.

(defstate gdal
  :start (do
           (log/debug "initializing GDAL")
           (gdal.core/init)))

;;; Data type related functions

(defrecord Chip [x y data])

(defprotocol Chipper
  ""
  (shape [data] "Dimensions [cols rows] of data.")
  (steps [data step-x step-y] "Subsetting coordinates within data.")
  (chips [data step-x step-y] "List of maps with x, y, data."))

(extend-type org.gdal.gdal.Band
  Chipper
  (shape [band]
    (gdal.band/get-size band))
  (steps [band step-x step-y]
    (let [[x-size y-size] (shape band)]
      (for [x (range 0 x-size step-x)
            y (range 0 y-size step-y)]
        [x y step-x step-y])))
  (chips [band step-x step-y]
    (log/debugf "creating chips for %s. step-x: %s, step-y: %s" band step-x step-y)
    (for [[x y xs ys] (steps band step-x step-y)]
      (->Chip x y (gdal.band/read-raster-direct band x y xs ys)))))

;;; Query validation

(defn conform
  "Used for transform maps of string values to other types."
  [{:keys [:ubid :x :y :acquired] :as params}]
  {:x        (some-> x numberize)
   :y        (some-> y numberize)
   :ubids    (some-> ubid vectorize)
   :acquired (some-> acquired util/parse-date-interval)})

(defn validate
  "Remove keys with values from the map, only invalid items remain."
  [params]
  (let [nil-value? (fn [[k v]] (nil? v))]
    (into {} (filter nil-value? params))))

;;; Database functions

(defn find
  "Query DB for all chips that match the UBIDs, contain (x,y), and
   were acquired during a certain period of time."
  [{:keys [ubids x y acquired] :as chips}]
  {:pre [(vector? ubids) (integer? x) (integer? y) (every? some? acquired)]}
  (if-let [spec (first (chip-spec/query
                        [:name :chip_x :chip_y :shift_x :shift_y]
                        [[:in :ubid ubids]]))]
    (let [table    (:name spec)
          [tx ty]  (snap x y spec)
          [t1 t2]  acquired
          where    (hayt/where [[:in :ubid ubids]
                                [= :x tx]
                                [= :y ty]
                                [>= :acquired (str t1)]
                                ;; TODO: check t2 for nil before using.
                                [<= :acquired (str t2)]])]
      (log/debugf "Finding chip(s) %s: %s" table chips)
      (db/execute (hayt/select table where)))

    (do (log/warnf "No chip-specs found for %s" ubids)
        (sequence []))))

(defn save
  "Save a chip. This function should be used for all saving that needs
   to happen (in batch) when processing a chip. Currently, this only
   inserts chip data, but it will soon update a scene inventory too."
  [chip]
  (let [params   (-> chip
                     (select-keys [:ubid :proj-x :proj-y :acquired :source :data])
                     (clojure.set/rename-keys {:proj-x :x :proj-y :y}))
        table    (chip :name)]
    (log/tracef "save chip to %s: %s" table params)
    (db/execute (hayt/insert table (hayt/values params)))))

;;; Chip supporting functions

(defn +spec
  "Retrieve a spec (for the given UBID) and add it to the band. This assumes
   only one chip-spec will be found. If multiple chip-specs exists, behavior
   is undefined.

   If a chip-spec is not found, then this function returns nil. The band should
   not be ingested."
  [band]
  (log/debugf "looking for chip-spec for %s" (:ubid band))
  (let [spec (first (chip-spec/query {:ubid (:ubid band)}))]
    (if (some? spec)
      (merge band spec)
      (log/warnf "no chip-spec present, skipping %s" (:ubid band)))))

(defn int16-fill
  "Produce a buffer used to detect INT16 type buffers containing all fill data."
  [data-size data-fill]
  (if data-fill
    (let [buffer (java.nio.ByteBuffer/allocate (* (/ Short/SIZE 8) data-size))
          shorts (short-array data-size (unchecked-short data-fill))]
      (-> buffer
          (.order java.nio.ByteOrder/LITTLE_ENDIAN)
          (.asShortBuffer)
          (.put shorts))
      buffer)))

(defn uint8-fill
  "Produce a buffer used to detect UINT8 type buffers all fill data."
  [data-size data-fill]
  (if data-fill
    (let [buffer (java.nio.ByteBuffer/allocate (* (/ Byte/SIZE 8) data-size))
          bytes (byte-array data-size (unchecked-byte data-fill))]
      (-> buffer
          (.order java.nio.ByteOrder/LITTLE_ENDIAN)
          (.put bytes))
      buffer)))

(defn uint16-fill
  "Produce a buffer used to detect UINT16 type buffers all fill data."
  [data-size data-fill]
  (if data-fill
    (let [buffer (java.nio.ByteBuffer/allocate (* 2 data-size))
          bytes (byte-array data-size (unchecked-byte data-fill))]
      (-> buffer
          (.order java.nio.ByteOrder/LITTLE_ENDIAN)
          (.put bytes))
      buffer)))

(def fill-buffer
  "Create a buffer memoized on data-size and data-fill."
  (fn [data-size data-fill data-type]
    (cond (= data-type "INT16") (int16-fill data-size data-fill)
          (= data-type "UINT8") (uint8-fill data-size data-fill)
          (= data-type "UINT16") (uint16-fill data-size data-fill))))

(defn +fill
  "Make a fill buffer used to detect no-data chips"
  [band]
  (log/debug "add fill buffer ...")
  (assoc band :fill (fill-buffer (apply * (band :data_shape))
                                 (band :data_fill)
                                 (band :data_type))))

(defn fill?
  "True if the chip is comprised entirely of fill values"
  [chip]
  (let [data (:data chip)
        fill (:fill chip)]
    (and (some? fill) (some? data) (zero? (.compareTo (.rewind data)
                                                      (.rewind fill))))))

(defn locate-fn
  "Build projection coordinate point calculator for GDAL dataset."
  [band]
  (log/debug "creating locate fn for band ...")
  ;; XXX It's possible to use a GDAL transform function to obtain
  ;;     the projection system coordinates for a pixel instead of
  ;;     doing the arithmetic ourselves. However, this approach is
  ;;     simple-enough for now.
  (gdal.core/with-dataset [dataset (band :path)]
    (let [[px sx _ py _ sy] (gdal.dataset/get-geo-transform dataset)]
      (fn [{x :x y :y :as chip}]
        (let [tx (long (+ px (* x sx)))
              ty (long (+ py (* y sy)))]
          (assoc chip :proj-x tx :proj-y ty))))))

(defn +locate
  "Make a raster to projection point transformer function."
  [band]
  (assoc band :locate-fn (locate-fn band)))

(defn locate
  "Use band's locator to turn a raster point to projection point."
  [chip]
  ((:locate-fn chip) chip))

(defn conforms?
  "PLACHOLDER. True if the referenced raster matches the band's chip-spec.
   This ensures the raster is the same projection and that the boundaries
   precisely align to the chip-specs grid values."
  [band]
  (some? band))

(defn scene->bands
  "Create sequence of from ESPA XML metadata."
  [path band-xf]
  (log/debugf "producing bands for %s" path)
  (sequence band-xf (espa/load path)))

(defn dataset->chips
  "Create sequence of chip from dataset referenced by band."
  [chip-xf dataset x-step y-step]
  (let [image (gdal.dataset/get-band dataset 1)
        chips (chips image x-step y-step)]
    (sequence chip-xf chips)))

(defn process-chip
  "Isolates all side-effect related behavior performed on each chip"
  [chip]
  (io!
   (save chip))
  chip)

(defn process-band
  "Saves band as chips."
  [band source]
  (gdal.core/with-dataset [dataset (:path band)]
    (let [chip-xf (comp (map #(merge band %))
                        (map locate)
                        (remove fill?))
          [xs ys] (:data_shape band)
          chips   (dataset->chips chip-xf dataset xs ys)]
      (progress source "band-start" (:ubid band))
      (dorun (map process-chip chips))
      (progress source "band-done" (:ubid band)))))

(defn process-scene
  "Saves all bands in dir referenced by path."
  ([scene-dir source]
   (let [band-xf   (comp (map +spec)
                         (filter conforms?)
                         (map +fill)
                         (map +locate)
                         (map (fn [band] (assoc band :source (:id source)))))]
     (dorun (map #(process-band % source) (scene->bands scene-dir band-xf)))
     :done)))

(defn process
  "Generate chips from an ESPA archive.

  The six states of a source:
  * queue
  * check
  * stage
  * chip
  * done
  * fail
  "
  [{:keys [id checksum uri] :as source}]
  (let [download-file   (fs/temp-file "lcmap-")
        uncompress-file (fs/temp-file "lcmap-")
        unarchive-file  (fs/temp-dir  "lcmap-")]
    (try
      (progress source "scene-start")
      (-> uri
          (util/download download-file)
          (util/verify checksum)
          (util/uncompress uncompress-file)
          (util/unarchive unarchive-file)
          (process-scene source))
      (progress source "scene-finish")
      :done
      (catch clojure.lang.ExceptionInfo ex
        (log/errorf "scene %s failed to process: %s" id (.getMessage ex))
        (progress source "fail" (.getMessage ex))
        :fail)
      (finally
        (fs/delete-dir unarchive-file)
        (fs/delete uncompress-file)
        (fs/delete download-file)))))

;;; Error handlers

(dire/with-handler! #'process
  org.apache.commons.compress.compressors.CompressorException
  (fn [e & [source]]
    (progress source "fail" "could not decompress source")
    :fail))

(dire/with-handler! #'process
  java.io.FileNotFoundException
  (fn [e & [source]]
    (progress source "fail" "could not find file")
    :fail))

(dire/with-handler! #'process
  java.net.MalformedURLException
  (fn [e & [source]]
    (progress source "fail" "malformed URL")
    :fail))

(dire/with-handler! #'process
  java.lang.IllegalArgumentException
  (fn [e & [source]]
    (progress source "fail" "invalid ESPA metadata")
    :fail))

(dire/with-handler! #'process
  clojure.lang.ExceptionInfo
  (fn [e & [source]]
    (progress source "fail" (:msg (ex-data e)))
    :error))

(dire/with-handler! #'process
  clojure.lang.ExceptionInfo
  (fn [e & [source]]
    (progress source "fail" (:msg (ex-data e)))
    :fail))
