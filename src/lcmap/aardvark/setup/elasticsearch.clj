(ns lcmap.aardvark.setup.elasticsearch
  "Prepare Elasticsearch"
  (:require [clojure.tools.logging :as log]
            [lcmap.aardvark.elasticsearch :as es]))

(defn setup
  ""
  [cfg]
  (let [url (get-in cfg [:search :index-url])]
    (log/infof "create Elasticsearch index")
    (log/debugf "use index: %s" url)
    (es/index-create url)))
