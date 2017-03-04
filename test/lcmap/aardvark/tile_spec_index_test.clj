(ns lcmap.aardvark.tile-spec-index-test
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [lcmap.aardvark.fixtures :as fixtures]
            [lcmap.aardvark.tile-spec :as tile-spec]
            [lcmap.aardvark.tile-spec-index :as index]))

(use-fixtures :once fixtures/with-services)

(use-fixtures :each fixtures/with-data)

(deftest test-indexing

  (testing "+tags"
    (is (not (= nil? (index/+tags (first (tile-spec/all))))))))

  (testing "clear the index"
    (let [out (index/clear)
          raw (index/search "tm")
          err (get-in (first (get-in raw [:root_cause])) [:type])]
      (is (= "index_not_found_exception" err))))
  (testing "load and search the index"
    #_(tile-spec/process L5 tile-spec-opts)
    (is (< 0 (count (index/result (index/search "tm"))))))
  (testing "index search"
    (let [raw (index/search "((tm AND cloud) OR band3) AND NOT shadow AND 5")
          results (index/result raw)
          expected-ubids (seq (vector "LANDSAT_5/TM/sr_cloud_qa"
                                      "LANDSAT_5/TM/sr_adjacent_cloud_qa"
                                      "LANDSAT_5/TM/cfmask_conf"
                                      "LANDSAT_5/TM/cfmask"
                                      "LANDSAT_5/TM/sr_band3"
                                      "LANDSAT_5/TM/toa_band3"))]

      ;; "LANDSAT_7/ETM/sr_band3" "LANDSAT_7/ETM/toa_band3"
      (log/trace "Raw results from search:" raw)
      (log/trace "Raw type:" (type raw))
      (log/trace "Intermediate:" (get-in raw [:hits]))
      (log/debug "Formatted results from search:" results)
      (is (not (= nil? results)))
      (is (seq? results))
      (is (= (sort expected-ubids) (sort (map #(get % :ubid) results)))))))
