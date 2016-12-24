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

docker-dev-up:
	docker-compose -f dev/resources/docker-compose.yml up -d

docker-dev-up-nodaemon:
	docker-compose -f dev/resources/docker-compose.yml up

docker-dev-down:
	docker-compose -f dev/resources/docker-compose.yml down

docker-test-up:
	docker-compose -f test/resources/docker-compose.yml up -d

docker-test-up-nodaemon:
	docker-compose -f test/resources/docker-compose.yml up

docker-test-down:
	docker-compose -f test/resources/docker-compose.yml down

docker-rm-all: docker-dev-down docker-test-down
	@if [ -n "$(CONTAINERS)" ]; then \
		docker rm $(CONTAINERS); fi;

docker-rmi-all: docker-rm-all
	@if [ -n "$(IMAGES)" ]; then \
		docker rmi $(IMAGES); fi;
