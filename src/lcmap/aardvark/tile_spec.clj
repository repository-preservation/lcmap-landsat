(ns lcmap.aardvark.tile-spec
  "Functions for retrieving and creating tile-specs."
  (:require [clojure.tools.logging :as log]
            [gdal.core]
            [gdal.dataset]
            [lcmap.aardvark.db :as db :refer [db-session]]
            [lcmap.aardvark.espa :as espa]
            [lcmap.aardvark.util :as util]
            [mount.core :as mount :refer [defstate]]
            [qbits.alia :as alia]
            [qbits.hayt :as hayt]
            [qbits.hayt.cql :as cql]
            [schema.core :as schema])
  (:refer-clojure :exclude [find]))

(defstate gdal
  :start (do
           (log/debug "initializing GDAL")
           (gdal.core/init)))

(def tile-spec-schema
  {:keyspace_name schema/Str
   :table_name schema/Str
   :ubid schema/Str
    schema/Keyword schema/Str})

(defn validate
  "Produce a map of errors if the tile-spec is invalid, otherwise nil."
  [tile-spec]
  (log/tracef "validate tile-spec: %s" tile-spec)
  (schema/check tile-spec-schema tile-spec))

(defn all
  "Retrieve all tile-specs."
  []
  (log/tracef "retrieve all tile-specs")
  (alia/execute db-session
                (hayt/select :tile_specs)))

(defn query
  "Find tile-spec in DB."
  [params]
  (log/tracef "search for tile-spec: %s" params)
  (alia/execute db-session
                (hayt/select :tile_specs
                   (hayt/where params)
                   (hayt/allow-filtering))))

(defn insert
  "Create tile-spec in DB."
  [tile-spec]
  (log/tracef "insert tile-spec: %s" tile-spec)
  (alia/execute db-session
                (hayt/insert :tile_specs (hayt/values tile-spec)))
  tile-spec)

;;; Worker related

(def column-names [:keyspace_name :table_name
                   :ubid :projection :satellite :instrument
                   :tile_x :tile_y :pixel_x :pixel_y :shift_x :shift_y
                   :band_product :band_category :band_name :band_long_name :band_short_name :band_spectrum
                   :data_fill :data_range :data_scale :data_type :data_units :data_shape :data_mask])

(defn relevant
  "Use to eliminate potentially invalid columns names."
  [tile-spec]
  (select-keys tile-spec column-names))

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
          shift_x (int (mod rx pixel_x))
          shift_y (int (mod ry pixel_y))]
      {:projection proj
       :pixel_x pixel_x
       :pixel_y pixel_y
       :tile_x tile_x
       :tile_y tile_x
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
               (insert)))))

(defn process
  "Generate tile-specs from an ESPA archive"
  [{:keys [id checksum uri] :as source} opts]
  (if (util/checksum! source)
    (util/with-temp [dir uri]
      (process-scene dir opts)
      :done)
    :failed))
