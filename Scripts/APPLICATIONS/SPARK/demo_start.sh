#!/bin/bash
hadoop fs -rm -r /StatelessSUMServer 
hdfs dfs -mkdir -p /StatelessSUMServer

spark-submit --master spark://127.0.0.1:7077 --class StatelessSUMServer target/simple-project-1.0-jar-with-dependencies.jar --THRESHOLD 40 --NETWORK wlp2s0 --DATASOURCES 127.0.0.1 --TEMPON $(((10000-8)/4)) -RECEIVERS 1 --CLIENTS 4 --WINDOWTIME 2000 --BLOCKINTERVAL 400 --CONCBLOCK 1 --HDFS hdfs://127.0.0.1:9000/StatelessSUMServer --PARAL 8


#spark-submit --master spark://127.0.0.1:7077 --class StatelessSUMServer target/simple-project-1.0-jar-with-dependencies.jar --PORT 4040 --NETWORK wlp2s0 --DATASOURCES 127.0.0.1 --TEMPON $(((30000-8)/4)) -RECEIVERS 4 --CLIENTS 4 --WINDOWTIME 1000 --BLOCKINTERVAL 200 --CONCBLOCK 1 --HDFS hdfs://127.0.0.1:9000/StatelessSUMServer --PARAL 12

