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

Travis-CI will automatically build and push a jarfile and Docker image for the develop and master branches after all tests pass.

You can build your own jarfile and Docker image using `make build` and `make docker-image`.

The Docker image can be run deployed to a service capable of running Docker containers (e.g. Marathon). Configuration values are provided using environment variables. Defaults for hostname like values correspond to a plausible name for the service; localhost is an unreasonable place for the Docker container to use.

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

## Usage

This application's primary purpose is providing Landsat raster data via HTTP.

Much like the way an operating system retrieves memory in pages, this application retrieves raster imagery as tiles, a stack of spatially and temporally coherent data.

### Retrieve data.  Any number of ubids may be specified.

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

# References

[1]: https://github.com/USGS-EROS/lcmap-landsat/blob/develop/resources/shared/lcmap-landsat.edn "Configuration File"
[2]: https://hub.docker.com/r/usgseros/lcmap-landsat/ "Docker Image"
[3]: docs/DevFAQ.md "Developers Frequently Asked Questions"
