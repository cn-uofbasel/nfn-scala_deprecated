#! /bin/bash


# $1 = DPU address
if [ -z "$1" ]; then 
	DPU_Address='127.0.0.1'
else 
	DPU_Address=$1
fi

cd ..

sbt 'runMain filterAccess.runnables.SecureServiceStarter -m /tmp/mgmtDCU -o 6133 -p 6233 -d -s ndn2013 -t ECU' & 

sleep 20

FACEID_DPU=`ccn-lite-ctrl -x /tmp/mgmtDCU newUDPface any $DPU_Address 6131 | ccn-lite-ccnb2xml | grep FACEID | sed s/[^0-9]//g`

ccn-lite-ctrl -x /tmp/mgmtDCU prefixreg /own/machine $FACEID_DPU ndn2013 | ccn-lite-ccnb2xml





