import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.*;
import java.nio.channels.ClosedByInterruptException;
import java.nio.charset.StandardCharsets;


class MulticastGroupReceiver implements Runnable,ReplyCode{
    private final MulticastSocket mcs;
    private final UserGroupInfo userInfo;

    /**
     * Inizializza le informazioni per ricevere
     * le informazioni che riguardano i gruppi multicast
     * che ha joinato.
     *
     * @param mcs multicast socket per i gruppi.
     * @param userInfo informazioni sull'utente corrente.
     */
    public MulticastGroupReceiver(MulticastSocket mcs,UserGroupInfo userInfo){
        this.mcs = mcs;
        this.userInfo = userInfo;
    }

    /**
     * Riceve un messaggio da parte del server su
     * uno dei gruppi multicast che ha joinato.
     *
     * @implNote tutti i gruppi utilizzano la stessa porta
     *           con differenti IP multicast, questo permette
     *           di utilizzare una sola receive che intercetta
     *           tutti i pacchetti destinati a quella porta
     *           e a tutti gli indirizzi multicast che ha joinato.
     */
    public void run(){
        byte[] buf = new byte[GroupImpl.MAX_UDP_PCKT_LEN];
        DatagramPacket recv;
        String json;
        JSONParser parser = new JSONParser();
        JSONObject obj;
        String grpName;
        String from;
        String msg;
        Thread currentThread = Thread.currentThread();
        while(!currentThread.isInterrupted()) {
            recv = new DatagramPacket(buf, buf.length);
            try {
                mcs.receive(recv);
                json = new String(recv.getData(),0,recv.getLength(), StandardCharsets.UTF_8);
                obj = (JSONObject) parser.parse(json);
                switch ((String) obj.get(TYPE)){
                    case GRPMSG:
                        grpName = (String) obj.get(GRPNAME);
                        from = (String) obj.get(FROM);
                        if(from.equals(userInfo.getUsr())) from = userInfo.getUsr();
                        msg = (String) obj.get(MSG);
                        System.out.printf("[GROUP: %s] %s: %s\n",grpName,from,msg);
                        break;
                    case GRPCLOSE:
                        grpName = (String) obj.get(GRPNAME);
                        msg = (String) obj.get(MSG);
                        System.out.println(msg);
                        InetAddress addr = userInfo.removeGroup(grpName);
                        if(addr!=null) {
                            mcs.leaveGroup(addr);
                        }
                        break;
                    default:
                        break;
                }
            } catch (SocketException e){
                /* Lanciata alla chiusura della mutlicast socket. */
                System.err.println("SOCKET EXCEPTION MULTICAST RECEIVER THREAD");
            }catch (SocketTimeoutException | ClosedByInterruptException e) {
                System.err.println("SOCKET TIMEOUT EXCEPTION MUTLICAST RECEIVER THREAD");
            }catch (IOException e) {
                System.err.println("IO EXCEPTION MULTICAST RECEIVER THREAD ");
            } catch (ParseException e) {
                System.err.println("PARSE EXCEPTION MULTICAST RECEIVER THREAD");
            }
        }
    }
}
