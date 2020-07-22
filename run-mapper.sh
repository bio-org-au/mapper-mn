#!/bin/bash

#This just sets up the environment variables and runs the mapper
#Note these should match your data setup in the shard
#You need a config file

#remove the next line once set up
echo "You need to set the environment variables and edit this file." ; exit 1

export MICRONAUT_SERVER_PORT=8080
export DATABASE_USER=nsl-db-user
export DATABASE_PASSWORD=Notogrammitis
export DATABASE_URL=postgresql://127.0.0.1:5432/nsl
export BASE_DOMAIN=biodiversity.local.fixme
export DOMAIN_PREFIX=test        #can be blank
export DOMAIN_PREFIX_DASH=test-  #can be blank
CONFIG_FILE_LOCATION=$HOME/.nsl/nsl-mapper-config-mn.groovy

if [ ! -f mapper-mn.jar ]; then
  echo "you need mapper-mn.jar in this directory. e.g. 'cp build/libs/mapper-mn-*-all.jar mapper-mn.jar'"
  exit 1
fi

java -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Dcom.sun.management.jmxremote -noverify ${JAVA_OPTS} \
-Dmicronaut.config.files=$CONFIG_FILE_LOCATION \
-jar mapper-mn.jar