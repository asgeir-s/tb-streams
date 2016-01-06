# Streams Microservice
Microservice used in the tradersbit project. 

## Interface
	POST: /streams {
	    "name": "Unique Name"
        "exchange": "bitstamp",
        "currencyPair": "btcUSD",
        "payoutAddress": "publishers-bitcoin-address",
        "subscriptionPriceUSD": 5
    } 	returns => {"id": "someId", "apiKeyId": "someApiKeyId"}
    
    POST: /streams/'streamID'/signals {
    	"timestamp": 1432122282747,
    	"price": 200.453,
    	"change": 0,
    	"id": 1,
    	"value": 100,
    	"signal": 1
    }
    
    POST: /streams/'streamID'/subscription-price {12.50}
    
    GET:  /streams/'streamID' returns Stream info and stats
    
    GET: /streams returns stream info for all streams
    
    POST: /streams/get {
        "streams": ["streamID1", "streamID2"],
        "infoLevel": "public" (public (this is info that can be avalibele to anyone) | auth (this is info for the publisher of the stream) | private (should not be a public endpoint, no user should get access to private info-level))
    }
   	
   	
## Environment Variables
	SNS_SUBSCRIBERS (default '[]') (should be comma separated)
	SIGNALS_SERVICE_PORT (default '80')
	AWS_ACCESS_KEY_ID (default for env)
	AWS_SECRET_KEY (default for env)
	AWS_DYNAMO_STREAMS_TABLE_NAME (default 'none')
	AWS_DYNAMO_REGION (default 'none')
	SIGNALS_SERVICE_ADDRESS (default 'none')
	LOG_LEVEL (default 'DEBUG')
	AWS_LAMBDA_NOTIFY_EMAIL_ARN
	SERVICE_APIKEY (default 'none')
	MICROSERVICES_HTTPS (default 'false')
	AWS_SNS_REGION (default 'none')

	optional(initialized for testing and automatically provided by AWS):
	RDS_HOSTNAME
	RDS_PORT
	RDS_DB_NAME
	RDS_USERNAME
	RDS_PASSWORD
	
### Test Parameters
    MICROSERVICES_HTTPS=false
    AWS_LAMBDA_NOTIFY_EMAIL_ARN=arn:aws:lambda:us-west-2:525932482084:function:tbLambdaBackend-email:development
    SERVICE_APIKEY=secret
    LOG_LEVEL=DEBUG
    AWS_SECRET_ACCESS_KEY=PPwA9sj77iCb0iyaySTiQi7wVhUB13Iwv6pYUSrc
    AWS_ACCESS_KEY_ID=AKIAJPBHJ6UH233F2KKA
    SIGNALS_SERVICE_ADDRESS=cs-signals-staging.elasticbeanstalk.com
    AWS_DYNAMO_STREAMS_TABLE_NAME=streams-test
    AWS_DYNAMO_REGION=us-west-2
    AWS_SNS_REGION=us-west-2
    SNS_SUBSCRIBERS=[]
    AWS_LAMBDA_NOTIFY_EMAIL_ARN=arn:aws:lambda:us-west-2:525932482084:function:tb-notify-email

## OSX Set Up
##### Docker
Local if when runing docker: localhost

One time setup:

	boot2docker init
	VBoxManage modifyvm "boot2docker-vm" --natpf1 "postgres-port,tcp,127.0.0.1,5432,,5432" #osx specific bind (local) # set postgres to "listen on *" and "host all all 0.0.0.0/0 trust"

Setup on each shell:

	boot2docker start
	eval "$(boot2docker shellinit)"

##### Deployment
One time setup:
	
	cd docker
	eb init (then select environment etc...)

## Makefile
	-test 
	-test-u (unit)
	-test-s (service)
	-run-l (run local)
	-build (builds a artifect and place it in the docker folder and afther that build the docker container)
	-deploy-s (deploy on staging)
	-test-s-s (service tests ageins staging)
	-deploy-p (deploy in production)