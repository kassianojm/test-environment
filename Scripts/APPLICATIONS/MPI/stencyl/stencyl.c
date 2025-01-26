#include <math.h>
#include <stdio.h>
#include <zmq.h>
#include <unistd.h>
#include <getopt.h>
#include <stdlib.h>
#include <mpi.h>
#include <string.h>
#include <sys/time.h>
#include <assert.h>
#include <omp.h>
#include <time.h>
#include <inttypes.h>


int isPerfectSquare(int number)
{
    int iVar;
    float fVar;
 
    fVar=sqrt((double)number);
    iVar=fVar;
 
    if(iVar==fVar)
        return 1;
    else
        return 0;
}

char** str_split(char* a_str, const char a_delim){
    char** result    = 0;
    size_t count     = 0;
    char* tmp        = a_str;
    char* last_comma = 0;
    char delim[2];
    delim[0] = a_delim;
    delim[1] = 0;

    /* Count how many elements will be extracted. */
    while (*tmp)
    {
        if (a_delim == *tmp)
        {
            count++;
            last_comma = tmp;
        }
        tmp++;
    }

    /* Add space for trailing token. */
    count += last_comma < (a_str + strlen(a_str) - 1);

    /* Add space for terminating null string so caller
       knows where the list of returned strings ends. */
    count++;

    result = malloc(sizeof(char*) * count);

    if (result)
    {
        size_t idx  = 0;
        char* token = strtok(a_str, delim);

        while (token)
        {
            assert(idx < count);
            *(result + idx++) = strdup(token);
            token = strtok(0, delim);
        }
        assert(idx == count - 1);
        *(result + idx) = 0;
    }

    return result;
}

//MQ: This function clean the buffer when the messages are created (zmq function)
void my_free (void *data, void *hint){
   free (data);
}

//This function gets the current timestamp in milliseconds
long long current_timestamp() {
    struct timeval te; 
    gettimeofday(&te, NULL); // get current time
    long long milliseconds = te.tv_sec*1000LL + te.tv_usec/1000; // calculate milliseconds
    return milliseconds;
}

//This updates the positions of the array to compute the next step
void make_next_step(float*** prev, float*** process, float* up, float* down, float* left, float* right, float array_width){
	
    for(int i=0; i<array_width; i++){
        for(int j=0; j<array_width; j++){
            float lup    = i-1 < 0             ? up[j]    : ((float**)*prev)[i-1][j];
            float ldown  = i+1 == array_width ? down[j]  : ((float**)*prev)[i+1][j];
            float lleft  = j-1 < 0             ? left[i]  : ((float**)*prev)[i][j-1];
            float lright = j+1 == array_width ? right[i] : ((float**)*prev)[i][j+1];
            ((float**)*process)[i][j] = (((float**)*prev)[i][j]+lup+ldown+lleft+lright)/5;
        }
    }
  
}

//Summary before starting
void summary(int rank, int buffer_size, int duration, int size, int how_many_queus){
	float tarray = (int)buffer_size;
	if (rank == 0){
		printf("Duration (s): %d\n", duration/1000);
		printf("MSG SIze: %.0f (Bytes), ~%.2f (MB)\n",tarray,tarray/1024/1024);
		printf("#MQs: %d\n", how_many_queus);
		printf("#Queues %d\n\n", size);
		printf("Queueing...\n");
	}	
}

//Useful information
/**
 * duration			: Execution time			
 * array_size			: Msg size in bytes and also represents the array size ()
 * ipqueue			: ip used to connect in the message queue
 * portqueue			: port use to connect in the message queue
 * io_threads			: Threads used per queue			
 * size			        : #MPI processes
 * */

int main(int argc, char **argv){
	
	if( argc < 9 || argc > 9)
    {
        printf("The list of arguments is not enought (%d instead of 9).\n", argc);
        printf("APP: stencyl <duration (sec)> <msg size in bytes> <ips MQ> <ports MQ> <#threads per Queue> <How many MQs> <#msg per process to verify (-1 disable mode) > <demo enabled 1 or disabled 0 >\n");
        printf("Np represents the number of queues in the application..\n");
        printf("Local: mpirun -np 8 --host 127.0.0.1 stencyl 40 20000 127.0.0.1,127.0.0.1 5050,4040 1 2 1000 \n");
        return 1;
    }
    
    
          
	//Initializing the MPI processes
    MPI_Init(&argc, &argv);
    // get my rank
    int rank;
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    // get the max rank
    int size;
    MPI_Comm_size(MPI_COMM_WORLD, &size);

    /* // Calcul de la largeur de la grille */
    /* int width = sqrt(size); */
    
    /* /\* if(isPerfectSquare(size)){ *\/ */
    /* /\*     		//printf("%d is a perfect square.\n",size); *\/ */
    /* /\*     	}else{ *\/ */
    /* /\*     		printf("%d is not a perfect square.\n",size); *\/ */
    /* /\*     		exit(1); *\/ */
    /* /\*     } *\/ */
    
    /* // Calcul du rang du début de la ligne, et celui de la fin de la ligne */
    /* int my_line  = (rank) / width; */
    /* int min_line = (my_line * width); */
    /* int max_line = (my_line * width) + width -1; */
    /* // Calcul des rangs des voisins */
    /* int up    = (((rank - width) % size) + size ) % size; */
    /* int down  = (((rank + width) % size) + size ) % size; */
    /* int left  = rank > min_line ? rank - 1 : max_line; */
    /* int right = rank < max_line ? rank + 1 : min_line; */

    /* computing the dimensions of the grid */

    int grid_size[2]={0,0};
    
    MPI_Dims_create(size, 2, grid_size);

    if(rank == 0){
        
        printf("Creating a grid of size %d x %d\n", grid_size[0], grid_size[1]);

        if(grid_size[1] == 1){
            printf("A grid can't be created with %d processes\n", size);
            exit(1);
        }
    }

    int up    = (rank + size - grid_size[0]) % size;
    int down  = (rank + size + grid_size[0]) % size;
    int my_column = rank % grid_size[0];
    int left  = rank - 1;
    int right = rank + 1;

    if (my_column == 0){
        left = rank + grid_size[0] - 1;
    }

    if (my_column == grid_size[0] -1){
        right = rank - (grid_size[0] - 1);
    }


    // printf("%d -- U: %d  D: %d L: %d R: %d (my_column: %d)\n", rank, up, down, left, right, my_column);
    
    
        

    
    
	int duration = atoi(argv[1]) * 1000;
	// MQ: The array_size (Msg size in bytes) was created using a time-serie based interval (Msg size / by 4). Thus, a MSG comprised of 280 bytes generates an array with 70 x 70 positions to store the MSGs' content.
	// Less 8 Represents the size two ints used as head in the buffer.
    int array_size = (atoi(argv[2]) - 8 )/ 4;
    char* ipqueue;
    int portqueue;
    char* list_ipqueue = argv[3];
    char* list_portqueue = argv[4];
    int io_threads = atoi(argv[5]);
    int how_many_queus = atoi(argv[6]);
    uint64_t toVerify  = atoi(argv[7]);
    char** split_ipqueue;
    char** split_portqueue;
	int demo = atoi(argv[8]);
   
   	// split ips and ports
	split_ipqueue   = str_split(list_ipqueue, ',');
    split_portqueue = str_split(list_portqueue, ',');  
    int my_queue_numbner = rank % how_many_queus;
	ipqueue   = split_ipqueue[my_queue_numbner];
    portqueue = atoi(split_portqueue[my_queue_numbner]);
        
     if(demo){
        printf("demo mod activated, no real computations performed\n");
       // printf("demo %d \n", demo);

    }else{
        //printf("rank %d | size %d, width %d\n", rank, size, width);
       // printf("rank %d | my_line %d, min_line %d, max_line %d\n", rank, my_line, min_line, max_line);
        //printf("rank %d | up %d, down %d, left %d, right %d\n", rank, up, down, left, right);
        //printf("Generating data\n");
        //printf("demo %d \n", demo);
    }
	
 	//MQ: for each process a ZMQ connection is open, thus the number of queues is equals to the number of MPI process. There is no division between ranks to create a scenario with intensive data ingestion.
    void* context = zmq_ctx_new();
    
    //MQ: I/O threads - by standard, just one thread is set for all sockets. Each thread handles 1Gb/s of data (in/out) and tons of connections
    zmq_ctx_set (context, ZMQ_IO_THREADS, io_threads);
    assert (zmq_ctx_get (context, ZMQ_IO_THREADS) == io_threads);
    
    //MQ: star to open a PUSH connection
    void* push    = zmq_socket (context, ZMQ_PUSH);
       
    
    //MQ: Creates the communication channel
    char push_string[100];
    sprintf (push_string, "tcp://%s:%d", ipqueue,portqueue);
    if (zmq_connect (push, push_string) == -1)
    {
        printf("rank %d | failure connecting push", rank);
        printf ("error in zmq_connect: %s \n", zmq_strerror (errno));
		return -1;
    }

    //Creates the buffer
	//int buffer_size = 4 + 4 + array_size*4;
    
    //Allocate the array grid and creates the buffer.
    
    int array_width = (int)floor(sqrt(array_size));
    float** simulation_grid = malloc(sizeof(float*)*array_width);
    float** t_1_simulation_grid = malloc(sizeof(float*)*array_width);
    for(int i=0; i<array_width; i++)
    {
        t_1_simulation_grid[i] = malloc(sizeof(float)*array_width);
        simulation_grid[i]     = malloc(sizeof(float)*array_width);
        for(int j=0; j<array_width; j++)
        {
            simulation_grid[i][j]     = j;
            t_1_simulation_grid[i][j] = j;
        }
     }
    int buffer_size = 8+(array_width*array_width)*4;

    
    float*** process = &simulation_grid;    // Buffer to store newly processed values
    float*** prev    = &t_1_simulation_grid;// Buffer to values to be send
      

    //Allocate the arrays to receive the neighbours data
    MPI_Request req_up;
    MPI_Request req_down;
    MPI_Request req_left;
    MPI_Request req_right;
    
    float* up_array    = malloc(array_width * sizeof(float));
    float* down_array  = malloc(array_width * sizeof(float));
    float* left_array  = malloc(array_width * sizeof(float));
    float* right_array = malloc(array_width * sizeof(float));

    //Allocate arrays for the communiction
    MPI_Request send_up;
    MPI_Request send_down;
    MPI_Request send_left;
    MPI_Request send_right;
    
    float* my_up_array;
    float* my_down_array;
    float* my_left_array  = malloc(array_width * sizeof(float));
    float* my_right_array = malloc(array_width * sizeof(float));

    //printf("rank %d | external arrays allocated \n", rank);
    MPI_Status status;

    MPI_Request barrier_request;
    
	long long start_time = current_timestamp(); 
    int nb_sent = 0;                              //Count how many msgs were sent to the queue
    int aux = 0;                                  //Flag used to obtain the queueing time
    int count_retry=1;                            //To count how many times a msg tried to be sent
    int need_retry = 1;                           //Msg status - 1 represents that the send of msg fails and, 0 representes the success
    long long qtime=0;                            //Time spent to queue msgs
    double msgsize;                               //Size of a msg
    long long timeaux=0;						  //Save the difference of the time
    int keepup = 1;								  //This variable control the application flow - if 1 the applications remains alive, if 0 (reached when duration's time over).
    
     if (rank == 0){
		printf("A brief summary...\n\n");
	}  
           
//    printf("Queue created by rank %d at %s:%d\n", rank, ipqueue,  portqueue);
	   
    summary(rank, buffer_size, duration, size, how_many_queus);

    //Starting the application
       
        
        
    MPI_Barrier(MPI_COMM_WORLD);

    for (int step=1; keepup; step++)
    {//Do it until the end of duration        
        
	 if(!demo){	
        my_up_array   = ((float**)*prev)[0];
        my_down_array = ((float**)*prev)[array_width-1];
		for(int i=0; i<array_width; i++)
		{
            my_left_array[i]  = ((float**)*prev)[i][0];
            my_right_array[i] = ((float**)*prev)[i][array_width-1];
        }

			//Asynchronous send operations
			MPI_Isend(my_up_array, array_width, MPI_FLOAT, up, 0, MPI_COMM_WORLD, &send_up);
			MPI_Isend(my_down_array, array_width, MPI_FLOAT, down, 0, MPI_COMM_WORLD, &send_down);
			MPI_Isend(my_left_array, array_width, MPI_FLOAT, left, 0, MPI_COMM_WORLD, &send_left);
			MPI_Isend(my_right_array, array_width, MPI_FLOAT, right, 0, MPI_COMM_WORLD, &send_right);
		
			//Receive arrays from neighbours
			MPI_Irecv(up_array, array_width, MPI_FLOAT, up, 0, MPI_COMM_WORLD, &req_up);
			MPI_Irecv(down_array, array_width, MPI_FLOAT, down, 0, MPI_COMM_WORLD, &req_down);
			MPI_Irecv(left_array, array_width, MPI_FLOAT, left, 0, MPI_COMM_WORLD, &req_left);
			MPI_Irecv(right_array, array_width, MPI_FLOAT, right, 0, MPI_COMM_WORLD, &req_right);
		}else{//Ensure all cores going at the same speed
            MPI_Ibarrier(MPI_COMM_WORLD, &barrier_request);
        }

		//Starts the procedure to send msg to the message queue
        {
			char* buffer = malloc(buffer_size);
            memcpy(buffer,&rank,4);
            int position = 4;
            memcpy(buffer+position,&step,4);
            position+=4;
            //double ccont=0;
            //char* output_csv = malloc(100);
	    //sprintf(output_csv, "/home/kassiano/Documents/gitRep/comd-erods/MPI/stencyl/LOG_Step/Log_Rank%dStep%d.csv",rank,step);
	    //FILE* outcsv = fopen(output_csv, "w");
            for(int i=0; i<array_width; i++)
            {
                for(int j=0; j<array_width; j++)
                {
					//float element = ((float**)*prev)[i][j];
					//ccont += (double)element;
					//fprintf(outcsv,"r%d,s%d,v[%d][%d]:%.2f,sum%f\n",rank,step,i,j,element,ccont);
                    memcpy(buffer+position,&(*prev)[i][j],4);
                    position+=4;          
                }
            }
            //fflush(outcsv);
	//		fclose(outcsv);
          //  float avg = ccont/((array_width*array_width));
            int arraysize = array_width*array_width;
            //printf("r%d,s%d,sum%f,array%d,avg%f\n",rank, step,ccont,arraysize,avg);



            //0MQ: Creating msg type
            zmq_msg_t msg;

            //0MQ: Creating the MSG's skeleton - the return of assert is 0 for sucess and -1 if fails
			assert(zmq_msg_init_data(&msg, buffer, buffer_size, my_free, NULL) != -1);
			
			//0MQ:Try to send the message - controll the queue size and looks for the timeout - no loss data due the buffers
             int need_retry = 1;
             
			 while(need_retry && keepup)
			 {
				msgsize =  zmq_msg_size(&msg);
				int nrank = *(int *)zmq_msg_data (&msg);
				int nstep = *(((int *)zmq_msg_data (&msg))+1);
				int ncontent = *(((int *)zmq_msg_data (&msg))+2);
				int rc = zmq_msg_send(&msg, push, ZMQ_DONTWAIT);
				//There is no space left in MSQ buffer
				if(rc == -1)
				{
					//Save the queueing time until fill out the buffer
					if(aux == 1)
					{
							long long now = current_timestamp();
							qtime += (now - start_time) - timeaux ;
							aux = 0;
											}
					if(errno == EAGAIN)
					{
						/*
						printf ("\n Error in zmq_msg_send: %s \n", zmq_strerror (errno));
						printf(" BUSY:: The message cannot be queued on the socket. \n");
						printf(" Rank %d | Queueing MSG %d again (%d)!\n",rank, step, count_retry);
						count_retry++;	*/
					}
                }
                else
                {
					if(aux == 0)
					{
						//Save a checkpoint with the current time
							long long now = current_timestamp();
							timeaux = (now - start_time);
					}
					//printf ("MSG Sent");

					aux=1;
					need_retry = 0; //Msq queued, maintain the status
					nb_sent++; //Count the msg queued
				}
					//Stop after DURATION in seconds
					long long now = current_timestamp();
					keepup = now - start_time < duration;
			}
			count_retry = 1;//Msg queued and flag is restarted
			assert(zmq_msg_close(&msg) != -1);	
		}
		//Wait for the messages from neighbours - MPI exchange msgs
        if(keepup){
			if(!demo){            
			int complete = 0;
            int a = 0;
            int b = 0;
            int c = 0;
            int d = 0;
            //while I don't have all the values to create a next step do it
            while(keepup && !complete){
                if(!a){MPI_Test(&req_up,    &a, &status);}
                if(!b){MPI_Test(&req_down,  &b, &status);}
                if(!c){MPI_Test(&req_left,  &c, &status);}
                if(!d){MPI_Test(&req_right, &d, &status);}
                //Logic expression - result is 0 or 1. If 1, the application goes to the next step
                complete = a & b & c & d;
                //Stop after DURATION seconds
                long long now = current_timestamp();
                keepup = now - start_time < duration;
   			}
			//
            if(keepup && complete){
				// Adjust positions
                make_next_step(prev, process, up_array, down_array, left_array, right_array, array_width);
            }
            }else{//await for the barrier to be complete
                int barrier_test = 0;
                while(!barrier_test && keepup){
                    MPI_Test(&barrier_request, &barrier_test, &status);
                    long long now = current_timestamp();
                    keepup = now - start_time < duration;
                }
            }

            //Awitch processes of next step
            float*** tmp;
            tmp     = prev;
            prev    = process;
            process = tmp;
            
        }
        //Finishes when the durantion was reached
        long long now = current_timestamp();
        keepup = now - start_time < duration;
        //printf("\n toVerify %ld - step %d ",toVerify,step);
        if(toVerify != -1 && toVerify == step){
			while(keepup)
			 {
					long long now = current_timestamp();
					keepup = now - start_time < duration;
					//printf(".");
			}
			MPI_Finalize();
			return 0;
		}
	}
    //int global_sum;
    //MPI_Reduce(&nb_sent,&global_sum,1, MPI_INT, MPI_SUM, 0, MPI_COMM_WORLD );
    //MPI_Ibarrier(MPI_COMM_WORLD, &barrier_request);
	/*
    if (rank == 0){
		// qtime all msgs were sent
		if (qtime == 0){
			qtime = duration;
		}
		printf("\n Queues:                  (#): %d \n", size);
		printf(" MSGs queued per queue:   (#): %d ", nb_sent - 1);
		nb_sent = global_sum - size;
		printf("\n MSGs queued:             (#): %d \n", nb_sent);
		printf(" Execution time          (ms): %d(%dsec) \n Queueing time           (ms): %lld(%llds)\n Retransmition time      (ms): %lld(%llds) \n", duration, duration / 1000, qtime, qtime / 1000, duration - qtime, (duration - qtime) / 1000);
		printf(" Throughput total     (bytes): %.2f (%.2fMb) (%.2fGb)\n", nb_sent * msgsize,  (nb_sent * msgsize) / 1024 / 1024, (nb_sent * msgsize) / 1024 / 1024 /1024);
		printf(" Avarage Throughput (bytes/s): %.2f (%.3fMb/s) (%.3f Gb/s) \n", ((nb_sent * msgsize) / (qtime / 1000)), (((nb_sent * msgsize) / (qtime / 1000)) / 1024) / 1024,(((nb_sent * msgsize) / ((qtime / 1000)) / 1024) / 1024) /1024);
	}
	*/
    MPI_Finalize();
    return 0;
}
