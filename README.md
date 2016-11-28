[![Build Status][travis-badge]][travis][![Clojars Project][clojars-badge]][clojars]

# lcmap-landsat

LCMAP Landsat data ingest, inventory &amp; distribution

### Development

Install docker-compose (make sure the version support docker-compose.yml version 2 formats).

```bash
cd resources/dev
docker-compose up
```

## Configuration

### File Based (using Extensible Data Notation / EDN)

This application expects `lcmap-landsat.edn` to be present on the resource-path for configuration. See [the example configuration][1] for configuration options.

### Command Line Parameters

Command line parameters are useful most useful when running the [`usgseros/lcmap-landsat` Docker image] and you don't want to build a subsequent image with a configuration file.

This is the example Java command:

```
java -jar lcmap-landsat-0.1.0-SNAPSHOT-standalone.jar
  --http.port 5678 \
  --http.join? true \
  --http.daemon? false \
  --database.contact-points localhost \
  --event.host localhost \
  --event.port 5672
```

This is the example Docker command:

```
docker run usgseros/lcmap-landsat:latest \
  --http.port 5678 \
  --http.join? true \
  --http.daemon? false \
  --database.contact-points localhost \
  --event.host localhost \
  --event.port 5672
```

## Deployment

[Docker images][2] are automatically built when all tests pass on Travis CI. You may either run the Docker image with additional command line parameters or, if you prefer, build an image using file based configuration.

### Links

[1]:https://github.com/USGS-EROS/lcmap-landsat/blob/develop/resources/shared/lcmap-landsat.edn "Configuration File"
[2]:https://hub.docker.com/r/usgseros/lcmap-landsat/ "Docker Image"
