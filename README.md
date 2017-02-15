[![Build Status](https://travis-ci.org/USGS-EROS/lcmap-landsat.svg?branch=develop)](https://travis-ci.org/USGS-EROS/lcmap-landsat)

<!-- Add the clojars badge once this project is actually pushed there -->
<!--[![Clojars Project][clojars-badge]][clojars]-->

# lcmap-landsat

LCMAP Landsat data ingest, inventory &amp; distribution.

## Development

### Requirements

* Leiningen 2.7.1
* Java 1.8
* Docker 1.13.1
* Docker Compose 1.10.0

### Setup

Clone the repo.

```bash
git clone git@github.com:usgs-eros/lcmap-landsat
```

Initialize submodules (to get dev/test data).

```bash
git submodule init
git submodule update
```

Start backing services.

```bash
make docker-deps-up
```

Run the tests.

```bash
lein test
```

Start a REPL.

```bash
lein run
```

### Sample Data

See `dev/curate.clj` to learn how to load sample data for you to play with during development. You can use your REPL to load a small (or large) amount of data.

### Problems?

If you run into problem, common development and test issues are available in the [FAQ][3].

## Deployment

During routine development, use `dev/user.clj` with a REPL to run the HTTP server and AMQP workers.

### Automated Build

Travis-CI will automatically build a jarfile and Docker image after a successful build of `develop` and `master` branches. The Docker image is pushed to [Docker Hub][2].

You can build your own jarfile and Docker image using `make build` and `make docker-image`.

### Configure

Provide configuration values using environment variables when runnning the application as a jarfile or docker container.

| Environment Variable          | Default    |
|:----------------------------- | ---------- |
| AARDVARK\_BASE\_URL           | /          |
| AARDVARK\_DB\_HOST            | cassandra  |
| AARDVARK\_DB\_USER            | nil        |
| AARDVARK\_DB\_PASS            | nil        |
| AARDVARK\_EVENT\_HOST         | rabbitmq   |
| AARDVARK\_EVENT\_PORT         | 5672       |
| AARDVARK\_EVENT\_USER         | nil        |
| AARDVARK\_EVENT\_PASS         | nil        |
| AARDVARK\_HTTP\_PORT          | 5678       |
| AARDVARK\_SEARCH\_INDEX\_URL  | http://elasticsearch:9200/tile-specs |
| AARDVARK\_SERVER\_EVENTS      | lcmap.landsat.server |
| AARDVARK\_WORKER\_EVENTS      | lcmap.landsat.worker |
| AARDVARK\_LOG\LEVEL           | INFO       |

You may notice that the default hostnames are not localhost. This is because localhost is not a reasonable default for the application to use when running in the context of a Docker container; the container does not provide these services. However, service discovery systems may provide DNS support that Docker automatically can use to resolve these services. If you do not have this available, simply provide the appropriate value instead.

### Deployment

The Docker image can be deployed to services capable of running Docker containers (e.g. Marathon, Kubernetes). Configuration values are provided using environment variables.

Although `dev` and `test` profiles use EDN based configuration files, these are not used in an operational environment.

## Usage

This application's primary purpose is providing Landsat raster data via HTTP. It provides three resources: tiles, tile-specs, and sources.

### Tiles

Much like the way an operating system retrieves memory in pages, this application retrieves raster imagery as tiles, a stack of spatially and temporally coherent data.

```bash
http http://$HOST:$PORT/tiles
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

### Tile-Specs

Tile-Specs describe the geometry and data-type for tiles.

You can get all available tile-specs...

```bash
http http://$HOST:$PORT/tile-specs
```

...or you can use [ElasticSearch QueryStringSyntax][4] to get a subset of available tile-specs. By default, elastic search applies the query against all indexed fields. Individual fields may also be searched directly by prepending the query with the field name plus colon. For example, `?q=ubid:landsat_7 AND etm AND sr_band1`

*Please note: UBIDs contain a forward-slash "/" and cannot be supplied as a ?q parameter; Elasticsearch syntax interprets this character as a regular expression, so they must be escaped.*

```bash
http http://$HOST:$PORT/tile-specs?q=((landsat AND 8) AND NOT sr AND (band1 OR band2 OR band3))
```

You can retrieve JSON (by default) and HTML representations of data by setting the HTTP `Accept` header to `application/json` or `text/html`.

### Sources

Sources provide information about where information was obtained. Each tile has a `source-id` that can be used to determine what file was ingested and which bands were tiled.

A random sample of ten sources can be retrieved like this:

```bash
http http://$HOST:$PORT/sources
```

A specific source can be retrieved like this:

```bash
http http://localhost:5678/source/LT050460261984185-SC20161231013600
``


[1]: https://github.com/USGS-EROS/lcmap-landsat/blob/develop/resources/shared/lcmap-landsat.edn "Configuration File"
[2]: https://hub.docker.com/r/usgseros/lcmap-landsat/ "Docker Image"
[3]: docs/DevFAQ.md "Developers Frequently Asked Questions"
[4]: https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html#query-string-syntax "Elasticsearch query syntax"
