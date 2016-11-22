(ns lcmap.aardvark.schema
  (:require [schema.core :as s]))

(def x {})
(def y {})
(def space {})
(def time {})

(def query "select ubid from lcmap.tile_specs")

(def landsat
  "Schema describing entity relationship for landsat"
  {:4 {:tm      {:toa {:red ["band1" "LANDSAT_4/TM/TOA/BAND1"]
                       :blue ["band2" "LANDSAT_4/TM/TOA/BAND1"]
                       :green "band3"}
                 :sr  {:red "band1"
                       :blue "band2"
                       :green "band3"}}}
   :5 {:tm      {:toa {:red "band1"
                       :blue "band2"
                       :green "band3"}
                 :sr  {:red "band1"
                       :blue "band2"
                       :green "band3"}}}
   :7 {:etm     {:toa {:red "band1"
                       :blue "band2"
                       :green "band3"}
                 :sr  {:red "band1"
                       :blue "band2"
                       :green "band3"}}}
   :8 {:olitirs {:toa {:red "band1"
                       :blue "band2"
                       :green "band3"}
                 :sr  {:red "band1"
                       :blue "band2"
                       :green "band3"}}}})

(def crs "Coordinate reference system" {})

(def Ingest
  "Schema constraining operations for landsat ingest"
  {})

(def Search
  "Schema constraining operations for landsat search"
  {})
