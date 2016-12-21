build:
	lein uberjar

docker-image:
	docker build --tag usgseros/lcmap-landsat:0.1.0-SNAPSHOT .

docker-shell:
	docker run -it --entrypoint=/bin/bash usgseros/lcmap-landsat:0.1.0-SNAPSHOT

docker-up:
	docker run usgseros/lcmap-landsat:0.1.0-SNAPSHOT

docker-super-clean:
	docker rm `docker ps -a -q`
	docker rmi `docker images -q`

docker-dev-up:
	docker-compose -f dev/resources/docker-compose.yml up -d

docker-dev-up-nodaemon:
		docker-compose -f dev/resources/docker-compose.yml up

docker-dev-down:
	docker-compose -f dev/resources/docker-compose.yml down

docker-test-up:
	docker-compose -f test/resources/docker-compose.yml up -d

docker-test-up-nodaemon:
	docker-compose -f test/resources/docker-compose.yml up -d

docker-test-down:
	docker-compose -f test/resources/docker-compose.yml down
