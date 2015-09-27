#! /bin/bash

cd ..

sbt 'runMain filterAccess.runnables.SecureServiceStarter -m /tmp/mgmtDSU -o 6130 -p 6230 -d -s ndn2013 -t DSU'
