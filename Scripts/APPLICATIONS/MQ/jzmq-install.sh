#!/bin/bash

#error during the mvn package
#Cannot get the revision information from the scm repository
#Find the lines that read:
#<plugin>
#<groupId>org.codehaus.mojo</groupId>
#<artifactId>buildnumber-maven-plugin</artifactId>
#<version>1.0</version>
#</plugin>
# and remove it.

sudo apt-get install autoconf autogen
unzip jzmq-master.zip

cd jzmq-master/jzmq-jni/

./autogen.sh
./configure
make
make install
cd ..
mvn package
mvn install -Dgpg.skip=true


