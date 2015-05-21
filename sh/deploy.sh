#! /bin/bash

SHA1=$1

cd docker
# Create new Elastic Beanstalk version
EB_BUCKET=elasticbeanstalk-us-west-2-525932482084

zip $CIRCLE_ARTIFACTS/streams Dockerfile Dockerrun.aws.json streams.jar

aws s3 cp $CIRCLE_ARTIFACTS/streams.zip s3://$EB_BUCKET/coinstreams/streams-$SHA1.zip
aws elasticbeanstalk create-application-version --application-name coinsignals --version-label $SHA1 --source-bundle S3Bucket=$EB_BUCKET,S3Key=coinstreams/streams-$SHA1.zip

# Update Elastic Beanstalk environment to new version
aws elasticbeanstalk update-environment --environment-name streams-staging --version-label $SHA1