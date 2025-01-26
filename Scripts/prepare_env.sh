#/!/bin/bash

#Load conf files
source ./FUNCTIONS/utils.sh
source ./CONFIG/conf.DATA

#Command line parameters
MPI=$1
MQ=$2
spark=$3
interface=$4
infiniband=$5

#Obtainining the current path
dir=$(pwd)

aux=`wc -l  $folder/$filename |cut -d " " -f1`
echo "Available nodes: $aux"

#check current request
if [[ "$MPI" = "" || "$MQ" = "" ||  "$spark" = "" || "$MPI" -eq 0 || "$MQ" -eq 0 ||  "$spark" -lt 2 || "$MPI" -gt "$aux" || "$spark" -gt "$aux" || "$MQ" -gt "$aux" || "$((MPI+spark+MQ))" -gt "$aux" ]];
then
	echo "Please, set the numer (non 0) of nodes for MPI, MQ and Spark (more than 1 node for)..."
	echo "E.g, ./prescript.sh <#nodes_mpi> <#nodes_mq> <#nodes_spark> =>>>>>> ./prescript 2 2 3 "
	exit 1
fi

#load the available nodes and set the current environment
number_nodes=`cat $folder/$filename | wc -l`
target=$((MPI+MQ+spark))

echo "Required: MPI: $MPI MQ: $MQ Spark: $spark -> Total: $target, #G5k available nodes: $number_nodes" 

#creating the required list of nodes for mpi mq and spark!


echo "Creating nodefiles ..."
echo "Mpi..."
[ -f $folder/$listofmpinodes ] && rm $folder/$listofmpinodes* || echo \" file $listofmpinodes was not found! Creating...\" 
echo "Done"

echo "MQ..."
[ -f $folder/$listofmqnodes ] && rm $folder/$listofmqnodes* || echo \" file $listofmqnodes was not found! Creating...\" 
echo "Done"

echo "Spark..."
[ -f $folder/$listofsparknodes ] && rm $folder/$listofsparknodes* || echo \" file $listofsparknodes was not found! Creating...\" 
echo "Done"

echo "--> Allocated nodes for mpi <--"
sed -n " 1,$MPI p" $folder/$filename >> $folder/$listofmpinodes
cat $folder/$listofmpinodes

echo "--> Allocated nodes for mq <--"
sed -n "$((MPI+1)),$((MPI+MQ)) p" $folder/$filename >> $folder/$listofmqnodes
cat $folder/$listofmqnodes 

echo "--> Allocated nodes for spark <--"
sed -n "$((MPI+MQ+1)),$((MPI+MQ+spark)) p" $folder/$filename >> $folder/$listofsparknodes
cat $folder/$listofsparknodes


echo "inf - " $infiniband
echo "card - " $interface

if [ ! -z $infiniband ];then
   cd FUNCTIONS/
   ./netAdjust.sh add $interface
   for i in $(cat ../$folder/$listofmpinodes)
   do 
	echo $i"-ib0" >> ../$folder/$listofmpinodes"1"

   done

   for i in $(cat ../$folder/$listofmqnodes)
   do 
        echo $i"-ib0" >> ../$folder/$listofmqnodes"1"

   done

   for i in $(cat ../$folder/$listofsparknodes)
   do 
        echo $i"-ib0" >> ../$folder/$listofsparknodes"1"

  done
  cd ..
else
	cd FUNCTIONS/
	./netAdjust.sh del $interface
	echo "No Infiniband"
	cd ..

fi



cd SparkInstall
./clusterSpark.sh -s $infiniband

cd ..
./FUNCTIONS/monitoring.sh prepare

