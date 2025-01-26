#!/bin/bash

for i in data_StatelessSUMServer data_GlobalSUMServer data_GlobalHistogramServer data_SUMServer batch_infos.csv
do
	if [ -d $i ] || [ -f $i ]
	then
		rm -rf $i
		echo "$i was removed..."
	else
		echo " Nothing to do for $i..."
	fi
done

mvn clean

