FROM usgseros/ubuntu-gis-clj
MAINTAINER USGS LCMAP http://eros.usgs.gov

ENV version 1.0.0-SNAPSHOT
ENV jarfile lcmap-landsat-$version-standalone.jar

RUN mkdir /app
WORKDIR /app
COPY target/uberjar/$jarfile $jarfile
COPY resources/log4j.properties log4j.properties

ENTRYPOINT ["java", "-jar", "lcmap-landsat-1.0.0-SNAPSHOT-standalone.jar"]
