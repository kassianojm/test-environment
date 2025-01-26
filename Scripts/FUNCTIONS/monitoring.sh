#/!/bin/bash

prepare(){
 for i in `cat NODEFILES/nodefile `; do scp APPLICATIONS/dmon $i:/tmp/ ;done
}


#to stop or to start
Spark(){
 for i in `cat NODEFILES/sparknodes `; do ssh $i "/tmp/dmon $1 /tmp/RESOURCES.txt" ;done
}

MQ(){
 for i in `cat NODEFILES/mqnodes `; do ssh $i "/tmp/dmon $1 /tmp/MQRESOURCES.txt" ;done
}

MPI(){
 for i in `cat NODEFILES/mpinodes `; do ssh $i "/tmp/dmon $1 /tmp/MPIRESOURCES.txt" ;done
}


if [[  $1 == "stop" ]] || [[ $1 == "start" ]] || [[ $1 == "prepare" ]];
then
	echo "commmand $1 selected..."


	if [[ $1 == "prepare" ]];
	then
		prepare
		exit 0
	fi	
	MQ $1
	Spark $1
#	MPI $1

else

	echo "Use: ./monitoring < start|stop|prepare >"
        exit 0

fi


