#! /bin/bash
data=$1
pkey=$2

ccn-lite-simplenfn -u 127.0.0.1/9000 "call 3 /nfn/node0/filterAccess_ndncomm15_services_Echo '$data' '$pkey'" | ccn-lite-pktdump -f 2
