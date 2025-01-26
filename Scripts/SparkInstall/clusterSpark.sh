#/!/bin/bash
#
#	title           :deploy-spark.sh
#	description     :Install Spark Cluster x.x - standalone and streaming mode 
#	author			:Kassiano Jose Matteussi "kjmatteussi@inf.ufrgs.br"
#	date            :Tue Jul 16 18:22:29 CEST 2019
#	version         :1.0
#	usage			:deploy-spark.sh


#### Global Parameters and Variables ###
source ../FUNCTIONS/utils.sh
source ../CONFIG/conf.DATA
dir="../NODEFILES"

D_NODE="/tmp/data/dataNode"
N_NODE="/tmp/data/nameNode"




header(){
	infininband=$aux
	master=`cat $dir/$listofsparknodes$infiniband | head -1`
	masterip=`nslookup -a $master | grep -e "Address"| sed -n 2p | cut -d ":" -f2 | sed -e 's/^[ \t]*//'`
}

##### ##### ##### ##### ##### ##### ##### 
start(){
	infiniband=$aux
	header $infiniband
	echo "Deploying the cluster..."
	for i in `cat $dir/$listofsparknodes$infiniband`; 
    	do
		if [ ! -z $infiniband ]; then
		    if [ $infiniband == 1 ]; then
			    echo $master--
			if [ $i == $master ] ;then
				echo "c $i $master"
				a=$(echo $i  |cut -d "-" -f1,2)

		                ssh $user@$a "mkdir -p /tmp/spark-events"	
			fi
		    fi
		else
			
			a=$master
			ssh $user@$i "mkdir -p /tmp/spark-events"
		fi
		source $install_path/bashrc 
		if [ "$i" == "$master" ]; then
			#creating and copying conf files to the master node
			echo "export SPARK_MASTER_HOST='$master'" 							> Conf_Files/sp/spark-env.sh
			echo "export SPARK_WORKER_DIR=/tmp/work"							>> Conf_Files/sp/spark-env.sh
			echo "export SPARK_LOG_DIR=/tmp/logs"                                                          	>> Conf_Files/sp/spark-env.sh
			echo "export SPARK_LOCAL_DIRS=/tmp/scratch"							>> Conf_Files/sp/spark-env.sh
			echo "export HADOOP_LOG_DIR=/tmp/hlogs"								> Conf_Files/had/hadoop-env.sh
			#echo "spark.eventLog.enabled        	true"  							> Conf_Files/sp/spark-defaults.conf 
                        #echo "spark.eventLog.enabled            false"                                                   > Conf_Files/sp/spark-defaults.conf
			#echo "spark.eventLog.dir 		hdfs://$masterip:9000/tmp/spark-event-logs" 		>> Conf_Files/sp/spark-defaults.conf
			#echo "spark.history.fs.logDirectory     hdfs://$masterip:9000/tmp/spark-event-logs" 		>> Conf_Files/sp/spark-defaults.conf
			#echo "spark.history.ui.port             18080"							>> Conf_Files/sp/spark-defaults.conf
			#echo "spark.local.dir			/tmp/spark"						>> Conf_Files/sp/spark-defaults.conf
			#echo "spark.history.fs.update.interval  1s"							>> Conf_Files/sp/spark-defaults.conf
			#echo "spark.history.provider            org.apache.spark.deploy.history.FsHistoryProvider"	>> Conf_Files/sp/spark-defaults.conf
			echo "spark.driver.defaultJavaOptions   -XX:+UseG1GC"                                       >> Conf_Files/sp/spark-defaults.conf
			echo "spark.executor.defaultJavaOptions -XX:+UseG1GC"                                       >> Conf_Files/sp/spark-defaults.conf
			#echo "spark.executor.extraJavaOptions   \"-XX:+UseG1GC -XX:ConcGCThreads=32\"" 				  >> Conf_Files/sp/spark-defaults.conf
			#echo "spark.executor.extraJavaOptions   \" -XX:ParallelGCThreads=32\"" 			  >> Conf_Files/sp/spark-defaults.conf
			#echo "spark.executor.extraJavaOptions   \" -XX:ConcGCThreads=32 \"" 					>> Conf_Files/sp/spark-defaults.conf
			#echo "spark.executor.extraJavaOption \"-XX:InitiatingHeapOccupancyPercent=20\""                 >> Conf_Files/sp/spark-defaults.conf
			#echo "spark.executor.extraJavaOption \"-XX:InitiatingHeapOccupancyPercent=90\""      		>> Conf_Files/sp/spark-defaults.conf
			#echo "spark.executor.extraJavaOptions -XX:+UseParallelOldGC"                             >> Conf_Files/sp/spark-defaults.conf 
			#echo "spark.driver.defaultJavaOptions -XX:+UseParallelOldGC"                             >> Conf_Files/sp/spark-defaults.conf 
		else
			echo "Add as Worker in Spark and Hadoop" $i
			echo $i >> Conf_Files/sp/slaves
			echo $i >> Conf_Files/had/workers
		fi
	done

	datanode=$(echo "$D_NODE" | sed 's/\//\\\//g')
	namenode=$(echo "$N_NODE" | sed 's/\//\\\//g')

	sed  "s/\${NAMENODE}/${namenode}/g; s/\${DATANODE}/${datanode}/g; s/\${CARD}/${interface}/g" Conf_Files/hdfs-site.xml > Conf_Files/had/hdfs-site.xml
	sed  s/\${HDFS_MASTER}/${masterip}/g Conf_Files/core-site.xml > Conf_Files/had/core-site.xml

	echo "starting the cluster..."

	#logs: ssh $user@$i "echo 'log4j.logger.org.apache.spark.deploy.history=INFO' >> $install_path/spark/conf/log4j.properties"
	cp -r Conf_Files/sp/* $install_path/spark/conf/
        cp -r Conf_Files/had/* $install_path/hadoop/etc/hadoop/

	echo "Starting SPARK"
	echo "start-master.sh -h $master - $a "
	ssh $user@$a  "start-master.sh -h $master; sleep 1 "
	#ssh $user@$master  'mkdir -p /tmp/spark-events; start-master.sh; sleep 1; start-slaves.sh; start-history-server.sh '


	for i in `cat $dir/$listofsparknodes$infiniband`;
        do
		if [ "$i" != "$master" ]; then
        #		if [ "$shuffle" == "shuffle" ]; then
	#			echo $shuffle
	#			#ssh $user@$i  'start-shuffle-service.sh '
	#		fi
			slave=$(echo $i  |cut -d "-" -f1,2)
			echo "start-slave.sh  spark://$master:7077 -h $i" 
			ssh $user@$slave  "start-slave.sh  spark://$master:7077 -h $i" 
		fi
	done

	echo "Starting HDFS"
	ssh $user@$a "hdfs namenode -format"
	sleep 1
	ssh $user@$a "start-dfs.sh"
        #ssh $user@$master 'hadoop fs -mkdir /tmp/spark-event-logs;hadoop fs -chmod 777 /tmp/spark-event-logs'
	echo "Done!"
}

stop()
{
	
        master=`cat $dir/$listofsparknodes | head -1`
	echo "Stopping services and server side cleaning ..."
#	stop-history-server.sh
	source $install_path/bashrc
	ssh $user@$master  " stop-slaves.sh; stop-dfs.sh; stop-master.sh; echo \"Done! \""
	for i in `cat $dir/$listofsparknodes`;
        do
                if [ "$i" != "$master" ]; then
			echo "cleaning tmp folder of worker nodes!!"
	                #ssh $user@$i  "hadoop fs -rm -r /tmp/spark-event-logs"
		        ssh $user@$i  "rm -rf /tmp/*"
		fi
        done
	#ssh $user@$master  "hadoop fs -rm -r /tmp/spark-event-logs"
	echo "CLeaning tmp folder of master node.."
        ssh $user@$master  "rm -rf /tmp/*"

	#ssh $user@$master  'stop-shuffle-service.sh '
	echo "Adjusting local folders!"
        [ -e Conf_Files/had/workers ] &&  > Conf_Files/had/workers || echo "File workers was not found in folder Conf_files!!"
	[ -e Conf_Files/sp/slaves ] &&   > Conf_Files/sp/slaves || echo "File slaves was not found in folder Conf_files!!"
	[ -e Conf_Files/sp/spark-defaults.conf ] &&  > Conf_Files/sp/spark-defaults.conf || echo "File spark-defaults.conf was not found in folder Conf_files!!"
}



GUI()
{
	echo "WebUI Spark          --> http://$masterip:8080"
	echo "AppUI Spark          --> http://$masterip:4040 (only during job execution)"
	echo "History Server SPARK --> http://$masterip:18080 (run the program with the history log option: --conf spark.eventLog.enabled=true )"
	echo "History Server (Json)--> http://$masterip:18080/api/v1/applications/(stages|jobs|..)"
	echo "WebUI HDFS           --> http://$masterip:9870"
	echo "URI HDFS             --> hdfs://$masterip:9000/<folder_path>"
	echo "Example: spark-submit --master spark://$masterip:7077 --class StatelessSUMServer target/simple-project-1.0-jar-with-dependencies.jar --PORT 9999 --NETWORK eth0 --DATASOURCES <ips> --TEMPON 10000 --RECEIVERS 1 --CLIENTS 1 --WINDOWTIME 1000 --BLOCKINTERVAL 200 --CONCBLOCK 1 --HDFS  hdfs://<ip>:9000/<folder> --PARAL 8"
	echo "Example (local mode): ssh $user@$master 'spark-submit --master spark://$masterip:7077 $install_path/spark/examples/src/main/python/pi.py 200'"	
}


help()
{
cat << EOF

This script (cluster-spark.sh) uses spark-2.4.3-bin-hadoop2.7.tgz, scala-2.13.0.tgz and, jdk-8u131-linux-x64.tar.gz
Just run, enjoy!
 
USAGE:  ./cluster-spark.sh [option]
 
OPTIONS:
   -h,  --help            Time to coffe! 
   -s,  --start           Deploy Spark cluster
   -f,  --start-shuffle   Deploy Spark cluster with shuffle service
   -p,  --stop            Stop all Spark components and Workers, removing all associated data.
   
EXAMPLES: 
     ./cluster-spark.sh -s
     ./cluster-spark.sh --start
             
EOF
}

while true;
do
  case "$1" in

    -h|--help)
      help
      exit 0
      ;;
    -s|--start)
	nodefile_verification $listofsparknodes $dir
	aux=$2
	stop
	echo $aux 
	start $aux
	GUI
      break
      ;;
      -f|--start-shuffle)
        nodefile_verification $listofsparknodes $dir
        stop 
        header
	shuffle="shuffle"
        start $shuffle
        GUI
      break
      ;;
    -p|--stop)
	nodefile_verification $listofsparknodes $dir
	header $aux
	stop
      break
      ;;
    --|-|*)
      help
      break
      ;;
  esac
done








