(ns seed
  "Provide recipes for seeding Cassandra (and Elasticsearch) with real-world
   congruent data."
  (:require [clojure.edn :as edn]
            [clojure.pprint :refer [pprint]]
            [lcmap.aardvark.source :as source]
            [lcmap.aardvark.tile-spec :as tile-spec]
            [lcmap.aardvark.tile :as tile]))

(comment
  "This will create tile-specs from real-world data that match what
   is seen production systems. The test data was created when the
   input format was not completely decided. Because this uses real
   data, it can take some time to download.

   seed/random-point and seed/contains? can be used from the repl or code to
   find a point that 'may' have data, depending on the number of scenes ingested
   via seed/tiles.  Ingest does not store tiles that are 100% fill data.")

   ;;; If the tiles are changed from path 12 row 30 below, make sure
   ;;; to update the :top :bottom :left and :right values with the proper
   ;;; projection coordinates.  The values can be found in the espa
   ;;; metadata xml file, which can be downloaded using one of the links
   ;;; contained in the target tile .edn file.  Example values:
   ;;; <corner_point location="UL" x="1784430.000000" y="2414790.000000"/>
   ;;; <corner_point location="LR" x="1934400.000000" y="2264820.000000"/>)

(def data {:top 2414790
           :bottom 2264820
           :left 1784430
           :right 1934400

           :L8 { :specs (-> (slurp "data/tile-specs/L8.edn")(edn/read-string))
                 :tiles (-> (slurp "data/sources/chesapeake/LC8012030.edn")
                            (edn/read-string))}

           :L7 { :specs (-> (slurp "data/tile-specs/L7.edn")(edn/read-string))
                 :tiles (-> (slurp "data/sources/chesapeake/LE07012030.edn")
                            (edn/read-string))}

           :L5 { :specs (-> (slurp "data/tile-specs/L5.edn")(edn/read-string))
                 :tiles (-> (slurp "data/sources/chesapeake/LT05012030.edn")
                            (edn/read-string))}

           :L4 { :specs (-> (slurp "data/tile-specs/L4.edn")(edn/read-string))
                 :tiles (-> (slurp "data/sources/chesapeake/LT04012030.edn")
                            (edn/read-string))}})

(defn specs
  "Loads tile specs for a given satellite"
  [satellite]
  (doseq [spec (get-in data [satellite :specs])]
    (tile-spec/save spec)))

(defn tiles
  "Ingests tiles for a given satellite"
  [satellite number]
  (let [tiles (take number (get-in data [satellite :tiles]))]
    (doseq [tile tiles]
      (source/insert tile)
      (tile/process tile))))

(defn covers?
 "Determines if a point is covered by the seed data"
  ([x y]
   (and (and (>= x (:left data))
             (<= x (:right data)))
        (and (>= y (:bottom data))
             (<= y (:top data)))))
  ([point]
   (covers? (first point)(second point))))

(defn random-point
  "Returns a random point from within the seed data spatial range"
  []
  (let [x-distance (Math/abs (- (:right data)(:left data)))
        y-distance (Math/abs (- (:top data)(:bottom data)))
        x (+ (:left data) (rand-int x-distance))
        y (+ (:bottom data) (rand-int y-distance))]
    [x y]))
