FROM usgseros/ubuntu-gis-clj
MAINTAINER USGS LCMAP http://eros.usgs.gov

ENV version 0.1.0-SNAPSHOT
ENV jarfile lcmap-landsat-$version-standalone.jar

RUN mkdir /app
WORKDIR /app
COPY target/uberjar/$jarfile $jarfile
COPY resources/log4j.properties log4j.properties
COPY resources/lcmap-landsat.edn lcmap-landsat.edn


ENTRYPOINT ["java", "-jar", "lcmap-landsat-0.1.0-SNAPSHOT-standalone.jar"]
