CONTAINERS=`docker ps -a -q`
IMAGES=`docker images -q`

build:
	lein uberjar

docker-image:
	docker build --tag usgseros/lcmap-landsat:0.1.0-SNAPSHOT .

docker-shell:
	docker run -it --entrypoint=/bin/bash usgseros/lcmap-landsat:0.1.0-SNAPSHOT

docker-up:
	docker run usgseros/lcmap-landsat:0.1.0-SNAPSHOT

docker-deps-up:
	docker-compose -f resources/docker-compose.yml up -d

docker-deps-up-nodaemon:
	docker-compose -f resources/docker-compose.yml up

docker-deps-down:
	docker-compose -f	resources/docker-compose.yml down

docker-rm-all: docker-deps-down
	@if [ -n "$(CONTAINERS)" ]; then \
		docker rm $(CONTAINERS); fi;

docker-rmi-all: docker-rm-all
	@if [ -n "$(IMAGES)" ]; then \
		docker rmi $(IMAGES); fi;
