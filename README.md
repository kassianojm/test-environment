# Project Overview

This repository contains scripts and configurations for managing and executing various distributed computing applications and frameworks, including **Spark**, **MPI**, **Message Queues**, and more. It also provides tools for environment preparation, monitoring, and cluster discovery.

---

## **Directory Structure**

Scripts/
├── APPLICATIONS
│   ├── dmon
│   ├── MPI
│   │   └── stencyl
│   │       ├── stencyl.c
│   ├── MQ
│   │   ├── message_queue.c
│   │   ├── message_queue_dynamic.c
│   │   ├── message_queue.h
│   │   ├── message_queue_static.c
│   └── SPARK
│       ├── src
│       │   ├── main
│       │      └── java
│       │          ├── GlobalHistogramServer.java
│       │          ├── GlobalSUMServer.java
│       │          ├── HistogramServer.java
|	|	   ├── SumServer.java
│       │          ├── StatelessSUMServer.java
├── CONFIG
│   └── conf.DATA
├── discovery_cluster.sh
├── Executionloop.sh
├── FUNCTIONS
│   ├── monitoring.sh
│   ├── netAdjust.sh
│   ├── ssh_config
│   └── utils.sh
├── prepare_env.sh
├── README.md
├── SparkInstall
│   ├── clusterSpark.sh
│   └── Conf_Files
│       ├── core-site.xml
│       ├── had
│       │   ├── core-site.xml
│       │   ├── hadoop-env.sh
│       │   ├── hdfs-site.xml
│       │   └── workers
│       ├── hdfs-site.xml
│       └── sp
│           ├── slaves
│           ├── spark-defaults.conf
│           ├── spark-env.sh
│           └── spark-env.sh.template
├── start_apps.sh

### **Scripts/**
Contains all application scripts, configuration files, and utility functions.  

#### **APPLICATIONS/**  
- **MPI/**:  
  - **stencyl/**: MPI-based stencil computation implementation.  
    - `stencyl.c`: Source code for the stencil application.  
- **MQ/**:  
  - **message_queue.c**: Standard broker application .
  - **message_queue_dynamic.c**: Heuristic for Dynamic Global Cache Solution.  
  - **message_queue_static.c**: Heuristic for Static Global Cache Solution.  
- **SPARK/**:  
  - **src/main/java/**: Contains Java implementations for Spark Streaming using varied levels of memory comsumption:  
    - `GlobalHistogramServer.java`: Highest memory usage.
    -- Purpose: Distributes data into fixed 10-bin histogram.
    -- Memory: Fixed-size arrays per (rank, step).
    -- State: mapWithState with 20s timeout.
    - `GlobalSUMServer.java`: Moderate memory usage.
    -- Purpose: Two-level mean calculation (local then global).
    -- Memory: Numeric values per (rank, step), dual aggregation state.
    -- State: mapWithState with 20s timeout.
    - `SumServer.java`: Lower memory.
    -- Purpose: Single-level sum calculation.
    -- Memory: Minimal state per (rank, step).
    -- State: mapWithState with 20s timeout.
    - `StatelessSUMServer.java`: Computing without states.  


#### **FUNCTIONS/**  
- **monitoring.sh**: Start monitoring.  
- **netAdjust.sh**: Network adjustment - Change network interface for application processing.  
- **ssh_config**: Allow localhost.  
- **utils.sh**: Utility functions for script reusability.  

#### Other Scripts  
- `discovery_cluster.sh`: Discovers, initializes and install packages into cluster nodes.  
- `Executionloop.sh`: Automates testing loop.  
- `prepare_env.sh`: Prepares the execution environment.  
- `start_apps.sh`: Starts the application pipeline on top of the cluster.  

---

### **SparkInstall/**  
Contains Spark installation and configuration files.  

- **clusterSpark.sh**: Script to set up a Spark cluster.  
- **Conf_Files/**: Configuration files for Hadoop and Spark:  
  - `core-site.xml`: Core Hadoop configurations.  
  - **had/**:  
    - `core-site.xml`: Hadoop core configurations.  
    - `hadoop-env.sh`: Hadoop environment variables.  
    - `hdfs-site.xml`: Hadoop HDFS configurations.  
    - `workers`: List of worker nodes.  
  - **sp/**:  
    - `slaves`: List of Spark slave nodes.  
    - `spark-defaults.conf`: Default Spark configurations.  
    - `spark-env.sh`: Spark environment variables.  
    - `spark-env.sh.template`: Template for Spark environment variables.  

---


