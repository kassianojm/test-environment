package spark;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.scheduler.StreamingListener;
import org.apache.spark.streaming.scheduler.StreamingListenerBatchCompleted;
import org.apache.spark.streaming.scheduler.StreamingListenerBatchStarted;
import org.apache.spark.streaming.scheduler.StreamingListenerBatchSubmitted;
import org.apache.spark.streaming.scheduler.StreamingListenerOutputOperationCompleted;
import org.apache.spark.streaming.scheduler.StreamingListenerOutputOperationStarted;
import org.apache.spark.streaming.scheduler.StreamingListenerReceiverError;
import org.apache.spark.streaming.scheduler.StreamingListenerReceiverStarted;
import org.apache.spark.streaming.scheduler.StreamingListenerReceiverStopped;
import org.apache.spark.streaming.scheduler.StreamingListenerStreamingStarted;
import org.yggdrasil.server.YggdrasilCommandListener;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

public abstract class AbstractClusterServer {
    protected Integer port    = null;
    protected int  TH;
    protected String  network = null;
    protected int     receivers_by_sender = 1;
    protected String[] senders= null;
    protected int tempon = 0;
    protected int batch_port = 5000;
    protected int in_port = 9999;
    protected int clients = 1;
    protected YggdrasilCommandListener ygg;
    protected JavaStreamingContext ssc;
    protected Integer window_time;
    protected String blockInterval;
    protected String concurrentJobs;
    protected String hdfs_root;
    protected String parallelism;
    protected int lastRec = 0;    


    public void run(String[] args) throws Exception {
        // create the command line parser
        CommandLineParser parser = new GnuParser();

        // create the Options
        Options options = new Options();
        options.addOption(OptionBuilder
               .withArgName("TH")
               .hasArg()
               .isRequired()
               .withLongOpt("THRESHOLD")
               .create());
        options.addOption(OptionBuilder
               .withArgName("N")
               .withLongOpt("NETWORK")
               .hasArg()
               .isRequired()
               .create());
        options.addOption(OptionBuilder
               .withArgName("I")
               .withLongOpt("DATASOURCES")
               .hasArg()
               .isRequired()
               .create());
        options.addOption(OptionBuilder
               .withArgName("T")
               .withLongOpt("TEMPON")
               .hasArg()
               .isRequired()
               .create());
        options.addOption(OptionBuilder
               .withArgName("C")
               .withLongOpt("CLIENTS")
               .hasArg()
               .isRequired()
               .create());
        options.addOption(OptionBuilder
               .withArgName("H")
               .withLongOpt("RECEIVERS")
               .hasArg()
               .isRequired()
               .create());
        options.addOption(OptionBuilder
               .withArgName("D")
               .withLongOpt("WINDOWTIME")
               .hasArg()
               .isRequired()
               .create());
        options.addOption(OptionBuilder
               .withArgName("E")
               .withLongOpt("BLOCKINTERVAL")
               .hasArg()
               .isRequired()
               .create());
        options.addOption(OptionBuilder
               .withArgName("F")
               .withLongOpt("CONCBLOCK")
               .hasArg()
               .isRequired()
               .create());
        options.addOption(OptionBuilder
               .withArgName("G")
               .withLongOpt("HDFS")
               .hasArg()
               .isRequired()
               .create());
        options.addOption(OptionBuilder
               .withArgName("K")
               .withLongOpt("PARAL")
               .hasArg()
               .isRequired()
               .create());

        try {
            // parse the command line arguments
            CommandLine line = parser.parse( options, args );
            // validate that block-size has been set
            if( line.hasOption( "THRESHOLD" ) ){
                TH = Integer.parseInt(line.getOptionValue( "THRESHOLD" ));
            }
            if( line.hasOption( "NETWORK" ) ){
                network =  line.getOptionValue( "NETWORK" );
            }
            if( line.hasOption( "DATASOURCES" ) ){
                senders =  line.getOptionValue( "DATASOURCES" ).split(",");
            }
            if( line.hasOption( "TEMPON" ) ){
                tempon =  Integer.parseInt(line.getOptionValue( "TEMPON" ));
            }
            if( line.hasOption( "RECEIVERS" ) ){
                receivers_by_sender =  Integer.parseInt(line.getOptionValue( "RECEIVERS" ));
            }
            if( line.hasOption( "CLIENTS" ) ){
                clients =  Integer.parseInt(line.getOptionValue( "CLIENTS" ));
            }
            if( line.hasOption( "WINDOWTIME" ) ){
                window_time =  Integer.parseInt(line.getOptionValue( "WINDOWTIME" ));
            }
            if( line.hasOption( "BLOCKINTERVAL" ) ){
                blockInterval =  line.getOptionValue( "BLOCKINTERVAL" );
            }
            if( line.hasOption( "CONCBLOCK" ) ){
                concurrentJobs =  line.getOptionValue( "CONCBLOCK" );
            }
            if( line.hasOption( "HDFS" ) ){
                hdfs_root =  line.getOptionValue( "HDFS" );
            }
            if( line.hasOption( "PARAL" ) ){
                parallelism =  line.getOptionValue( "PARAL" );
            }
        }
        catch( ParseException exp ) {
            System.out.println( "Unexpected exception:" + exp.getMessage() );
        }

		/**
		* spark.streaming.concurrentJobs is the number of concurrent jobs, i.e. threads in streaming-job-executor thread pool (default: 1)
		* spark.default.parallelism defaults to the number of all cores on all machines 
		* spark.streaming.blockInterval represents the interval at which data received by Spark Streaming receivers is chunked into blocks of data before storing them in Spark. Minimum recommended - 50 ms.
		**/
        SparkConf sparkConf = new SparkConf().setAppName(getName());
        sparkConf.set("spark.streaming.stopGracefullyOnShutdown","true");
        //sparkConf.set("spark.streaming.backpressure.enabled",    "true");
        sparkConf.set("spark.streaming.concurrentJobs",concurrentJobs);
//	sparkConf.set("spark.default.parallelism",     parallelism);
        sparkConf.set("spark.streaming.blockInterval", blockInterval);
      
		ZContext ctx = new ZContext();
        ZMQ.Socket push = ctx.createSocket(ZMQ.PUSH);

		//mechanism to send msg containing batches informations (send the same thing for multiple MQs)
		//for (int i=0; i<senders.length; i++){
			String sender = senders[0];
			push.connect("tcp://"+sender+":"+batch_port);
			System.out.println("connected on "+sender+" PUSH "+ batch_port);
			//batch_port++;
		//}


        applyCustomConf();
        ssc = new JavaStreamingContext(sparkConf, Durations.milliseconds(window_time));
        
        
        ssc.sparkContext().setLogLevel("ERROR");

        
        PrintWriter pw = new PrintWriter(new File("/tmp/batch_infos.csv"));
        StringBuilder sb = new StringBuilder();
        sb.append("batchTime;");
        sb.append("numRecords;");
        sb.append("processingDelay;");
        //sb.append("processingStartTime;");
        //sb.append("processingEndTime;");
        sb.append("schedulingDelay;");
        //sb.append("submissionTime;");
        //sb.append("totalDelay\n");
		sb.append("\n");
        pw.write(sb.toString());
        
        
        ssc.addStreamingListener(new StreamingListener() {
            @Override
            public void onStreamingStarted(StreamingListenerStreamingStarted arg0) {}
            
            @Override
            public void onReceiverStopped(StreamingListenerReceiverStopped arg0) {}
            
            @Override
            public void onReceiverStarted(StreamingListenerReceiverStarted arg0) {}
            
            @Override
            public void onReceiverError(StreamingListenerReceiverError arg0) {}
            
            @Override
            public void onOutputOperationStarted(StreamingListenerOutputOperationStarted arg0) {}
            
            @Override
            public void onOutputOperationCompleted(StreamingListenerOutputOperationCompleted arg0) {}
            
            @Override
            public void onBatchSubmitted(StreamingListenerBatchSubmitted arg0) {
                //TODO ?
            }
            
            @Override
            public void onBatchStarted(StreamingListenerBatchStarted arg0) {
                //TODO ?
            }
            
            @Override
            public void onBatchCompleted(StreamingListenerBatchCompleted arg0) {
                StringBuilder sb = new StringBuilder();
                sb.append(arg0.batchInfo().batchTime().milliseconds());
                sb.append(";");
                sb.append(arg0.batchInfo().numRecords());
				sb.append(";");
                sb.append(arg0.batchInfo().processingDelay().get());
                sb.append(";");
                //sb.append(arg0.batchInfo().processingStartTime().get());
                //sb.append(";");
                //sb.append(arg0.batchInfo().processingEndTime().get());
                //sb.append(";");
                sb.append(arg0.batchInfo().schedulingDelay().get());
                //sb.append(";");
                //sb.append(arg0.batchInfo().submissionTime());
                //sb.append(";");
                //sb.append(arg0.batchInfo().totalDelay().get());
                sb.append("\n");
                byte bytes[] = sb.toString().getBytes();
		push.send(bytes,0);
		pw.write(sb.toString());
                pw.flush();
            }
        });

        register_spark_application();

        ssc.start();
        ssc.awaitTermination();
        pw.close();
    }

    protected void applyCustomConf() {}
    protected abstract String getName();
    protected abstract void register_spark_application() throws IOException;
}
