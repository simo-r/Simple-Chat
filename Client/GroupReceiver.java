import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

class GroupReceiver implements Runnable,ReplyCode {
    private final DatagramSocket groupServer;

    /**
     * Inizializza la datagram socket per i gruppi.
     *
     * @param groupServer datagram socket per i gruppi
     */
    public GroupReceiver(DatagramSocket groupServer){
        this.groupServer = groupServer;
    }

    /**
     * Riceve le risposte dal server attraverso UDP
     * riguardo l'invio di messaggi nei gruppi.
     */
    public void run() {
        JSONObject obj;
        Thread currentThread = Thread.currentThread();
        try {
            while (!currentThread.isInterrupted()) {
                try {
                    obj = receive();
                } catch (ParseException e) {
                    System.err.println("PARSE EXCEPTION GROUP RECEIVER THREAD");
                    /* Non termino ma riprovo a leggere */
                    continue;
                }
                switch ((String) obj.get(TYPE)) {
                    case NACK:
                        String msg = (String) obj.get(MSG);
                        System.out.println(msg);
                        break;
                }
            }
        }catch (SocketException e){
            /* Lanciata alla chiusura della datagram socket. */
            System.err.println("SOCKET EXCEPTION GROUP RECEIVER THREAD");
        } catch (IOException e) {
            System.err.println("IO EXCEPTION GROUP RECEIVER THREAD");
        }
    }

    /**
     * Riceve un pacchetto UDP dalla socket per i gruppi.
     *
     * @return JSONOBJECT contenente il pacchetto UDP ricevuto
     * @throws IOException se la ricezione del pacchetto fallisce.
     * @throws ParseException se il parsing del pacchetto fallisce.
     */
    private JSONObject receive() throws IOException,ParseException{
        JSONParser parser = new JSONParser();
        byte[] buf = new byte[GroupImpl.MAX_UDP_PCKT_LEN];
        DatagramPacket recv = new DatagramPacket(buf,buf.length);
        groupServer.receive(recv);
        /* Prelievo SOLO i dati realmente inviati. */
        String json = new String(recv.getData(),0,recv.getLength(), StandardCharsets.UTF_8);
        return (JSONObject) parser.parse(json);
    }
}
