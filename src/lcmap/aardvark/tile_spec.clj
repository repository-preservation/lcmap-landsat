(ns lcmap.aardvark.tile-spec
  "Functions for retrieving and creating tile-specs."
  (:require [clojure.tools.logging :as log]
            [gdal.core]
            [gdal.dataset]
            [lcmap.aardvark.espa :as espa]
            [lcmap.aardvark.util :as util]
            [me.raynes.fs :as fs]
            [mount.core :as mount :refer [defstate]]
            [qbits.alia :as alia]
            [qbits.hayt :as hayt]
            [qbits.hayt.cql :as cql]
            [schema.core :as schema]
            [lcmap.aardvark.db :as db]
            [lcmap.aardvark.config :refer [config]]
            [lcmap.aardvark.elasticsearch :as es])
  (:refer-clojure :exclude [find]))

;; States

(defstate gdal
  :start (do
           (log/debug "initializing GDAL")
           (gdal.core/init)))

(defstate tile-spec-url
  :start (get-in config [:search :tile-spec-url])
  :stop nil)

;; Database support

(defn all
  "Retrieve all tile-specs."
  ([]
   (log/tracef "retrieve all tile-specs")
   (db/execute (hayt/select :tile_specs)))

  ([columns]
   (db/execute (hayt/select :tile_specs
                            (apply hayt/columns columns)))))

(def column-names [:name :ubid :tags :wkt :satellite :instrument
                   :tile_x :tile_y :pixel_x :pixel_y :shift_x :shift_y
                   :band_product :band_category :band_name :band_long_name :band_short_name :band_spectrum
                   :data_fill :data_range :data_scale :data_type
                   :data_units :data_shape :data_mask])

(defn relevant
  "Use to eliminate potentially invalid columns names."
  [tile-spec]
  (select-keys tile-spec column-names))

(defn insert
  "Create tile-spec in DB."
  [tile-spec]
  (log/tracef "insert tile-spec: %s" tile-spec)
  (->> (update tile-spec :tags set)
       (relevant)
       (hayt/values)
       (hayt/insert :tile_specs)
       (db/execute))
  tile-spec)

(defn query
  "Find tile-spec in DB."
  ([params]
   (log/tracef "search for tile-spec: %s" params)
   (db/execute (hayt/select :tile_specs
                            (hayt/where params)
                            (hayt/allow-filtering))))
  ([columns params]
   (db/execute (hayt/select :tile_specs
                            (apply hayt/columns columns)
                            (hayt/where params)
                            (hayt/allow-filtering)))))

;; Search-indexing support

(defn +tags
  "Appends additional tags to a tile-spec"
  [tile-spec]
  (log/debugf "appending additional tags to tile-spec")
  (let [ubid-tags (clojure.string/split (:ubid tile-spec) #"/|_")]
    (update tile-spec :tags concat ubid-tags)))

(defn index
  "Save tile-spec to Elasticsearch."
  [& tile-specs]
  (es/doc-bulk-index tile-spec-url tile-specs {:_id :ubid}))

(defn get
  "Retrive tile-spec from Elasticsearch."
  [ubid]
  (es/doc-get tile-spec-url ubid))

(defn search
  "Query tile-specs in Elasticsearch."
  [query]
  (es/hits->sources (es/search tile-spec-url query {:size 1000})))

;; Convenience functions

(defn save
  "Saves a tile-spec, includes writing to DB and updating search index."
  [tile-spec]
  (some->> tile-spec +tags insert index some?))

;; Utility functions, not commonly used.

(defn dataset->spec
  "Deduce tile spec properties from band's dataset at file_path and band's data_shape"
  [path shape]
  (gdal.core/with-dataset [ds path]
    (let [proj (gdal.dataset/get-projection-str ds)
          [rx px _ ry _ py] (gdal.dataset/get-geo-transform ds)
          [dx dy] shape
          pixel_x (int px)
          pixel_y (int py)
          tile_x  (int (* px dx))
          tile_y  (int (* py dy))
          shift_x (int (mod rx tile_x))
          shift_y (int (mod ry tile_y))]
      {:wkt proj
       :pixel_x pixel_x
       :pixel_y pixel_y
       :tile_x tile_x
       :tile_y tile_y
       :shift_x shift_x
       :shift_y shift_y})))

(defn process-scene
  "Create tile-specs for each band in scene"
  [path opts]
  (doall (for [band (espa/load path)]
           (-> (merge (dataset->spec (:path band) (:data_shape opts))
                      (select-keys (:global_metadata band) [:satellite :instrument])
                      opts
                      band)
               (relevant)
               (save)))))

(defn process
  "Generate tile-specs from an ESPA archive"
  [{:keys [id checksum uri] :as source} opts]
  (let [download-file   (fs/temp-file "lcmap-")
        uncompress-file (fs/temp-file "lcmap-")
        unarchive-file  (fs/temp-dir  "lcmap-")]
    (try
      (-> uri
          (util/download download-file)
          (util/verify checksum)
          (util/uncompress uncompress-file)
          (util/unarchive unarchive-file)
          (process-scene opts))
      :done
      (finally
        (fs/delete-dir unarchive-file)
        (fs/delete uncompress-file)
        (fs/delete download-file)))))
