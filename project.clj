(defproject lcmap-landsat "0.1.0-SNAPSHOT"
  :description "Landsat HTTP resource & ingest for LCMAP"
  :url "http://github.com/usgs-eros/lcmap-landsat"
  :license {:name "Public Domain"
            :url ""}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 ;; http server
                 [ring/ring "1.5.0"]
                 [ring-jetty-component "0.3.1"]
                 [usgs-eros/ring-accept "0.2.0-SNAPSHOT"]
                 [usgs-eros/ring-problem "0.1.0-SNAPSHOT"]
                 [compojure "1.5.1"]
                 ;; json/xml/html
                 [cheshire "5.6.3"]
                 [org.clojure/data.xml "0.1.0-beta2"]
                 [enlive "1.1.6"]
                 [hiccup "1.0.5"]
                 ;; persistence
                 [com.datastax.cassandra/cassandra-driver-core "3.1.2"]
                 [cc.qbits/alia-all "3.3.0"]
                 [cc.qbits/hayt "3.2.0"]
                 ;; messaging
                 [com.novemberain/langohr "3.6.1"]
                 ;; logging
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-log4j12 "1.7.21"]
                 ;; state management
                 [mount "0.1.10"]
                 ;; configuration
                 [usgs-eros/uberconf "0.1.0-SNAPSHOT"]]
  :profiles {:dev {:resource-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.3.0-alpha3"]
                                  [http-kit "2.2.0"]
                                  [http-kit.fake "0.2.2"]]
                   :plugins [[lein-codox "0.10.0"]
                             [lein-ancient "0.6.10"]
                             [lein-kibit "0.1.2"]
                             [jonase/eastwood "0.2.3"]]}
             :test {:dependencies [[http-kit "2.2.0"]
                                   [http-kit.fake "0.2.2"]]
                    :resource-paths ["test"]}

             :uberjar {:aot :all
                       :main lcmap.aardvark.core}}
  :target-path "target/%s/"
  :compile-path "%s/classes"
  :repl-options {:init-ns dev})
