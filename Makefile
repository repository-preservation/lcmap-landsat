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
