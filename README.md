# Streams Microservice
Microservice used in the coinsignals project. 

## Interface
	POST: /streams {
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
   	
   	
## Environment Variables
	SNS_SUBSCRIBERS (default '[]') (should be comma separated)
	STREAMS_SERVICE_ADDRESS (default 'none')
	SIGNALS_SERVICE_PORT (default '80')
	AWS_ACCESS_KEY_ID (default 'none')
	AWS_SECRET_KEY (default 'none')
	DYNAMO_STREAMS_TABLE_NAME (default 'none')
	DYNAMO_STREAMS_TABLE_REGION (default 'none')
	SIGNALS_SERVICE_ADDRESS (default 'none')
	LOG_LEVEL (default 'DEBUG')
	
	optional(initialized for testing and automatically provided by AWS):
	RDS_HOSTNAME
	RDS_PORT
	RDS_DB_NAME
	RDS_USERNAME
	RDS_PASSWORD
	
### Test Parameters
	LOG_LEVEL=DEBUG
	AWS_SECRET_KEY=qjKCyXmcnlFnJhij3jIcMtcGNAKSFNYkTvHgGA5C
	DYNAMO_STREAMS_TABLE_NAME=streams-test
	AWS_ACCESS_KEY_ID=AKIAJNHQ6UIBTNXYYPFA
	SIGNALS_SERVICE_PORT=80
	SIGNALS_SERVICE_ADDRESS=cs-signals-staging.elasticbeanstalk.com
	DYNAMO_STREAMS_TABLE_REGION=us-west-2

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