#!/bin/bash
#$1: listening port for TestServer
#$2: numHosts
#optional args:
#-h <IP address of TestServer>: IP Address of TestServer
#--nopurge: deactivates purge
#-pResults <percentageRequiredResults>: percentage of received results prior to perform evaluation (e.g. 50 means 50%, 75 means 75%)
#--remoteMode: Server will run in different computers (or more than one Server in a single computer but this computer having the same internal and external IP address)
#--localMode: (default running mode. If no mode is specified it will suppose local mode) all Serves will run in the same computers
#--menu: run in menu mode
#--logResults: appends the result of the each execution to a file named as the groupId
#-path <path>: path to directory where store results (if --logResults is activated)
#--remoteTestServer: indicates that the TestServer runs in a different computer that Servers
#--noremove: deactivates the generation by simulation of operations that remove recipes

# killall java

sleep 1

LOCAL_TEST_SERVER="true"
for TOKEN in $*; do
  if [ $TOKEN = "--remoteTestServer" ]; then
    LOCAL_TEST_SERVER="false"
  fi
done
# phase 2 to 4
if [ $LOCAL_TEST_SERVER = "true" ]; then
  java -cp ../bin:../lib/* recipes_service.test.TestServer $* &
  sleep 1
fi
sleep 1
java -cp ../bin:../lib/* recipes_service.test.SendArgsToTestServer $*

wait
