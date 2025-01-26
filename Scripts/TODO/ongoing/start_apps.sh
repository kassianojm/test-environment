#!/bin/bash

source ./FUNCTIONS/utils.sh
source ./CONFIG/conf.DATA
folder="./NODEFILES"

cont=1
DATE=`date +%Y-%m-%d:%H:%M:%S`
auxD=$DATE
sheet=$1



default_values (){
	appMQthreads=32
	appMPIthreads=1
	threshold=3000000000
	MPIbuffer=10000
	ZMQbuffer=10000
}

getMPIcores(){
        TnodesMPI=0
        MPI=`cat $folder/$listofmpinodes |head -1`

        for i in `cat $folder/$listofmpinodes`;
        do
                Ncores=`ssh $user@$i "nproc --all"`
                Tcores=$((Tcores+Ncores))
                TnodesMPI=$((TnodesMPI+1))

        done
        a=$(bc <<< "scale=0; sqrt($Tcores)")
        MPIcores=$((a*a))
}

#This function discovers the amount of memory and CPU of overall workers nodes
getCpuMem (){
        #select the driver node
        spark=`cat $folder/$listofsparknodes |head -1`
        TnodesSpark=0
        #Getting total of memory per node in bytes
        cmd="cat /proc/meminfo |grep -e 'MemTotal:' |sed 's/ \s//g' | sed 's/\s/,/g' | cut -d ',' -f2"
        for i in `cat $folder/$listofsparknodes`;
        do
                #excluding the collet from driver node
                if [ "$i" != "$spark" ];then
                        NMEM=`ssh $user@$i "$cmd"`
                        TMEM=$((TMEM+NMEM))
                        TnodesSpark=$((TnodesSpark+1))
                        PORTS=$((PORTS+2))
                fi
        done
}

custon_static_exe1(){
	#number of executor is the number of worker nodes and receivers can vary
	#--conf spark.network.timeout 10000000
	#--conf spark.driver.maxResultSize=4g
	#--spark.scheduler.listenerbus.eventqueue.capacity=20000
	ec=$Ncores
	em=`echo $TMEM*1024/$Ncores|bc`
	coverhead=`echo 0.07*$em |bc`
	cem=`echo $em-$coverhead|bc |cut -d "." -f1`
	appspark="spark-submit --master spark://$sparkip:7077 --driver-memory $((NMEM*1024)) --driver-cores $Ncores --num-executors $TnodesSpark --executor-cores $ec --executor-memory $cem --conf spark.streaming.receiver.maxRate=-1 --conf spark.driver.maxResultSize=4g --class $SparkApp ~/SPARK/target/simple-project-1.0-jar-with-dependencies.jar --PORT $TnodesSpark --NETWORK eth0 --DATASOURCES $SOURCES_SPARK --TEMPON $(((msgsize-8)/4)) --RECEIVERS $SparkReceivers --CLIENTS $MPIcores --WINDOWTIME $SparkWindow --BLOCKINTERVAL $SparkBclockInterval --CONCBLOCK $SparkConcBlock --HDFS hdfs://$sparkip:9000/$SparkApp --PARAL $SparkParal"

}

if [ -z "$1" ]; then
                echo "Load experiment file!"
		
                exit
fi


for line in `sed 1d $sheet`
do
        if [ -z "$line" ]; then
                echo "No more tests to load in $sheet"
	        exit
        fi
	rep=$(echo $line |cut -d "," -f12)
	for (( it=1; it <= $rep; it++ ))
	do
		time=$(echo $line |cut -d "," -f1)
		msgsize=$(echo $line |cut -d "," -f2)
		ACK=$(echo $line |cut -d "," -f3)
		Nsockets=$(echo $line |cut -d "," -f4)
		msgbuffer=$((ACK*msgsize))
		SparkReceivers=$(echo $line |cut -d "," -f5)
		SparkParal=$(echo $line |cut -d "," -f6)
		SparkWindow=$(echo $line |cut -d "," -f7)
		SparkBclockInterval=$(echo $line |cut -d "," -f8)
		SparkConcBlock=$(echo $line |cut -d "," -f9)
		SparkApp=$(echo $line |cut -d "," -f10)
		wss=$(echo $line |cut -d "," -f11)
		logname="${auxD}_${cont}_${rep}_${time}_${msgsize}_${ACK}_${msgbuffer}_${Nsockets}_${SparkReceivers}_${SparkParal}_${SparkWindow}_${SparkBclockInterval}_${SparkConcBlock}_${SparkApp}_message_queue${wss}.c"
		default_values
		echo $logname

		getMPIcores
		getCpuMem
		#starting MQ
		#get the number of MQ nodes
		MQNODES=`wc -l $folder/$listofmqnodes |cut -d " " -f1`
		MPIport=5050
		MQport=9999
		#each MQ receives data in one uniq socket (MPIport) and send data for spark using one or multiple sockets based on the sending sockets number (Nsockets)
		#it starts from 9999, plus 2 for additional connections
		./kill.sh
		for i in `cat $folder/$listofmqnodes`;
		do
        		MQip=`nslookup -a $i | grep -e "Address"| sed -n 2p | cut -d ":" -f2 | sed -e 's/^[ \t]*//'`
        		if [ "$MQNODES" -gt 1 ];then
                		#it stores the ips of each MQ to start the spark app
                		[ -z "$SOURCES_SPARK" ] && SOURCES_SPARK=$MQip || SOURCES_SPARK="${SOURCES_SPARK},$MQip"
                		#it stores all MQ input ports and helps the mpi application to spread data equally
                		[ -z "$PORTS_MPI" ] && PORTS_MPI=$MPIport || PORTS_MPI="${PORTS_MPI},$MPIport"	
                		appmq="./MQ/message_queue$wss $time $MPIport 9999 $appMQthreads $msgbuffer $ZMQbuffer $Nsocket $msgsize $threshold"
		                MQport=$((MQport+(2*Nsockets)))
                		MPIport=$((MPIport+1000))
        		else
               			SOURCES_SPARK=$MQip
                		PORTS_MPI=$MPIport
		                appmq="./MQ/message_queue$wss $time $MPIport $MQport $appMQthreads $msgbuffer $ZMQbuffer $Nsockets $msgsize $threshold"
        		fi
       			echo "---------------"
        		echo "Starting an instance of MQ in $i ($MQip)"
        		echo "Summary => "$appmq
        		echo "---------------"
        		ssh $user@$i "$appmq" 2>&1 &
		done
		echo "Starting Spark"
		ssh $user@$spark "hadoop fs -rm -r /$SparkApp"
		ssh $user@$spark "hdfs dfs -mkdir -p /$SparkApp"
		sparkip=`nslookup -a $spark | grep -e "Address"| sed -n 2p | cut -d ":" -f2 | sed -e 's/^[ \t]*//'`
		custon_static_exe1
		sleep 1
		echo "Startgin Spark: $appspark"
		ssh $user@$spark "$appspark" 2>&1 &
		sleep 1
		echo "Starting MPI: $appc"
		appc="mpirun --allow-run-as-root -np $MPIcores --hostfile ~/$listofmpinodes ./MPI/stencyl/stencyl $time $msgsize $SOURCES_SPARK $PORTS_MPI $appMPIthreads $MQNODES $MPIbuffer"
		ssh $user@$MPI "$appc" 2>&1 &
		cont=$((cont+1))
	
		auxt=0

		time=$((time+3))
		while [ $auxt -lt $time ]
		do 
			sleep 1
			echo $auxt 
			auxt=$((auxt+1))
		done
		#./logs.sh $logname
	done
done 


