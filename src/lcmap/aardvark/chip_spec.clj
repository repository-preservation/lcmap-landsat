(ns lcmap.aardvark.chip-spec
  "Functions for retrieving and creating chip-specs."
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

(defstate chip-spec-url
  :start (get-in config [:search :chip-spec-url])
  :stop nil)

;; Database support

(defn all
  "Retrieve all chip-specs."
  ([]
   (log/tracef "retrieve all chip-specs")
   (db/execute (hayt/select :chip_specs)))

  ([columns]
   (db/execute (hayt/select :chip_specs
                            (apply hayt/columns columns)))))

(def column-names [:name :ubid :tags :wkt :satellite :instrument
                   :chip_x :chip_y :pixel_x :pixel_y :shift_x :shift_y
                   :band_product :band_category :band_name :band_long_name :band_short_name :band_spectrum
                   :data_fill :data_range :data_scale :data_type
                   :data_units :data_shape :data_mask])

(defn relevant
  "Use to eliminate potentially invalid columns names."
  [chip-spec]
  (select-keys chip-spec column-names))

(defn insert
  "Create chip-spec in DB."
  [chip-spec]
  (log/tracef "insert chip-spec: %s" chip-spec)
  (->> (update chip-spec :tags set)
       (relevant)
       (hayt/values)
       (hayt/insert :chip_specs)
       (db/execute))
  chip-spec)

(defn query
  "Find chip-spec in DB."
  ([params]
   (log/tracef "search for chip-spec: %s" params)
   (db/execute (hayt/select :chip_specs
                            (hayt/where params)
                            (hayt/allow-filtering))))
  ([columns params]
   (db/execute (hayt/select :chip_specs
                            (apply hayt/columns columns)
                            (hayt/where params)
                            (hayt/allow-filtering)))))

;; Search-indexing support

(defn +tags
  "Appends additional tags to a chip-spec"
  [chip-spec]
  (log/debugf "appending additional tags to chip-spec")
  (let [ubid-tags (clojure.string/split (:ubid chip-spec) #"/|_")]
    (update chip-spec :tags concat ubid-tags)))

(defn index
  "Save chip-spec to Elasticsearch."
  [& chip-specs]
  (es/doc-bulk-index chip-spec-url chip-specs {:_id :ubid}))

(defn get
  "Retrive chip-spec from Elasticsearch."
  [ubid]
  (es/doc-get chip-spec-url ubid))

(defn search
  "Query chip-specs in Elasticsearch."
  [query]
  (es/hits->sources (es/search chip-spec-url query {:size 1000})))

;; Convenience functions

(defn save
  "Saves a chip-spec, includes writing to DB and updating search index."
  [chip-spec]
  (some->> chip-spec +tags insert index some?))

;; Utility functions, not commonly used.

(defn dataset->spec
  "Deduce chip spec properties from band's dataset at file_path and band's data_shape"
  [path shape]
  (gdal.core/with-dataset [ds path]
    (let [proj (gdal.dataset/get-projection-str ds)
          [rx px _ ry _ py] (gdal.dataset/get-geo-transform ds)
          [dx dy] shape
          pixel_x (int px)
          pixel_y (int py)
          chip_x  (int (* px dx))
          chip_y  (int (* py dy))
          shift_x (int (mod rx chip_x))
          shift_y (int (mod ry chip_y))]
      {:wkt proj
       :pixel_x pixel_x
       :pixel_y pixel_y
       :chip_x chip_x
       :chip_y chip_y
       :shift_x shift_x
       :shift_y shift_y})))

(defn process-scene
  "Create chip-specs for each band in scene"
  [path opts]
  (doall (for [band (espa/load path)]
           (-> (merge (dataset->spec (:path band) (:data_shape opts))
                      (select-keys (:global_metadata band) [:satellite :instrument])
                      opts
                      band)
               (relevant)
               (save)))))

(defn process
  "Generate chip-specs from an ESPA archive"
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
