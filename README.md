[![Build Status](https://travis-ci.org/USGS-EROS/lcmap-landsat.svg?branch=develop)](https://travis-ci.org/USGS-EROS/lcmap-landsat)

<!-- Add the clojars badge once this project is actually pushed there -->
<!--[![Clojars Project][clojars-badge]][clojars]-->

# lcmap-landsat

LCMAP Landsat data ingest, inventory &amp; distribution.

## Development

Install docker-compose (make sure the version support docker-compose.yml version 2 formats).

```bash
cd resources/dev
docker-compose up
```

Start a REPL.

```bash
lein run
```

Switch to the `lcmap.aardvark.dev` namespace and start the system. This
will start a server (using Jetty) and a worker.

```clojure
(dev)
(start)
```

## Building and Running


### Building

Use `lein uberjar` (or `make build`) to build a standalone jarfile.

### Running

There are two modes of operation: a web-server that handles HTTP requests, and a worker that handles AMQP messages. A single process can simultaneusly run both modes, although this is not recommended in a production environment.

Execute the jarfile like this, passing configuration data as EDN via STDIN:

```bash
java -jar \
  target/uberjar/lcmap-landsat-0.1.0-SNAPSHOT-standalone.jar \
  $(cat dev/resources/lcmap-landsat.edn)
```

### Docker Image

Use `make docker-image` to build a Docker image that includes GDAL dependencies.

## Deployment

[Docker images][2] are automatically built when all tests pass on Travis CI. You may either run the Docker image with additional command line parameters or, if you prefer, build an image using file based configuration.

### Links

[1]:https://github.com/USGS-EROS/lcmap-landsat/blob/develop/resources/shared/lcmap-landsat.edn "Configuration File"
[2]:https://hub.docker.com/r/usgseros/lcmap-landsat/ "Docker Image"
