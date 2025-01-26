#!/bin/bash

source ./FUNCTIONS/utils.sh
source ./CONFIG/conf.DATA
folder="./NODEFILES"

echo "--------MQ--------"
aux=1
for i in `cat $folder/$listofmqnodes`;
do
	echo "---> $i"
	scp $user@$i:/tmp/*.csv LOGS/
	scp $user@$i:/tmp/MQRESOURCES.txt LOGS/
	mv LOGS/LOG.csv LOGS/LOG$aux$log.csv
	mv LOGS/MQRESOURCES.txt LOGS/MQRESOURCES$aux$log.txt
	APPLICATIONS/dmon parse LOGS/MQRESOURCES$aux$log.txt > LOGS/MQRESOURCES$aux$log.csv
	rm LOGS/MQRESOURCES$aux$log.txt
	aux=$((aux+1))
done

clean (){
	for i in `cat $folder/$listofmqnodes`;
	do
		 ssh $user@$i "rm /tmp/*.csv"
		 ssh $user@$i "rm /tmp/*.txt"
		 ssh $user@$i "rm /tmp/*.log"
	
	done
	for i in `cat $folder/$listofsparknodes`;
	do
		 ssh $user@$i "rm /tmp/*.csv"
                 ssh $user@$i "rm /tmp/*.txt"
                 ssh $user@$i "rm /tmp/*.log"
	done	
}

echo "--------SPARK--------"

spark=`cat $folder/$listofsparknodes |head -1`

aux=1
for i in `cat $folder/$listofsparknodes`;
do
#        scp $user@$i:/tmp/file* LOGS/
#	sort -g LOGS/file* > LOGS/SumFile$aux
#        rm LOGS/file*
#	ssh $user@$i "rm /tmp/file*"
	scp $user@$i:/tmp/EXECUTOR.log LOGS/
	scp $user@$i:/tmp/RESOURCES.txt LOGS/
	mv LOGS/EXECUTOR.log LOGS/EXECUTOR$aux.log
	mv LOGS/RESOURCES.txt LOGS/RESOURCES$aux.txt
	APPLICATIONS/dmon parse LOGS/RESOURCES$aux.txt >  LOGS/RESOURCES$aux.csv
	rm LOGS/RESOURCES$aux.txt
	aux=$((aux+1)) 
done


#cp -a Globalfiles.conf LOGS/
scp $user@$spark:/tmp/*.csv LOGS/
scp $user@$spark:/tmp/DRIVER.log LOGS/

clean

