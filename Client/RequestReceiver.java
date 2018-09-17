import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.ArrayList;

class RequestReceiver extends Receiver implements Runnable {
    /* Gestisce le risposte alle richieste effettuate e i gruppi multicast. */
    private UserGroupInfo userInfo;
    private MulticastSocket mcs;
    private Thread chatRoomReceiver;

    /**
     * Inizializza il receiver sulla socket per le richieste,
     * la socket multicast per i gruppi, le informazioni
     * sull'utente corrente.
     *
     * @param reader lato in lettura sulla socket di richiesta.
     * @param userInfo informazioni sull'utente corrente.
     * @param mcGroupPort porta per i gruppi multicast.
     * @throws IOException se la creazione della multicast
     *                     socket fallisce.
     */
    public RequestReceiver(BufferedReader reader,UserGroupInfo userInfo, int mcGroupPort) throws IOException{
        super(reader);
        this.mcs = new MulticastSocket(mcGroupPort);
        mcs.setReuseAddress(true);
        mcs.setSoTimeout(Client.JOIN_TIME);
        MulticastGroupReceiver gr = new MulticastGroupReceiver(mcs,userInfo);
        this.userInfo = userInfo;
        chatRoomReceiver = new Thread(gr);
    }

    /**
     * Starta un nuovo thread in ricezione sulla
     * socket multicast dei gruppi e riceve le
     * risposte inviate sulla connessione di
     * richiesta dal server.
     */
    public void run() {
        joinGroups();
        chatRoomReceiver.start();
        JSONObject obj;
        String msg;
        ArrayList<String> arr;
        Thread currentThread = Thread.currentThread();
        try{
            while(!currentThread.isInterrupted()) {
                try{
                obj = receive();
                }catch (ParseException e){
                    System.err.println("PARSE EXCEPTION REQUEST RECEIVER");
                    /* Non termino ma riprovo a leggere */
                    continue;
                }
                if(obj == null) break;
                /* Il messaggio è sempre presente in tutte le risposte. */
                msg = (String) obj.get(MSG);
                System.out.println(msg);
                switch ((String) obj.get(TYPE)) {
                    case ACK:
                        break;
                    case NACK:
                        break;
                    case FRNDACK:
                        /* Ricezione lista amici. */
                        arr = (ArrayList<String>) obj.get(LIST);
                        userInfo.setFriendsList(arr);
                        for (String friend : arr) {
                            System.out.printf("Friend: %s\n", friend);
                        }
                        break;
                    case GRPACK:
                        /* ACK per la creazione o join di un gruppo. */
                        String groupName = (String) obj.get(GRPNAME);
                        String ip = (String) obj.get(IP);
                        /* getByName può generare UnknHostEx che fa terminare il receiver. */
                        InetAddress tmpAddr = InetAddress.getByName(ip);
                        mcs.joinGroup(tmpAddr);
                        userInfo.addGroup(groupName, tmpAddr);
                        break;
                    case GRPLST:
                        arr = (ArrayList<String>) obj.get(LISTUSRGRP);
                        for(String group : arr){
                            System.out.printf("Group: %s [JOINED]\n",group);
                        }
                        arr = (ArrayList<String>) obj.get(LIST);
                        for(String group : arr){
                            System.out.printf("Group: %s [NOT JOINED]\n",group);
                        }
                        break;
                    default:
                        System.err.println("DEFAULT " + obj.get(TYPE));
                        break;
                }
            }
        }catch (SocketException e){
            /* Lanciata alla chiusura della socket. */
            System.err.println("SOCKET EXCEPTION REQUEST RECEIVER THREAD");
        }catch (IOException e) {
            System.err.println("IO EXCEPTION REQUEST RECEIVER THREAD");
        }finally {
            close();
        }
    }

    /**
     * Esce da tutti i gruppi multicast in cui era,
     * chiude la multicastsocket ed interrompe il thread
     * in ricezione sui gruppi.
     */
    private void close(){
        ArrayList<InetAddress> groupAddr = userInfo.getGroupsAddr();
        for(InetAddress addr : groupAddr){
            try {
                mcs.leaveGroup(addr);
            } catch (IOException e) {
                System.err.println("UNABLE TO LEAVE A GROUP");
            }
        }
        mcs.close();
        chatRoomReceiver.interrupt();
        try {
            chatRoomReceiver.join(60000);
        } catch (InterruptedException e) {
            System.err.println("INTERRUPTED WHILE WAITING FOR CHAT ROOM RECEIVER");
        }
    }

    /**
     * Joina tutti i gruppi multicast subito dopo il login,
     * non appena viene startato il thread corrente.
     */
    private void joinGroups(){
        for(InetAddress addr : userInfo.getGroupsAddr()){
            try {
                mcs.joinGroup(addr);
            } catch (IOException e) {
                System.err.println("MULTICAST GROUP JOIN FAILED");
            }
        }
    }
}
