package utils;
import java.util.Arrays;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.Timer;
import java.util.TimerTask;

public class CSVUtils {

    private static final char DEFAULT_SEPARATOR = ',';
    private final Semaphore available = new Semaphore(1, true);
    private FileWriter w;
    private int nbValues = 0;
    private String type;
    private Timer t;
    private int here = 0;

    public CSVUtils(String filename, String type) throws IOException{
        w = new FileWriter(filename);
        this.type = type;
        writeLine(Arrays.asList("seconds", "how", "type"));
        t = new Timer(true);
        t.schedule(new TimerTask(){
           public void run() {
               writeHowMany();
           }
        }, 1000, 1000);
    }

    public void writeHowMany(){
        try{
            available.acquire();
            try{
                writeLine(Arrays.asList(""+here, ""+nbValues,type));
                w.flush();
            }catch(Exception e){
                t.cancel();
            }
            nbValues = 0;
            here +=1;
            available.release();
        }catch(Exception e){
        }
    }

    public void incrementNbValues(int plusplus){
        try{
            available.acquire();
            nbValues+=plusplus;
            available.release();
        }catch(Exception e){ }
    }

    public void incrementNbValues(){
        try{
            available.acquire();
            nbValues++;
            available.release();
        }catch(Exception e){ }
    }

    public void close() throws IOException{
        writeHowMany();
        w.close();
    }

    public void writeLine(List<String> values) throws IOException {
        writeLine(values, DEFAULT_SEPARATOR, ' ');
    }

    public void writeLine(List<String> values, char separators)
            throws IOException {
        writeLine(values, separators, ' ');
    }

    //https://tools.ietf.org/html/rfc4180
    private String followCVSformat(String value) {

        String result = value;
        if (result.contains("\"")) {
            result = result.replace("\"", "\"\"");
        }
        return result;

    }

    public void writeLine(List<String> values, char separators,
            char customQuote) throws IOException {

        boolean first = true;

        //default customQuote is empty

        if (separators == ' ') {
            separators = DEFAULT_SEPARATOR;
        }

        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (!first) {
                sb.append(separators);
            }
            if (customQuote == ' ') {
                sb.append(followCVSformat(value));
            } else {
                sb.append(customQuote).append(
                        followCVSformat(value)).append(customQuote);
            }

            first = false;
        }
        sb.append("\n");
        w.append(sb.toString());


    }

}
