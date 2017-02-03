[![Build Status](https://travis-ci.org/USGS-EROS/lcmap-landsat.svg?branch=develop)](https://travis-ci.org/USGS-EROS/lcmap-landsat)

<!-- Add the clojars badge once this project is actually pushed there -->
<!--[![Clojars Project][clojars-badge]][clojars]-->

# lcmap-landsat

LCMAP Landsat data ingest, inventory &amp; distribution.

## Usage

#### Retrieve data.  Any number of ubids may be specified.
```bash
# Using httpie
user@machine:~$ http http://host:port/tiles
                     ?x=-2013585
                     &y=3095805
                     &acquired=2000-01-01/2017-01-01
                     &ubid=LANDSAT_8/OLI_TIRS/sr_band1
                     &ubid=LANDSAT_8/OLI_TIRS/sr_band2
                     &ubid=LANDSAT_8/OLI_TIRS/sr_band3
```

#### Retrieve data (alternative endpoint).
This endpoint may provide better performance for single ubid retrievals due to caching, as some HTTP caches do not account for the querystring.  Currently only available for a single ubid at a time.
```bash
# Using httpie
user@machine:~$ http http://host:port/tile/LANDSAT_8/OLI_TIRS/sr_band1
                     ?x=-2013585
                     &y=3095805
                     &acquired=2000-01-01/2017-01-01
```

#### Get all tile-specs.
```bash
user@machine:~$ http http://host:port/tile-specs
```

#### Search for tile-specs.  
?q= parameter uses [ElasticSearch QueryStringSyntax](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html#query-string-syntax).
By default, elastic search applies the query against all indexed fields.

Individual fields may also be searched directly by prepending the query
with the field name plus colon.

Example: ?q=ubid:landsat_7 AND etm AND sr_band1

UBIDS cannot be supplied as is to the ?q parameter, as they are tokens separate by a forward slash "/". This
character denotes a regex expression in elastic search syntax.  See the QueryString query syntax guide above.
```bash
user@machine:~$ http http://host:port/tile-specs
                     ?q=((landsat AND 8) AND NOT sr AND (band1 OR band2 OR band3))
```

lcmap-landsat honors HTTP ```Accept``` headers for both ```application/json```
and ```text/html```.  The default is json.


## Developing
Clone this repository
```bash
git clone git@github.com:usgs-eros/lcmap-landsat
```

Initialize submodules (to get dev/test data).

```bash
git submodule init
git submodule update
```

Install docker-compose (make sure the version support docker-compose.yml version 2 formats).

```bash
# will run infrastructure as a daemon
make docker-deps-up

# Keeps processes in foreground, useful for troubleshooting
make docker-deps-up-nodaemon

# cleanly shut down daemons when done.
make docker-deps-down
```

Run the tests.
```bash
lein test
```

Start a REPL.

```bash
lein run
```

A [FAQ][3] is available for common development & test issues.


## Build, Run & Deployment

Use `lein uberjar` (or `make build`) to build a standalone jarfile.

There are two modes of operation: a web-server that handles HTTP requests, and a worker that handles AMQP messages. A single process can simultaneusly run both modes, although this is not recommended in a production environment.

Execute the jarfile like this, passing configuration data as EDN via STDIN:

```bash
java -jar \
  target/uberjar/lcmap-landsat-0.1.0-SNAPSHOT-standalone.jar \
  $(cat dev/resources/lcmap-landsat.edn)
```

Use `make docker-image` to build a Docker image that includes GDAL dependencies.

[Docker images][2] are automatically built when all tests pass on Travis CI. You may either run the Docker image with additional command line parameters or, if you prefer, build an image using file based configuration.

Example:
```
docker run -p 5679:5679 usgseros/lcmap-landsat:0.1.0-SNAPSHOT $(cat ~/landsat.edn)
```

## Configuration

Example config:
```edn
{:database  {:cluster {:contact-points "172.17.0.1"}
             :default-keyspace "lcmap_landsat"}
 :event     {:host "172.17.0.1"
             :port 5672
             :queues [{:name "lcmap.landsat.server.queue"
                       :opts {:durable true
                              :exclusive false
                              :auto-delete false}}
                      {:name "lcmap.landsat.worker.queue"
                       :opts {:durable true
                              :exclusive false
                              :auto-delete false}}]
             :exchanges [{:name "lcmap.landsat.server.exchange"
                          :type "topic"
                          :opts {:durable true}}
                         {:name "lcmap.landsat.worker.exchange"
                          :type "topic"
                          :opts {:durable true}}]
             :bindings [{:exchange "lcmap.landsat.server.exchange"
                         :queue "lcmap.landsat.worker.queue"
                         :opts {:routing-key "ingest"}}]}
 :http      {:port 5679
             :join? false
             :daemon? true}
 :server    {:exchange "lcmap.landsat.server.exchange"
             :queue    "lcmap.landsat.server.queue"}
 :worker    {:exchange "lcmap.landsat.worker.exchange"
             :queue    "lcmap.landsat.worker.queue"}
 :search     {:index-url      "http://localhost:9200/tile-specs"
              :bulk-api-url   "http://localhost:9200/tile-specs/ops/_bulk"
              :search-api-url "http://localhost:9200/tile-specs/_search"
              :max-result-size 10000}}
```

#### :database
```:cluster``` [options are here.](https://github.com/mpenet/alia/blob/master/docs/guide.md)

#### :event
```:queue```, ```:exchange``` and ```:binding``` opts are in the Langohr docs.

#### :http
Specify Jetty options.

#### :server
```:exchange``` and ```:queue``` from the ```:event``` configuration

#### :worker
```:exchange``` and ```:queue``` from the ```:event``` configuration

#### :search
```:index-url```, ```:bulk-api-url```, and ```:search-api-url``` all require
the full url to each endpoint.  This is to enable lcmap-landsat to make use
of read vs. write endpoints during operational deployments.
```:index-url``` path to elastic search including index name
```:bulk-api-url``` full url for bulk api, including 'index type' value
```:search-api-url``` full url to the search api endpoint
```:max-result-size``` number of results that should be returned from search operations.

[1]: https://github.com/USGS-EROS/lcmap-landsat/blob/develop/resources/shared/lcmap-landsat.edn "Configuration File"
[2]: https://hub.docker.com/r/usgseros/lcmap-landsat/ "Docker Image"
[3]: docs/DevFAQ.md "Developers Frequently Asked Questions"
