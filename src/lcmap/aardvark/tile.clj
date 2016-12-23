(ns lcmap.aardvark.tile
  "Functions for producing and retrieving tile data."
  (:require [clojure.java.io :as io]
            [clojure.core.memoize :as memoize]
            [clojure.set]
            [clojure.tools.logging :as log]
            [dire.core :as dire]
            [gdal.core]
            [gdal.band]
            [gdal.dataset]
            [lcmap.aardvark.config :refer [config]]
            [lcmap.aardvark.db :as db :refer [db-session]]
            [lcmap.aardvark.espa :as espa]
            [lcmap.aardvark.event :as event]
            [lcmap.aardvark.source :as source :refer [progress]]
            [lcmap.aardvark.tile-spec :as tile-spec]
            [lcmap.aardvark.util :as util]
            [lcmap.aardvark.middleware :refer [wrap-handler]]
            [langohr.basic :as lb]
            [mount.core :as mount :refer [defstate]]
            [qbits.alia :as alia]
            [qbits.hayt :as hayt]
            [schema.core :as schema])
  (:refer-clojure :exclude [find time]))

;;; GDAL requires initialization in order to read files.

(defstate gdal
  :start (do
           (log/debug "initializing GDAL")
           (gdal.core/init)))

(def tile-schema
  {:ubid schema/Str
   :x schema/Num
   :y schema/Num
   :acquired [:schema/Str]})

;;; Data type related functions

(defrecord Tile [x y data])

(defprotocol Tiled
  ""
  (shape [data] "Dimensions [cols rows] of data.")
  (steps [data step-x step-y] "Subsetting coordinates within data.")
  (tiles [data step-x step-y] "List of maps with x, y, data."))

(extend-type org.gdal.gdal.Band
  Tiled
  (shape [band]
    (gdal.band/get-size band))
  (steps [band step-x step-y]
    (let [[x-size y-size] (shape band)]
      (for [x (range 0 x-size step-x)
            y (range 0 y-size step-y)]
        [x y step-x step-y])))
  (tiles [band step-x step-y]
    (log/debugf "creating tiles for %s. step-x: %s, step-y: %s" band step-x step-y)
    (for [[x y xs ys] (steps band step-x step-y)]
      (->Tile x y (gdal.band/read-raster-direct band x y xs ys)))))

;;; Helper functions

(defn snap
  "Transform an arbitrary projection system coordinate (x,y) into the
   coordinate of the tile that contains it."
  [x y spec]
  (let [{:keys [tile_x tile_y shift_x shift_y]} spec
        tx (+ shift_x (- x (mod x tile_x)))
        ty (+ shift_y (- y (mod y (- tile_y))))]
    (log/debug "snap using spec (%s): (%d,%d) to (%d,%d)" spec x y tx ty)
    [(long tx) (long ty)]))

;;; Database functions
;; TODO - Add IN clause for query, make ubids a vector instead of single value
(defn find
  "Query DB for all tiles that match the UBID, contain (x,y), and
   were acquired during a certain period of time."
  [{:keys [ubid x y acquired] :as tile}]
  (let [spec     (first (tile-spec/query {:ubid ubid}))
        table    (:name spec)
        [tx ty]  (snap x y spec)
        [t1 t2]  acquired
        where    (hayt/where [[= :ubid ubid]
                              [= :x tx]
                              [= :y ty]
                              [>= :acquired (str t1)]
                              [<= :acquired (str t2)]])]
    (if (nil? spec)
      (throw (ex-info (format "no tile-spec for %s" ubid) {})))
    (log/debugf "find tile %s: %s" table tile)
    (alia/execute db-session (hayt/select table where))))

(defn save
  "Save a tile. This function should be used for all saving that needs
   to happen (in batch) when processing a tile. Currently, this only
   inserts tile data, but it will soon update a scene inventory too."
  [tile]
  (let [params   (-> tile
                     (select-keys [:ubid :proj-x :proj-y :acquired :source :data])
                     (clojure.set/rename-keys {:proj-x :x :proj-y :y}))
        table    (tile :name)]
    (log/tracef "save tile to %s: %s" table params)
    (alia/execute db-session (hayt/insert table (hayt/values params)))))

;;; Tile supporting functions

(defn +spec
  "Retrieve a spec (for the given UBID) and add it to the band. This assumes
   only one tile-spec will be found. If multiple tile-specs exists, behavior
   is undefined."
  [band]
  (log/debugf "using db-session %s to find tile-spec for %s" db-session (:ubid band))
  (let [spec (first (tile-spec/query {:ubid (:ubid band)}))]
    (if (nil? spec)
      (throw (ex-info (format "no tile-spec for %s" (:ubid band))))
      (merge band spec))))

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

(defn- uint8-fill
  "Produce a buffer used to detect UINT8 type buffers all fill data."
  [data-size data-fill]
  (if data-fill
    (let [buffer (java.nio.ByteBuffer/allocate (* (/ Byte/SIZE 8) data-size))
          bytes (byte-array data-size (unchecked-byte data-fill))]
      (-> buffer
          (.order java.nio.ByteOrder/LITTLE_ENDIAN)
          (.put bytes))
      buffer)))

(def fill-buffer
  "Create a buffer memoized on data-size and data-fill."
  (memoize/lu
   (fn [data-size data-fill data-type]
     (cond (= data-type "INT16") (int16-fill data-size data-fill)
           (= data-type "UINT8") (uint8-fill data-size data-fill)))))

(defn +fill
  "Make a fill buffer used to detect no-data tiles"
  [band]
  (log/debug "add fill buffer ...")
  (assoc band :fill (fill-buffer (apply * (band :data_shape))
                                 (band :data_fill)
                                 (band :data_type))))

(defn fill?
  "True if the tile is comprised entirely of fill values"
  [tile]
  (let [data (:data tile)
        fill (:fill tile)]
    (and (some? fill) (some? data) (zero? (.compareTo data fill)))))

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
      (fn [{x :x y :y :as tile}]
        (let [tx (long (+ px (* x sx)))
              ty (long (+ py (* y sy)))]
          (assoc tile :proj-x tx :proj-y ty))))))

(defn +locate
  "Make a raster to projection point transformer function."
  [band]
  (assoc band :locate-fn (locate-fn band)))

(defn locate
  "Use band's locator to turn a raster point to projection point."
  [tile]
  ((:locate-fn tile) tile))

(defn conforms?
  "PLACHOLDER. True if the referenced raster matches the band's tile-spec.
   This ensures the raster is the same projection and that the boundaries
   precisely align to the tile-specs grid values."
  [band]
  true)

(defn scene->bands
  "Create sequence of from ESPA XML metadata."
  [path band-xf]
  (log/debugf "producing bands for %s" path)
  (sequence band-xf (espa/load path)))

(defn dataset->tiles
  "Create sequence of tile from dataset referenced by band."
  [tile-xf dataset x-step y-step]
  (let [image (gdal.dataset/get-band dataset 1)
        tiles (tiles image x-step y-step)]
    (sequence tile-xf tiles)))

(defn process-tile
  "Isolates all side-effect related behavior performed on each tile"
  [tile]
  (io!
   (save tile))
  tile)

(defn process-band
  "Saves band as tiles."
  [band source]
  (gdal.core/with-dataset [dataset (:path band)]
    (let [tile-xf (comp (map #(merge band %))
                        (map locate)
                        (remove fill?))
          [xs ys] (:data_shape band)
          tiles   (dataset->tiles tile-xf dataset xs ys)]
      (progress source "band-start" (format "ubid: %s" (:ubid band)))
      (dorun (pmap process-tile tiles))
      (progress source "band-done" (format "ubid: %s" (:ubid band))))))

(defn process-scene
  "Saves all bands in dir referenced by path."
  ([scene-dir source]
   (let [band-xf   (comp (map +spec)
                         (map +fill)
                         (map +locate)
                         (filter conforms?))]
     (dorun (pmap #(process-band % source) (scene->bands scene-dir band-xf)))
     :done)))

(defn process
  "Generate tiles from an ESPA archive.

  The six states of a source:
  * queue
  * check
  * stage
  * tile
  * done
  * fail
  "
  [{:keys [id checksum uri] :as source}]
  (activity source "check")
  (util/checksum! source)
  (activity source "stage")
  (util/with-temp [dir uri]
    (process-scene dir source))
  (activity source "done")
  :done)

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
    (progress source "fail" (ex-data e))
    :fail))
