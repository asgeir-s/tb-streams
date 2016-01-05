#! /bin/bash

SHA1=$1

cd docker
# Create new Elastic Beanstalk version
EB_BUCKET=tradersbit-staging
FOLDER=streams-app
SERVICE_NAME=streams
APPLICATION_NAME=TradersBit

mv .elasticbeanstalk_staging .elasticbeanstalk
zip $CIRCLE_ARTIFACTS/$SERVICE_NAME Dockerfile Dockerrun.aws.json $SERVICE_NAME.jar .elasticbeanstalk/* .ebextensions/cwl-* .ebextensions/eb-*

aws s3 cp $CIRCLE_ARTIFACTS/$SERVICE_NAME.zip s3://$EB_BUCKET/$FOLDER/$SERVICE_NAME-$SHA1.zip
aws elasticbeanstalk create-application-version --application-name $APPLICATION_NAME --version-label $SHA1 --source-bundle S3Bucket=$EB_BUCKET,S3Key=$FOLDER/$SERVICE_NAME-$SHA1.zip

# Update Elastic Beanstalk environment to new version
aws elasticbeanstalk update-environment --environment-name tb-staging-$SERVICE_NAME --version-label $SHA1