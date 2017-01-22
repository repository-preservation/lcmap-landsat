(ns lcmap.aardvark.espa
  "Functions related to turning ESPA XML metadata into a list of bands."
  (:require [clojure.java.io :as io]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :refer :all]
            [clojure.tools.logging :as log])
  (:refer-clojure :exclude [load]))

;; transformation functions

(def ubid-tags
  "Mapping of ubids to tags"
  {"LANDSAT_4/TM/sr_band1"         #{"blue"}
   "LANDSAT_4/TM/sr_band2"         #{"green"}
   "LANDSAT_4/TM/sr_band3"         #{"red"}
   "LANDSAT_4/TM/sr_band4"         #{"nir"}
   "LANDSAT_4/TM/sr_band5"         #{"swir"}
   "LANDSAT_4/TM/sr_band6"         #{"thermal"}
   "LANDSAT_4/TM/sr_band7"         #{"swir2"}
   "LANDSAT_4/TM/cfmask"           #{"qa"}
   "LANDSAT_5/TM/sr_band1"         #{"blue"}
   "LANDSAT_5/TM/sr_band2"         #{"green"}
   "LANDSAT_5/TM/sr_band3"         #{"red"}
   "LANDSAT_5/TM/sr_band4"         #{"nir"}
   "LANDSAT_5/TM/sr_band5"         #{"swir"}
   "LANDSAT_5/TM/sr_band6"         #{"thermal"}
   "LANDSAT_5/TM/sr_band7"         #{"swir2"}
   "LANDSAT_5/TM/cfmask"           #{"qa"}
   "LANDSAT_7/ETM/sr_band1"        #{"blue"}
   "LANDSAT_7/ETM/sr_band2"        #{"green"}
   "LANDSAT_7/ETM/sr_band3"        #{"red"}
   "LANDSAT_7/ETM/sr_band4"        #{"nir"}
   "LANDSAT_7/ETM/sr_band5"        #{"swir"}
   "LANDSAT_7/ETM/sr_band6"        #{"thermal"}
   "LANDSAT_7/ETM/sr_band7"        #{"swir2"}
   "LANDSAT_7/ETM/cfmask"          #{"qa"}
   "LANDSAT_8/OLI_TIRS/sr_band1"   #{"coastal" "aerosol" "plurpal"}
   "LANDSAT_8/OLI_TIRS/sr_band2"   #{"blue"}
   "LANDSAT_8/OLI_TIRS/sr_band3"   #{"green"}
   "LANDSAT_8/OLI_TIRS/sr_band4"   #{"red"}
   "LANDSAT_8/OLI_TIRS/sr_band5"   #{"nir"}
   "LANDSAT_8/OLI_TIRS/sr_band6"   #{"swir1"}
   "LANDSAT_8/OLI_TIRS/sr_band7"   #{"swir2"}
   "LANDSAT_8/OLI_TIRS/sr_band8"   #{"pan"}
   "LANDSAT_8/OLI_TIRS/sr_band9"   #{"cirrus"}
   "LANDSAT_8/OLI_TIRS/sr_band10"  #{"tirs1 thermal"}
   "LANDSAT_8/OLI_TIRS/sr_band11"  #{"tirs2 thermal"}
   "LANDSAT_8/OLI_TIRS/cfmask"     #{"qa"}
   "LANDSAT_4/TM/toa_band1"        #{"blue"}
   "LANDSAT_4/TM/toa_band2"        #{"green"}
   "LANDSAT_4/TM/toa_band3"        #{"red"}
   "LANDSAT_4/TM/toa_band4"        #{"nir"}
   "LANDSAT_4/TM/toa_band5"        #{"swir"}
   "LANDSAT_4/TM/toa_band6"        #{"thermal"}
   "LANDSAT_4/TM/toa_band7"        #{"swir2"}
   "LANDSAT_5/TM/toa_band1"        #{"blue"}
   "LANDSAT_5/TM/toa_band2"        #{"green"}
   "LANDSAT_5/TM/toa_band3"        #{"red"}
   "LANDSAT_5/TM/toa_band4"        #{"nir"}
   "LANDSAT_5/TM/toa_band5"        #{"swir"}
   "LANDSAT_5/TM/toa_band6"        #{"thermal"}
   "LANDSAT_5/TM/toa_band7"        #{"swir2"}
   "LANDSAT_7/ETM/toa_band1"       #{"blue"}
   "LANDSAT_7/ETM/toa_band2"       #{"green"}
   "LANDSAT_7/ETM/toa_band3"       #{"red"}
   "LANDSAT_7/ETM/toa_band4"       #{"nir"}
   "LANDSAT_7/ETM/toa_band5"       #{"swir"}
   "LANDSAT_7/ETM/toa_band6"       #{"thermal"}
   "LANDSAT_7/ETM/toa_band7"       #{"swir2"}
   "LANDSAT_8/OLI_TIRS/toa_band1"  #{"coastal" "aerosol" "plurpal"}
   "LANDSAT_8/OLI_TIRS/toa_band2"  #{"blue"}
   "LANDSAT_8/OLI_TIRS/toa_band3"  #{"green"}
   "LANDSAT_8/OLI_TIRS/toa_band4"  #{"red"}
   "LANDSAT_8/OLI_TIRS/toa_band5"  #{"nir"}
   "LANDSAT_8/OLI_TIRS/toa_band6"  #{"swir1"}
   "LANDSAT_8/OLI_TIRS/toa_band7"  #{"swir2"}
   "LANDSAT_8/OLI_TIRS/toa_band8"  #{"pan"}
   "LANDSAT_8/OLI_TIRS/toa_band9"  #{"cirrus"}
   "LANDSAT_8/OLI_TIRS/toa_band10" #{"tirs1 thermal"}
   "LANDSAT_8/OLI_TIRS/toa_band11" #{"tirs2 thermal"}})

(defn +acquired
  [scene band]
  (assoc band :acquired (scene :acquired)))

(defn path
  "Get absolute path to band's raster image."
  [scene-dir band]
  (.getAbsolutePath (io/file scene-dir (:file_name band))))

(defn +path
  "Add absolute path to raster for band."
  [scene band]
  (assoc band :path (path scene band)))

(defn source
  "Get source scene ID."
  [scene]
  (let [file-name (:lpgs_file scene)]
    (re-find #"[A-Z0-9]+" file-name)))

(defn +source
  "Add source scene to band."
  [scene band]
  (assoc band :source (source scene)))

(defn ubid
  "Make a UBID for band."
  [scene band]
  (let [vals ((juxt :satellite :instrument :band_name) (merge scene band))]
    (clojure.string/join "/" vals)))

(defn +ubid
  "Add UBID to band."
  [scene band]
  (assoc band :ubid (ubid scene band)))

(defn +tags
  "Add tags to band."
  [scene band]
  (assoc band :tags (get ubid-tags (ubid scene band))))

;; XML parsing functions

(defn solar-angles->map
  "Convert a solar_angles element to a map."
  [global]
  (let [sazip (xml1-> global :solar_angles)
        to-double #(if (some? %) (java.lang.Double/parseDouble %))
        props {:zenith  (to-double (attr sazip :zenith))
               :azimuth (to-double (attr sazip :azimuth))
               :units   (attr sazip :units)}]
    props))

(defn scene-id
  "Use LPGS metadata file to deduce scene ID"
  [global]
  (let [file-name (xml1-> global :lpgs_metadata_file text)]
    (re-find #"[A-Z0-9]+" file-name)))

(defn global->map
  "Convert a global_metadata element to a map."
  [root]
  (let [gmzip (xml1-> root :global_metadata)]
    {:source       (scene-id gmzip)
     :lpgs_file    (xml1-> gmzip :lpgs_metadata_file text)
     :satellite    (xml1-> gmzip :satellite text)
     :instrument   (xml1-> gmzip :instrument text)
     :provider     (xml1-> gmzip :data_provider text)
     :acquired     (xml1-> gmzip :acquisition_date text)
     :solar_angles (solar-angles->map gmzip)}))

(defn data-range->list
  "Convert a valid_range element into a list."
  [band]
  (if-let [element (xml1-> band :valid_range)]
    [(attr element :min)
     (attr element :max)]))

(defn mask-values->map
  "Convert a class_values element into a map."
  [band]
  (if-let [items (concat (xml-> band :class_values :class)
                         (xml-> band :bitmap_description :bit))]
    (into {} (for [item items]
               [(Integer/parseInt (attr item :num)) (text item)]))))

(defn bands->list
  "Convert all band elements into a list of maps."
  [root]
  (for [band (xml-> root :bands :band)
        :let [props {:file_name  (xml1-> band :file_name text)
                     :band_short_name (xml1-> band :short_name text)
                     :band_long_name  (xml1-> band :long_name text)
                     :band_category   (attr band :category)
                     :band_product    (attr band :product)
                     :band_name       (attr band :name)
                     :data_type  (attr band :data_type)
                     :data_fill  (some-> (attr band :fill_value) Short/parseShort)
                     :data_scale (some-> (attr band :scale_factor) Double/parseDouble)
                     :data_range (map #(if (some? %) (int (Double/parseDouble %)))
                                      (data-range->list band))
                     :data_units (attr band :data_units)
                     :data_mask  (mask-values->map band)}]]
    props))

(defn find-xml
  "Gets the path to the metadata file of an ESPA archive.

  If multiple XML files are present then the first match
  is used -- this shouldn't be the case and is silently
  ignored for now."
  [path]
  (let [files (-> path io/file file-seq)
        names (map #(.getPath ^java.io.File %) files)
        xml   (filter #(re-find #".+\.xml" %) names)]
    (first xml)))

(defn parse-bands
  "Create a list of bands from ESPA metadata."
  [path]
  (log/debug "parsing metadata file:" path)
  (let [data (xml/parse path)
        root (zip/xml-zip data)]
    (bands->list root)))

(defn load-bands
  "Build list of bands from ESPA metadata."
  [dir]
  (let [xml-path (find-xml dir)]
    (map #(+path dir %) (parse-bands xml-path))))

(defn parse-global-metadata
  "Process global_metadata element of XML at path."
  [path]
  (let [data (xml/parse path)
        root (zip/xml-zip data)]
    (global->map root)))

(defn load-global-metadata
  "Build global metadata map from ESPA metadata."
  [dir]
  (let [xml-path (find-xml dir)]
    (parse-global-metadata xml-path)))

(defn load
  "Build a list of bands combined with global metadata."
  [dir]
  (let [bands (load-bands dir)
        scene (load-global-metadata dir)]
    (sequence (comp (map #(+acquired scene %))
                    (map #(+path dir %))
                    (map #(+source scene %))
                    (map #(+ubid scene %))
                    (map #(+tags scene %))
                    (map #(assoc % :global_metadata scene)))
              bands)))
