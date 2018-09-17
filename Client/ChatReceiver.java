import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class ChatReceiver extends Receiver implements Runnable {
    /* Riceve le risposte alle richieste effettuate. */

    private final ThreadPoolExecutor fileExecutor;
    private final BufferedWriter writer;
    /* <Identificatore,path> dei file da inviare. */
    private final ConcurrentHashMap<Long,String> files;

    /**
     * Inizializza un thread "ricevitore" delle risposte alle richieste
     * effettuate. Inizializza il thread pool che gestirà l'invio dei
     * file in p2p e la struttura dati che contiene le informazioni
     * riguardo i file da inviare.
     *
     * @param reader lato in lettura sulla chat socket.
     * @param writer lato in scrittura sulla chat socket.
     * @param files struttura dati condivisa che contiene i file da inviare.
     */
    public ChatReceiver(BufferedReader reader,BufferedWriter writer,ConcurrentHashMap<Long,String> files){
        super(reader);
        this.writer = writer;
        fileExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        this.files = files;
    }

    /**
     * Si mette in attesa di ricevere sulla chat socket nuove risposte.
     */
    public void run() {
        JSONObject obj;
        String msg;
        Long ide;
        Long port;
        Thread currentThread = Thread.currentThread();
        try {
            while(!currentThread.isInterrupted()) {
                try{
                    obj = receive();
                }catch (ParseException e){
                    System.err.println("PARSE EXCEPTION CHAT RECEIVER");
                    /* Non termino ma riprovo a leggere */
                    continue;
                }
                if(obj == null) break;
                switch ((String) obj.get(TYPE)) {
                    case ACK:
                        /* Sia ACK che NACK fanno le stesse cose. */
                    case NACK:
                        msg = (String) obj.get(MSG);
                        System.out.println(msg);
                        break;
                    case CHATMSG:
                        msg = "[MSG] "+obj.get(FROM) + ": " + obj.get(MSG);
                        System.out.println(msg);
                        break;
                    case FILEMSG:
                        /* Destinatario del file. */
                        String from = (String) obj.get(FROM);
                        String fileName = (String) obj.get(FILENAME);
                        ide = (Long) obj.get(IDE);
                        Long len = (Long) obj.get(LEN);
                        msg = "[FILE] "+from+": " + fileName;
                        fileExecutor.execute(new FileReceiver(from,fileName,len,ide,writer));
                        System.out.println(msg);
                        break;
                    case SOCKETINFO:
                        /* Mittente del file. */
                        String ip = (String) obj.get(IP);
                        port = (Long) obj.get(PORT);
                        ide = (Long) obj.get(IDE);
                        /* Prendo il valore e rimuovo. */
                        String path = files.remove(ide);
                        if(path != null) {
                            fileExecutor.execute(new FileSender(ip, port, path));
                        }
                        break;
                    default:
                        System.err.println("DEFAULT " + obj.get(TYPE));
                        break;
                }
            }
        } catch (SocketException e){
            /* Lanciata alla chiusura della socket. */
            System.err.println("SOCKET EXCEPTION CHAT RECEIVER THREAD");
        }catch (IOException e) {
            System.err.println("IO EXCEPTION CHAT RECEIVER THREAD");
        } finally {
            poolTermination();
        }
    }

    /**
     * Termina il pool di thread (gracefully).
     */
    private void poolTermination(){
        fileExecutor.shutdown();
        try {
           fileExecutor.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            System.err.println("INTERRUPTED WHILE WAITING POOL TERMINATION");
        }
        if(!fileExecutor.isTerminated()) {
            fileExecutor.shutdownNow();
            try {
                fileExecutor.awaitTermination(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                System.err.println("INTERRUPTED WHILE WAITING POOL TERMINATION");
            }
        }
        if(!fileExecutor.isTerminated()){
            System.err.println("POOL TERMINATION FAILED");
        }
    }
}
