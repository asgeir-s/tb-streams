test:
	sbt test

test-s:
	sbt "testOnly com.cluda.*Spec"

test-u:
	sbt "testOnly com.cluda.*Test"

run-l:
	docker run -p 8888:8888 --rm -it coinsignals/streams

build:
	sbt assembly
	docker build -t coinsignals/streams docker/

deploy-s:
	cd docker; eb use streams-staging; eb deploy;

test-s-s:

deploy-p:
	cd docker; eb use streams; eb deploy;

setup-db: