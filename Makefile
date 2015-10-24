test:
	sbt test

test-s:
	sbt "testOnly com.cluda.*Spec"

test-u:
	sbt "testOnly com.cluda.*Test"

run-l:
	docker run -p 8888:8888 --rm -it tradersbit/streams

build:
	sbt assembly
	docker build -t tradersbit/streams docker/

deploy-s: build
	cd docker; eb deploy tb-staging-streams -r us-west-2;

test-s-s:

deploy-p: build
	cd docker; eb deploy tb-streams -r us-east-1;

setup-db: