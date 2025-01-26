#/!/bin/bash
source ./FUNCTIONS/utils.sh
source ./CONFIG/conf.DATA
#This function discovers the correct number of cpu cores to be used in stencyl APP (square-based)
factor=$1
SparkApp=$2
javaOP=$3
infiniband=$5
mqversion=$8


if  [ -z "$factor" ];then
	echo "set up the qos"
	echo "./start.. <value>"
	exit;
fi

getMPIcores(){
	TnodesMPI=0
	MPI=`cat $folder/$listofmpinodes |head -1`

	for i in `cat $folder/$listofmpinodes`;
	do
		#Ncores=`ssh $user@$i "nproc --all"`
		Ncores=`ssh $user@$i "grep -c ^processor /proc/cpuinfo"`   
		Tcores=$((Tcores+Ncores))
		TnodesMPI=$((TnodesMPI+1))

	done
	
#	a=$(bc <<< "scale=0; sqrt($Tcores)")
#	MPIcores=$((a*a))
	#echo "    #MPI nodes: $TnodesMPI"
	echo "Cores per node: $Ncores"
	echo "Total of cores: $Tcores"
	#echo " Usefull cores: $MPIcores"
}

#This function discovers the amount of memory and CPU of overall workers nodes
getCpuMem (){
	#select the driver node
	spark=`cat $folder/$listofsparknodes$infiniband |head -1`
	sparkadj=$(echo $spark  |cut -d "-" -f1,2)

	TnodesSpark=0
	#Getting total of memory per node in bytes
	cmd="cat /proc/meminfo |grep -e 'MemTotal:' |sed 's/ \s//g' | sed 's/\s/,/g' | cut -d ',' -f2"
	for i in `cat $folder/$listofsparknodes`;
	do
		SC=`ssh $user@$i "grep -c ^processor /proc/cpuinfo"`

		#excluding the collet from driver node
        	if [ "$i" != "$sparkadj" ];then
			NMEM=`ssh $user@$i "$cmd"`
                	TMEM=$((TMEM+NMEM))
			TnodesSpark=$((TnodesSpark+1))
			PORTS=$((PORTS+2))
		fi
	done
	#echo "#Worker nodes: $TnodesSpark"
	#echo " RAM per node: $NMEM (Kilobytes) - $((NMEM*1024/1024/1024/1024)) Gb"
	#echo " Total of RAM: $TMEM (Kilobytes) - $((TMEM*1024/1024/1024/1024)) Gb"
}

custon_static_exe1(){
#number of executor is the number of worker nodes and receivers can vary

ec=$Ncores
em=`echo $TMEM*1024/$TnodesSpark|bc`
coverhead=`echo 0.07*$em |bc`
cem=`echo $em-$coverhead|bc |cut -d "." -f1`


if [ "$mqversion" == "message_queue" ]; then
	#spark.streaming.backpressure.enabled=true
	#spark.streaming.backpressure.initialRate=$bpini 
	#bpini
	echo "BP OFF - AI-Driven"
	appspark="spark-submit --master spark://$sparkip:7077 --conf spark.ui.enabled=true $javaOP --driver-cores $SC --driver-memory $((NMEM*1024)) --num-executors $((TnodesSpark)) --executor-cores $SC --executor-memory $cem --class $SparkApp $dir/APPLICATIONS/SPARK/target/simple-project-1.0-jar-with-dependencies.jar --THRESHOLD $th --NETWORK ib0 --DATASOURCES $SOURCES_SPARK --TEMPON $msgsize --RECEIVERS $SparkReceivers --CLIENTS $Tcores --WINDOWTIME $SparkWindow --BLOCKINTERVAL $SparkBclockInterval --CONCBLOCK $SparkConcBlock --HDFS hdfs://$sparkip:9000/$SparkApp --PARAL $SparkParal"

else
	echo "BP ON"
	appspark="spark-submit --master spark://$sparkip:7077 --conf spark.streaming.backpressure.enabled=true --conf spark.streaming.backpressure.initialRate=$bpini --conf spark.ui.enabled=false $javaOP --driver-cores $SC --driver-memory $((NMEM*1024)) --num-executors $((TnodesSpark)) --executor-cores $SC --executor-memory $cem --class $SparkApp $dir/APPLICATIONS/SPARK/target/simple-project-1.0-jar-with-dependencies.jar --THRESHOLD $th --NETWORK ib0 --DATASOURCES $SOURCES_SPARK --TEMPON $msgsize --RECEIVERS $SparkReceivers --CLIENTS $Tcores --WINDOWTIME $SparkWindow --BLOCKINTERVAL $SparkBclockInterval --CONCBLOCK $SparkConcBlock --HDFS hdfs://$sparkip:9000/$SparkApp --PARAL $SparkParal"
fi


}

custon_static_exe2(){

ec=$Ncores
em=`echo $TMEM*1024/$TnodesSpark|bc`
coverhead=`echo 0.07*$em |bc`
cem=`echo $em-$coverhead|bc |cut -d "." -f1`

SplitSC=`echo $((SC/2))|bc |cut -d "." -f1`

Splitcem=`echo $((cem/2))|bc |cut -d "." -f1`


appspark="spark-submit --master spark://$sparkip:7077 --conf spark.streaming.backpressure.enabled=false --conf spark.ui.enabled=true $javaOP --driver-cores $SC --driver-memory $((NMEM*1024)) --num-executors $((TnodesSpark*2)) --executor-cores $SplitSC --executor-memory $Splitcem --class $SparkApp $dir/APPLICATIONS/SPARK/target/simple-project-1.0-jar-with-dependencies.jar --THRESHOLD $th --NETWORK ib0 --DATASOURCES $SOURCES_SPARK --TEMPON $msgsize --RECEIVERS $SparkReceivers --CLIENTS $Tcores --WINDOWTIME $SparkWindow --BLOCKINTERVAL $SparkBclockInterval --CONCBLOCK $SparkConcBlock --HDFS hdfs://$sparkip:9000/$SparkApp --PARAL $SparkParal"



}


sparkrun(){
	echo "Starting Spark"
	slave=$(echo $spark  |cut -d "-" -f1,2)
	ssh $user@$slave "hdfs dfs -rm -r /$SparkApp"
	ssh $user@$slave "hdfs dfs -mkdir -p /$SparkApp"
	sparkip=`nslookup -a $spark | grep -e "Address"| sed -n 2p | cut -d ":" -f2 | sed -e 's/^[ \t]*//'`
	custon_static_exe1
	#custon_static_exe2
	echo "Starting Spark: $appspark"

	ssh $user@$slave "$appspark" 2>&1 &

}

globalConf(){
	#the MQ receives the maximum amount of threads available for
	appMQthreads=$Ncores

	#MQ: I/O threads - by standard, just one thread is set for all sockets. Each thread handles 1Gb/s of data (in/out) and tons of connections
	#each proccess start one thread
	appMPIthreads=1

	time=1800
	msgsize=10000
	th=1000
	#to create a batch, just add 100 * msgsize
	msgbuffer=$((msgsize))
	#each executor receives one receiver - more than one incurrs in performance loss.

	#default parallelism hardcode in JavaClass
	SparkParal=1024

	SparkWindow=2000
	SparkBclockInterval=400
	bpini=2000

	Nsockets=$1
	SparkReceivers=$1
	echo $Nsockets"-----------------$1--------------------"
	qosmax=$2
	loss=$3

	#Number of conc DAGs - more than one incurrs in performance loss due Problems in internals of Spark
	SparkConcBlock=1

}


getMPIcores
getCpuMem
globalConf $4 $6 $7

# $4 - number of sockets
# $6 - qosmax
# $7 - loss
# $8 - mqversion
echo "################ $4 ###############"

./FUNCTIONS/monitoring.sh start

#-------------------------------------------------------------------
#starting MQ

dir=$(pwd)
#get the number of MQ nodes
MQNODES=`wc -l $folder/$listofmqnodes |cut -d " " -f1`
MPIport=5050
MQport=9999
#each MQ receives data in one uniq socket (MPIport) and send data for spark using one or multiple sockets based on the sending sockets number (Nsockets)
#it starts from 9999, plus 2 for additional connections


for i in `cat $folder/$listofmqnodes$infiniband`;
do
        MQip=`nslookup -a $i | grep -e "Address"| sed -n 2p | cut -d ":" -f2 | sed -e 's/^[ \t]*//'`
	echo $MQip
        [ -z "$cachingips" ] && cachingips=$MQip || cachingips="${cachingips},$MQip"
  	if [ "$MQNODES" -gt 1 ];then
		#it stores the ips of each MQ to start the spark app
       		[ -z "$SOURCES_SPARK" ] && SOURCES_SPARK=$MQip || SOURCES_SPARK="${SOURCES_SPARK},$MQip"
       		#it stores all MQ input ports and helps the mpi application to spread data equally
       		[ -z "$PORTS_MPI" ] && PORTS_MPI=$MPIport || PORTS_MPI="${PORTS_MPI},$MPIport"
	 	MQport=$((MQport+(2*Nsockets)))
        	MPIport=$((MPIport+1000))
	else

		SOURCES_SPARK=$MQip
                PORTS_MPI=$MPIport
	fi		
done

MPIport=5050
MQport=9999
count=0
for i in `cat $folder/$listofmqnodes$infiniband`;
do
	MQip=`nslookup -a $i | grep -e "Address"| sed -n 2p | cut -d ":" -f2 | sed -e 's/^[ \t]*//'`
	if [ "$MQNODES" -gt 1 ];then
		if [ "$count" -eq 0 ];then
			valueM=0
		else
			valueM=$count
		fi
		 appmq="$dir/APPLICATIONS/MQ/$mqversion $((time+10)) $MPIport $MQport $appMQthreads $msgbuffer $Nsockets $msgsize $th $factor $cachingips $valueM $qosmax $loss $SparkWindow"
		
		sleep 1
		MQport=$((MQport+(2*Nsockets)))
                MPIport=$((MPIport+1000))

	else
		SOURCES_SPARK=$MQip
		PORTS_MPI=$MPIport
		valueM=$count
		appmq="$dir/APPLICATIONS/MQ/$mqversion $((time+10)) $MPIport $MQport $appMQthreads $msgbuffer $Nsockets $msgsize $th $factor $cachingips $valueM $qosmax $loss $SparkWindow"

	fi
	count=$((count+1))
	echo "---------------"
        echo "Starting an instance of MQ in $i ($MQip)"
        echo "Summary => "$appmq
        echo "---------------"
	slave=$(echo $i  |cut -d "-" -f1,2)
	ssh $user@$slave "$appmq"  &

done
#-------------------------------------------------------------------


sleep 1
sparkrun
sleep 5
echo "Summary"
echo "Msg size:             " $msgsize
echo "threshold:            " $th
echo "Time:                 " $time
echo "Msg buffer:           " $msgbuffer
echo "MQ threads:           " $appMQthreads
echo "MQ Version	    " $mqversion
echo "MPI threads:          " $appMPIthreads
echo "Spark Block Interval: " $SparkBclockInterval
echo "# Spark Receivers:    " $SparkReceivers
echo "Spark Parallelism:    " $SparkParal
echo "Window:               " $SparkWindow
echo "App:                  " $SparkApp
echo "Concurrent Blocks: " $SparkConcBlock
echo "Square core-based overall allocated nodes:" $MPIcores



#--mca btl_base_warn_component_unused 0 
#--mca btl_tcp_if_exclude eno1 --mca btl_openib_if_include ib0
#--mca btl_openib_cpc_include rdmacm


#appc="mpirun -np $MPIcores --mca btl_openib_pkey 0x8108 --mca btl openib,self --mca btl_tcp_if_exclude eno1 --mca btl_openib_if_include ib0 --hostfile $dir/$folder/$listofmpinodes $dir/APPLICATIONS/MPI/stencyl/stencyl $time $msgsize $SOURCES_SPARK $PORTS_MPI $appMPIthreads $MQNODES -1 0"


appc="mpirun -np $Tcores  --hostfile $dir/$folder/$listofmpinodes$infiniband $dir/APPLICATIONS/MPI/stencyl/stencyl $time $msgsize $SOURCES_SPARK $PORTS_MPI $appMPIthreads $MQNODES -1 0"



echo "###########Starting MPI: $appc"

ssh $user@$MPI "$appc" 2>&1 &



auxt=0

time=$((time))
while true; do sleep 1; echo $auxt; if [ "$auxt" -gt $time  ]; then echo "done"; ./kill.sh; ./FUNCTIONS/monitoring.sh stop; exit 0;fi; auxt=$((auxt+1));   done 
