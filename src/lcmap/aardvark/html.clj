(ns lcmap.aardvark.html
  ""
  (:require [cheshire.core :as json]
            [clj-time.format :as time-fmt]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure.spec :as spec]
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

;; Used to produce navigation element, intended for use
;; with all templates.
(html/defsnippet nav "public/application.html"
  [:nav]
  []
  identity)

(html/deftemplate default "public/application.html"
  [entity]
  [:nav] (html/content (nav))
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
  [:nav] (html/content (nav))
  [:#id] (html/content (:id (first sources)))
  [:#uri] (html/content (:uri (first sources)))
  [:#checksum] (html/content (:checksum (first sources)))
  [:table] (html/content (progress sources)))

;; Used to produce a list of links to sources.

(html/deftemplate source-list "public/source-list.html"
  [sources]
  [:nav] (html/content (nav))
  [:tbody :tr] (html/clone-for [source sources]
                               [:a] (html/content (:id source))
                               [:a] (html/set-attr :href (str "/landsat/source/" (:id source)))))

(defn describe-tiles
  ""
  [tiles]
  (let [tally (count tiles)
        tile  (first tiles)
        [ubid x y] (vals (select-keys tile [:ubid :x :y]))]
    (format "%s tiles for band %s contain point (%s, %s)" tally ubid x y)))

(html/deftemplate tile-list "public/tile-list.html"
  [tiles]
  [:nav]       (html/content (nav))
  [:header :p] (html/content (describe-tiles tiles))
  [:tbody :tr] (html/clone-for [tile tiles]
                               [:.id] (html/content (str ((juxt :x :y) tile)))
                               [:.source] (html/content (:source tile))
                               [:time] (html/content (-> tile :acquired str))))

(html/deftemplate tile-info "public/tile-info.html"
  [tiles]
  [:nav] (html/content (nav))
  [:content] (html/content "Tile details"))

(html/deftemplate tile-spec-list "public/tile-spec-list.html"
  [tile-specs]
  [:nav] (html/content (nav))
  [:table :> :tr] (html/clone-for [tile-spec tile-specs]
                                  [:a] (html/content (:ubid tile-spec))
                                  [:a] (html/set-attr :href (str "/landsat/tile-spec/" (:ubid tile-spec)))))

(def geom-fields [:tile_x :tile_y :shift_x :shift_y :pixel_x :pixel_y])

(def data-fields [:data_fill :data_range :data_scale :data_type :data_units :data_shape :data_mask])

(def band-fields [:band_product :band_category :band_name :band_long_name :band_short_name :band_spectrum])

(defn describe-tile-spec
  ""
  [tile-specs]
  (format "%s %s %s" (select-keys tile-specs [:satellite :instrument :sensor])))

(html/deftemplate tile-spec-info "public/tile-spec-info.html"
  [tile-spec]
  [:nav] (html/content (nav))
  [:h2 :#ubid] (html/content (:ubid tile-spec))
  [:pre.wkt]  (html/content (:wkt tile-spec))
  [:pre.geom] (html/content (json/encode (select-keys tile-spec geom-fields)
                                         {:pretty true}))
  [:pre.data] (html/content (json/encode (select-keys tile-spec data-fields)
                                         {:pretty true}))
  [:pre.band] (html/content (json/encode (select-keys tile-spec band-fields)
                                         {:pretty true})))
