import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

class GroupTask implements Runnable,ReplyCodeServer {

    private JSONObject obj;
    private final GroupService groupOp;
    private final InetAddress destAddr;
    private final int destPort;

    /**
     * Inizializza un nuovo task che si occuperà di
     * eseguire la richiesta di un utente e l'invio di una
     * risposta utilizzando, sempre, UDP.
     *
     * @param obj richiesta del mittente.
     * @param groupOp implementazione delle operazioni sui gruppi.
     * @param destAddr indirizzo del mittente della richiesta.
     * @param destPort porta del mittente della richiesta.
     */
    public GroupTask(JSONObject obj, GroupService groupOp, InetAddress destAddr, int destPort){
        this.obj = obj;
        this.groupOp = groupOp;
        this.destAddr = destAddr;
        this.destPort = destPort;
    }

    /**
     * Esegue l'operazione richiesta.
     */
    public void run() {
        switch((String)obj.get(TYPE)){
            case GRPMSG:
                String destGrp = (String) obj.get(GRPNAME);
                String from = (String) obj.get(FROM);
                String msg = (String) obj.get(MSG);
                sendGrpMsg(from,destGrp,msg);
                break;
            default:
                System.out.println("GROUP TASK DEFAULT");
                break;
        }
    }

    /**
     * Invia il messaggio al gruppo indicato ed una risposta
     * al mittente sempre attraverso UDP.
     *
     * @param from mittente del messaggio.
     * @param destGrp gruppo a cui inviare il messaggio.
     * @param msg messaggio.
     */
    private void sendGrpMsg(String from, String destGrp,String msg){
        SSCode result = groupOp.sendGrpMsg(from,destGrp,msg);
        obj = new JSONObject();
        switch (result) {
            case OK:
                System.out.printf("[GROUP MESSAGE] from: %s to group: %s msg: %s RESULT: ACK\n",from,destGrp,msg);
                /* La risposta è stata inviata tramite multicast. */
                break;
            case GRP_NO_USR:
                System.out.printf("[GROUP MESSAGE] from: %s to group: %s msg: %s RESULT: NOT IN GROUP\n",from,destGrp,msg);
                obj.put(TYPE, NACK);
                obj.put(MSG, usrNotInGrp(destGrp));
                break;
            case GRP_NO_ON_USR:
                System.out.printf("[GROUP MESSAGE] from: %s to group: %s msg: %s RESULT: NO ONLINE USER\n",from,destGrp,msg);
                obj.put(TYPE, NACK);
                obj.put(MSG, noOnUsr(destGrp));
                break;
            case GRP_NOT_EXIST:
                System.out.printf("[GROUP MESSAGE] from: %s to group: %s msg: %s RESULT: NOT EXISTS\n",from,destGrp,msg);
                obj.put(TYPE, NACK);
                obj.put(MSG, grpNotExist(destGrp));
                break;
            case GRP_SEND_FAIL:
                System.out.printf("[GROUP MESSAGE] from: %s to group: %s msg: %s RESULT: SEND FAILED\n",from,destGrp,msg);
                obj.put(TYPE, NACK);
                obj.put(MSG, grpSendFail(destGrp));
                break;
        }
        try {
            if(!result.equals(SSCode.OK)) send(obj);
        }catch (SocketException e){
            System.err.printf("SOCKET EXCEPTION GROUP TASK. GROUP: %s FROM: %s\n",destGrp,from);
        }catch (IOException e){
            System.err.printf("IOEXCEPTION GROUP TASK. GROUP: %s FROM: %s\n",destGrp,from);
        }
    }

    /**
     * Invia una pacchetto UDP al mittente contenente
     * il risultato dell'operazione richiesta .
     *
     * @param obj risposta da inviare.
     * @throws SocketException se la socket non può essere aperta o legata ad una porta.
     * @throws IOException se è impossibile inviare il pacchetto.
     */
    private void send(JSONObject obj) throws IOException{
        byte[] json  = obj.toJSONString().getBytes(StandardCharsets.UTF_8);
        DatagramPacket sndPckt = new DatagramPacket(json, json.length, destAddr, destPort);
        DatagramSocket ds = new DatagramSocket();
        ds.send(sndPckt);
    }
}
