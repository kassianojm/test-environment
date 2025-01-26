#!/bin/bash


source ./FUNCTIONS/utils.sh
source ./CONFIG/conf.DATA
infiniband=$1
mpi(){
echo "--------MPI--------"
for i in `cat $folder/$listofmpinodes$infiniband`;
do
	echo "---> $i"
	#ssh $user@$i "echo 'Proc to kill:'; ps ax | grep -v grep | grep -e stencyl | awk '{print \$1}';kill -9 \$(ps ax | grep -v grep | grep -e stencyl | awk '{print \$1}') 2> /dev/null"
	ssh $user@$i " echo 'Proc to kill:'; pidof stencyl; kill -9 \$(pidof stencyl) 2> /dev/null"
done
}

mq(){
echo "--------MQ--------"
for i in `cat $folder/$listofmqnodes$infiniband`;
do
	echo "---> $i"
        ssh $user@$i "echo 'Proc to kill:'; pidof message_queue_dynamic; kill -9 \$(pidof message_queue_dynamic) 2> /dev/null"
	ssh $user@$i "echo 'Proc to kill:'; pidof message_queue; kill -9 \$(pidof message_queue) 2> /dev/null"
	ssh $user@$i "echo 'Proc to kill:'; pidof message_queue_static; kill -9 \$(pidof message_queue_static) 2> /dev/null"
done

}

spark(){
	echo "--------SPARK--------"
	spark=`cat $folder/$listofsparknodes$infiniband |head -1`
	for app in StatelessSUMServer SUMServer GlobalSUMServer GlobalHistogramServer
	do
		echo "Trying to close $app ..."
		ssh $user@$spark " echo 'Proc to kill:'; ps ax | grep -v grep | grep -e "$app" | awk '{print \$1}';kill -9 \$(ps ax | grep -v grep | grep -e "$app" | awk '{print \$1}') 2> /dev/null"
	done
}

mpi
mq
spark

./FUNCTIONS/monitoring.sh stop
