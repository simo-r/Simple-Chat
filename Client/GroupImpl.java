import org.json.simple.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class GroupImpl extends Sender implements ReplyCode {
    public static final int MAX_UDP_PCKT_LEN = 1024;
    private final InetAddress serverAddr;
    private final int groupPort;
    private JSONObject obj;
    private DatagramSocket groupServer;

    /**
     * Inizializza la INET socket del server e la porta
     * utilizzata da UDP per i messaggi sui gruppi.
     *
     * @param serverAddr INET address del server.
     * @param groupPort porta del server per le richieste dei gruppi.
     */
    public GroupImpl(InetAddress serverAddr, int groupPort){
        this.serverAddr= serverAddr;
        this.groupPort = groupPort;
    }

    /**
     * Inizializza il writer per mandare la richieste su TCP
     * e la DatagramSocket per mandare i messaggi per i gruppi
     * al server tramite UDP.
     *
     * @param writer writer sulla request socket.
     */
    public boolean init(BufferedWriter writer){
        boolean result = true;
        this.writer = writer;
        try {
            groupServer = new DatagramSocket();
            receiverT = new Thread(new GroupReceiver(groupServer));
            receiverT.start();
        } catch (SocketException e) {
            System.out.println();
            result = false;
        }
        return result;

    }

    /**
     * Invia la richiesta di creazione del gruppo attraverso
     * la connessione TCP per le richieste al server.
     *
     * @param groupName nome del gruppo da creare.
     * @return true se l'operazione va a buon fine,
     *         false altrimenti.
     */
    public boolean createGroup(String groupName){
        obj = new JSONObject();
        obj.put(TYPE,GRPCREATE);
        obj.put(GRPNAME,groupName);
        return send(obj);
    }

    /**
     * Invia la richiesta di far parte del gruppo
     * attraverso la connessione TCP per le richieste
     * al server.
     *
     * @param groupName nome del gruppo.
     * @return true se l'operazione va a buon fine,
     *         false atrimenti.
     */
    public boolean joinGroup(String groupName){
        obj = new JSONObject();
        obj.put(TYPE,GRPJOIN);
        obj.put(GRPNAME,groupName);
        return send(obj);
    }

    /**
     * Invia una richiesta al server per ricevere la lista
     * dei gruppi presenti con indicazione di quelli a
     * cui appartiene.
     *
     * @return true se l'invio della richiesta va a buon fine,
     *         false altrimenti.
     */
    public boolean groupList(){
        obj = new JSONObject();
        obj.put(TYPE,GRPLST);
        return send(obj);
    }

    /**
     * Invia una richiesta al server per chiudere un gruppo.
     *
     * @param groupName nome del gruppo da chiudere.
     * @return true se l'invio della richiesta va a buon fine,
     *         false altrimenti.
     */
    public boolean closeGroup(String groupName){
        obj = new JSONObject();
        obj.put(TYPE,GRPCLOSE);
        obj.put(GRPNAME,groupName);
        return send(obj);
    }

    /**
     * Invia al server la richiesta di mandare un messaggio al gruppo
     * attraverso UDP.
     *
     * @param from destinatario del messaggio.
     * @param destGrp gruppo di destinazione del messaggio.
     * @param msg messaggio da inviare.
     * @return true se l'operazione va a buon fine,
     *         false altrimenti.
     */
    public boolean sendGrpMsg(String from, String destGrp, String msg ){
        boolean result = true;
        obj = new JSONObject();
        obj.put(TYPE,GRPMSG);
        obj.put(FROM,from);
        obj.put(GRPNAME,destGrp);
        obj.put(MSG,msg);
        byte[] json  = obj.toJSONString().getBytes(StandardCharsets.UTF_8);
        if(json.length > MAX_UDP_PCKT_LEN) {
            System.out.printf("[GROUP] Message to %s too big!\n", destGrp);
            result = false;
        }else {
            /* Mando il pacchetto UDP con la richiesta. */
            DatagramPacket sndPckt = new DatagramPacket(json, json.length, serverAddr, groupPort);
            try {
                groupServer.send(sndPckt);
            } catch (IOException e) {
                System.err.printf("IO EXCEPTION SENDING GROUP MSG TO: %s\n",destGrp);
                result = false;
            }
        }
        return result;
    }

    /**
     * Overriding del metodo della super classe
     * con aggiunta della chiusura della datagram socket.
     */
    public void close(){
        if(groupServer != null && !groupServer.isClosed()) {
            groupServer.close();
        }
        /* La variabile socket nel super è null. */
        super.close();

    }
}
