(ns lcmap.aardvark.health
  (:require [metrics.health.core :as h]
            [metrics.counters :as counters]
            [metrics.timers :as timers]
            [lcmap.aardvark.config :as config]
            [lcmap.aardvark.db :as db]
            [lcmap.aardvark.event :as event]
            [lcmap.aardvark.elasticsearch :as es]))

(h/defhealthcheck db-health
  (fn []
    (let [conn-count (.. db/db-cluster
                         (getMetrics)
                         (getOpenConnections)
                         (getValue))]
      (if (zero? conn-count)
        (h/unhealthy "DB connection closed.")
        (h/healthy (format "DB connections: %s" conn-count))))))

(h/defhealthcheck event-health
  (fn []
    (if (-> event/amqp-connection bean :open)
      (h/healthy "AMQP connection open.")
      (h/unhealthy (format "AMQP connection closed.")))))

(h/defhealthcheck es-health
  (fn []
    (let [index-url (get-in config/config [:search :index-url])
          index-info (es/index-get index-url)]
      (if (-> index-info :error :root_cause first :reason)
        (h/unhealthy "Elasticsearch index missing.")
        (h/healthy "Elasticsearch index works.")))))

(defn health-status []
  {:db    (select-keys (bean (h/check db-health))
                       [:message :healthy :error])
   :event (select-keys (bean (h/check event-health))
                       [:message :healthy :error])
   :search (select-keys (bean (h/check es-health))
                        [:message :healthy :error])})
