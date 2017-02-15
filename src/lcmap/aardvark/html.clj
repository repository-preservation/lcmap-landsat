(ns lcmap.aardvark.html
  "Define templates for various resources."
  (:require [cheshire.core :as json]
            [clj-time.format :as time-fmt]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [lcmap.aardvark.config :as config]
            [net.cgrand.enlive-html :as html]
            [camel-snake-kebab.core :as csk]))

(defn prep-for-html
  ""
  [source]
  (-> source
      (update :progress_at str)
      (update :progress_name str)
      (update :progress_desc str)))

(defn str-vals [kvs]
  ""
  (->> kvs
       (map (fn [[k v]] [k (str v)]))
       (into {})))

;; Used to produce head element, intended for use
;; with all templates.
(html/defsnippet header "public/application.html"
  [:head]
  []
  [:base] (html/set-attr :href (get-in config/config [:html :base-url] "/")))

;; Used to produce navigation element, intended for use
;; with all templates.
(html/defsnippet nav "public/application.html"
  [:nav]
  [])

(html/deftemplate default "public/application.html"
  [entity]
  [:head] (html/substitute (header))
  [:nav]  (html/substitute (nav))
  [:#debug] (html/content (json/encode entity {:pretty true})))

;; Used to produce detailed information about a source.

(html/defsnippet progress "public/source-info.html"
  [:table]
  [sources]
  [:table :> :tr] (html/clone-for
                   [source sources]
                   [:.progress-name] (html/content (:progress_name source))
                   [:.progress-desc] (html/content (:progress_desc source))
                   [:time]           (html/content (str (:progress_at source)))))

(html/deftemplate source-info "public/source-info.html"
  [sources]
  [:head] (html/substitute (header))
  [:nav]  (html/substitute (nav))
  [:#id] (html/content (:id (first sources)))
  [:#uri] (html/content (:uri (first sources)))
  [:#uri] (html/set-attr :href (:uri (first sources)))
  [:#checksum] (html/content (:checksum (first sources)))
  [:table] (html/content (progress sources)))

;; Used to produce a list of links to sources.

(html/deftemplate source-list "public/source-list.html"
  [sources]
  [:head] (html/substitute (header))
  [:nav]  (html/substitute (nav))
  [:tbody :tr] (html/clone-for [source sources]
                               [:a] (html/content (:id source))
                               [:a] (html/set-attr :href (str "source/" (:id source)))))

(html/deftemplate status-list "public/status.html"
  [services]
  [:head] (html/substitute (header))
  [:nav]  (html/substitute (nav))
  [:tbody :tr] (html/clone-for [[svc-name status] services]
                               [:.healthy] (html/content (-> status :healthy str))
                               [:.service] (html/content (name svc-name))
                               [:.message] (html/content (-> status :message str))))

(defn describe-tiles
  ""
  [tiles]
  (let [tally (count tiles)
        tile  (first tiles)
        [ubid x y] (vals (select-keys tile [:ubid :x :y]))]
    (format "%s tiles for band %s contain point (%s, %s)" tally ubid x y)))

(html/deftemplate tile-list "public/tile-list.html"
  [tiles]
  [:head] (html/substitute (header))
  [:nav]  (html/substitute (nav))
  [:header :p] (html/content (describe-tiles tiles))
  [:tbody :tr] (html/clone-for [tile tiles]
                               [:.id] (html/content (str ((juxt :x :y) tile)))
                               [:.source] (html/content (:source tile))
                               [:time] (html/content (-> tile :acquired str))))

(html/deftemplate tile-info "public/tile-info.html"
  [tiles]
  [:head] (html/substitute (header))
  [:nav]  (html/substitute (nav))
  [:content] (html/content "Tile details"))

(html/deftemplate tile-spec-list "public/tile-spec-list.html"
  [tile-specs]
  [:head] (html/substitute (header))
  [:nav]  (html/substitute (nav))
  [:table :> :tr] (html/clone-for [tile-spec tile-specs]
                                  [:a] (html/content (:ubid tile-spec))
                                  [:a] (html/set-attr :href (str "tile-spec/" (:ubid tile-spec)))))

(def geom-fields [:tile_x :tile_y :shift_x :shift_y :pixel_x :pixel_y])

(def data-fields [:data_fill :data_range :data_scale :data_type :data_units :data_shape :data_mask])

(def band-fields [:band_product :band_category :band_name :band_long_name :band_short_name :band_spectrum])

(defn describe-tile-spec
  ""
  [tile-specs]
  (format "%s %s %s" (select-keys tile-specs [:satellite :instrument :sensor])))

(html/deftemplate tile-spec-info "public/tile-spec-info.html"
  [tile-spec]
  [:head] (html/substitute (header))
  [:nav]  (html/substitute (nav))
  [:h2 :#ubid] (html/content (:ubid tile-spec))
  [:pre.wkt]  (html/content (:wkt tile-spec))
  [:pre.geom] (html/content (json/encode (select-keys tile-spec geom-fields)
                                         {:pretty true}))
  [:pre.data] (html/content (json/encode (select-keys tile-spec data-fields)
                                         {:pretty true}))
  [:pre.band] (html/content (json/encode (select-keys tile-spec band-fields)
                                         {:pretty true})))
