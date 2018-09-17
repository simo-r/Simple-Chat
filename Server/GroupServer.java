import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

class GroupServer implements Runnable {
    private static final int MAX_UDP_PCKT_LEN = 1024;
    private DatagramSocket ds;
    private AtomicInteger errorCount;
    private ThreadPoolExecutor executor;
    private GroupService groupOp;

    /**
     * Inizializza la socket UDP per le operazioni sui gruppi,
     * il thread poool da utilizzare, il numero di errori correnti, e
     * l'implementazione delle operazioni.
     *
     * @param groupPort porta per la datagram socket dei gruppi.
     * @param errorCount numero di errori correnti rilevati.
     * @param executor thread pool per eseguire le richieste.
     * @param groupOp oggetto contenente le implementazioni delle operazioni sui gruppi.
     * @throws SocketException se la creazione della datagram socket fallisce.
     */
    public GroupServer(int groupPort, AtomicInteger errorCount,
                       ThreadPoolExecutor executor, GroupService groupOp) throws SocketException {
        ds = new DatagramSocket(groupPort);
        this.errorCount = errorCount;
        this.executor = executor;
        this.groupOp = groupOp;
    }

    /**
     * Riceve una richiesta di invio di un messaggio
     * ad un gruppo attraverso UDP e la fa elaborare
     * ad un thread del pool.
     */
    public void run() {
        System.out.println("GROUP SERVER UP");
        byte[] buf = new byte[MAX_UDP_PCKT_LEN];
        DatagramPacket recv;
        String json;
        JSONObject obj;
        JSONParser parser = new JSONParser();
        Thread currentThread = Thread.currentThread();
        while(!currentThread.isInterrupted()){
            recv = new DatagramPacket(buf,buf.length);
            try {
                ds.receive(recv);
                json = new String(recv.getData(),0,recv.getLength(), StandardCharsets.UTF_8);
                obj = (JSONObject) parser.parse(json);
                executor.execute(new GroupTask(obj,groupOp,recv.getAddress(),recv.getPort()));
            } catch (IOException e) {
                errorCount.incrementAndGet();
                System.err.printf("UDP RECEIVE EXCEPTION. ERROR NUMBER: %d",errorCount.get());
            } catch (ParseException e) {
                /* Non incremento gli errori ma riprovo a leggere */
                System.err.printf("UDP PARSER EXCEPTION. ERROR NUMBER: %d",errorCount.get());
            }
        }
        close();
    }

    /**
     * Chiude la datagram socket.
     */
    public void close(){
        ds.close();
    }
}
