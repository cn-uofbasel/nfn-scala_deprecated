#! /bin/bash


# $1 = DSU address
# $2 = DPU address
if [ -z "$1" ]; then 
	DSU_Address='127.0.0.1'
else 
	DSU_Address=$1
fi
if [ -z "$2" ]; then 
	DPU_Address='127.0.0.1'
else 
	DPU_Address=$2
fi

cd ../nfn-scala

sbt 'runMain filterAccess.runnables.SecureServiceStarter -m /tmp/mgmtDCU -o 6132 -p 6232 -d -s ndn2013 -t DCU' & 

sleep 10

FACEID_DSU=`ccn-lite-ctrl -x /tmp/mgmtDCU newUDPface any $DSU_Address 6130 | ccn-lite-ccnb2xml | grep FACEID | sed s/[^0-9]//g` 
FACEID_DPU=`ccn-lite-ctrl -x /tmp/mgmtDCU newUDPface any $DPU_Address 6131 | ccn-lite-ccnb2xml | grep FACEID | sed s/[^0-9]//g`

ccn-lite-ctrl -x /tmp/mgmtDCU prefixreg /serviceprovider/storage $FACEID_DSU ndn2013 | ccn-lite-ccnb2xml
ccn-lite-ctrl -x /tmp/mgmtDCU prefixreg /own/domain $FACEID_DPU ndn2013 | ccn-lite-ccnb2xml





