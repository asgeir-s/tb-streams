# Streams Microservice
Microservice used in the coinsignals project.

## Interface
	POST: /streams {	
    	"id": "coinbase-account-id",
        "exchange": "bitstamp",
        "currencyPair": "btcUSD",
        "payoutAddress": "publishers-bitcoin-address",
        "subscriptionPriceUSD": 5
    }
    
    POST: /streams/'streamID'/signals {
    	"timestamp": 1432122282747,
    	"price": 200.453,
    	"change": 0,
    	"id": 1,
    	"value": 100,
    	"signal": 1
    }
    
    GET:  /streams/'streamID' returns Stream info and stats
    
    GET: /streams returns stream info for all streams
   	

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