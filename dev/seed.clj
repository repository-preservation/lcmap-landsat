(ns seed
  "Provide recipes for seeding Cassandra (and Elasticsearch) with real-world
   congruent data."
  (:require [lcmap.aardvark.tile-spec :as tile-spec]))

(comment
  "This will create tile-specs from real-world data that match what
   is seen production systems. The test data was created when the
   input format was not completely decided. Because this uses real
   data, it can take some time to download."
  (let [spec-opts {:data_shape [100 100]
                   :name "conus"}
        L8 {:id "LC80410282013101-SC20161214141315"
            :checksum "482333697999739445f2b10ac39b709d"
            :uri "https://edclpdsftp.cr.usgs.gov/downloads/lcmap/sites/washington/./clay.austin.ctr@usgs.gov-12142016-124335-732/LC80410282013101-SC20161214141315.tar.gz"}
        L7 {:id "LE070410282000138-SC20161230023316"
            :checksum "b6a2c24eed48101546a2c178d64e3af5"
            :uri "https://edclpdsftp.cr.usgs.gov/downloads/lcmap/sites/washington/./clay.austin.ctr@usgs.gov-12272016-204133-305/LE070410282000138-SC20161230023316.tar.gz"}
        L5 {:id "LT050410281984118-SC20170103075240"
            :checksum "b4f5e1245dcd8078d9981395bace8060"
            :uri "https://edclpdsftp.cr.usgs.gov/downloads/lcmap/sites/washington/./clay.austin.ctr@usgs.gov-12272016-204932-763/LT050410281984118-SC20170103075240.tar.gz"}
        L4 {:id "LT040410281982328-SC20170103075212"
            :checksum "d65532771a5234176ebd125466e206ff"
            :uri "https://edclpdsftp.cr.usgs.gov/downloads/lcmap/sites/washington/./clay.austin.ctr@usgs.gov-12272016-204932-763/LT040410281982328-SC20170103075212.tar.gz"}]
    @(delay (tile-spec/process L8 spec-opts))
    @(delay (tile-spec/process L7 spec-opts))
    @(delay (tile-spec/process L5 spec-opts))
    @(delay (tile-spec/process L4 spec-opts))))
