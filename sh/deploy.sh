#! /bin/bash

SHA1=$1

cd docker
# Create new Elastic Beanstalk version
EB_BUCKET=coinsignals
FOLDER=streams-app
SERVICE_NAME=streams
APPLICATION_NAME=coinsignals
AWS_DEFAULT_REGION=us-west-2

zip $CIRCLE_ARTIFACTS/$SERVICE_NAME Dockerfile Dockerrun.aws.json $SERVICE_NAME.jar

aws s3 cp $CIRCLE_ARTIFACTS/$SERVICE_NAME.zip s3://$EB_BUCKET/$FOLDER/$SERVICE_NAME-$SHA1.zip
aws elasticbeanstalk create-application-version --application-name $APPLICATION_NAME --version-label $SHA1 --source-bundle S3Bucket=$EB_BUCKET,S3Key=$FOLDER/$SERVICE_NAME-$SHA1.zip

# Update Elastic Beanstalk environment to new version
aws elasticbeanstalk update-environment --environment-name cs-$SERVICE_NAME-staging --version-label $SHA1