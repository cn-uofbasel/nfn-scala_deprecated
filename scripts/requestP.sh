#! /bin/bash
arg=$1
ccn-lite-simplenfn -u 127.0.0.1/9000 "call 2 /nfn/node0/filterAccess_ndncomm15_services_EchoP '$arg'" | ccn-lite-pktdump -f 2
