#! /bin/bash

# $1 = DSU address
# $2 = DCU address

if [ -z "$1" ]; then 
	DSU_Address='127.0.0.1'
else 
	DSU_Address=$1
fi
if [ -z "$2" ]; then 
	DCU_Address='127.0.0.1'
else 
	DCU_Address=$2
fi

cd ..

sbt 'runMain filterAccess.runnables.SecureServiceStarter -m /tmp/mgmtDPU -o 6131 -p 6231 -d -s ndn2013 -t DPU -r' & 

sleep 10

FACEID_DSU=`ccn-lite-ctrl -x /tmp/mgmtDPU newUDPface any $DSU_Address 6130 | ccn-lite-ccnb2xml | grep FACEID | sed s/[^0-9]//g`
FACEID_DCU=`ccn-lite-ctrl -x /tmp/mgmtDPU newUDPface any $DCU_Address 6132 | ccn-lite-ccnb2xml | grep FACEID | sed s/[^0-9]//g`

ccn-lite-ctrl -x /tmp/mgmtDPU prefixreg /serviceprovider/storage $FACEID_DSU ndn2013 | ccn-lite-ccnb2xml
ccn-lite-ctrl -x /tmp/mgmtDPU prefixreg /serviceprovider/filtering $FACEID_DCU ndn2013 | ccn-lite-ccnb2xml

