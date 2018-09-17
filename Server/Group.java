import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.Set;

class Group implements ReplyCodeServer {
    private static final int[] mcIp = new int[]{224,0,0,1};
    private final String name;
    private final String admin;
    private int onlineCounter;
    private HashMap<String,Boolean> member;
    private InetAddress groupAddr;

    /**
     * Crea un nuovo gruppo assegnandogli un indirizzo
     * multicast, un admin e una lista dei membri vuota.
     *
     * @param admin creatore del gruppo.
     * @param name nome del gruppo.
     * @throws IOException se non esiste l'IP associato al nome
     *                     oppure se sono finiti gli IP multicast.
     * @implNote lancerebbe anche UnknownHostException ma
     *           in questo caso non è possibile.
     */
    public Group(String admin,String name) throws IOException {
        this.name = name;
        this.admin = admin;
        String ip = Group.getNextMulticastIp();
        if(ip == null) throw new IOException();
        this.groupAddr = InetAddress.getByName(ip);
        member = new HashMap<>();
        member.put(admin,true);
        onlineCounter++;
    }

    /**
     * Restituisce un indirizzo IPv4 multicast
     * nell'intervallo [224.0.0.1 , 239.255.255.255].
     *
     * @return stringa contenente il prossimo indirizzo
     *         multicast da utilizzare oppure null
     *         se gli IP multicast sono terminati.
     */
    private static String getNextMulticastIp() {
        /* Se ho terminato gli IP multicast. */
        if(mcIp[0] == 240) return null;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            result.append(mcIp[i]);
            if (i != 3) result.append(".");
        }
        for (int i = 3; i > 0; i--) {
            if (mcIp[i] != 0 && (mcIp[i] % 255) == 0) {
                mcIp[i] = 0;
                mcIp[i - 1]++;
            } else {
                if (i == 3) mcIp[i]++;
            }
        }
        return result.toString();
    }

    /**
     * Ritorna l'IP address del gruppo multicast.
     *
     * @return IP address del gruppo.
     */
    public String getMulticastIp(){
        return this.groupAddr.getHostAddress();
    }

    /**
     * Ritorna l'IP address del gruppo multicast.
     *
     * @return IP address del gruppo.
     */
    public InetAddress getInetAddr(){ return this.groupAddr; }

    /**
     * Ritorna il nome del gruppo.
     *
     * @return nome del gruppo.
     */
    public String getGroupName(){ return this.name; }

    /**
     * Ritorna il nome dell'utente
     * che è admin del gruppo.
     *
     * @return nome dell'admin del gruppo.
     */
    public String getAdmin(){ return this.admin;}

    /**
     * Ritorna l'insieme degli utenti
     * che fanno parte del gruppo.
     *
     * @return insieme dei membri del gruppo.
     */
    public Set<String> getMembers(){ return this.member.keySet();}

    /**
     * Se l'utente era online decrementa il numero
     * di utenti online nel gruppo.
     *
     * @param usr nome dell'utente andato offline.
     */
    public void decrOnline(String usr){
        Boolean isOnline = member.get(usr);
        if( isOnline != null && isOnline.booleanValue()){
            System.out.printf("[GROUP: %s] DECR: %s online!\n",name,usr);
            member.put(usr,!isOnline);
            if(onlineCounter > 0) {
                this.onlineCounter--;
            }else{
                System.err.printf("NEGATIVE GROUP ONLINE COUNTER: %s\n",name);
            }
        }else{
            System.out.printf("[GROUP: %s] DECR: %s offline!\n",name,usr);
        }

    }

    /**
     * Se l'utente era offline incrementa il numero
     * di utenti online nel gruppo.
     *
     * @param usr nome dell'utente andato online.
     */
    public void incrOnline(String usr){
        Boolean isOnline = member.get(usr);
        if(isOnline != null && !(isOnline.booleanValue())){
            System.out.printf("[GROUP: %s] INCR: %s offline!\n",name,usr);
            member.put(usr,!isOnline);
            if(onlineCounter < member.size()){
                this.onlineCounter++;
            }else{
                System.err.printf("TOO POSITIVE GROUP ONLINE COUNTER: %s\n",name);
            }
        }else{
            System.out.printf("[GROUP: %s] INCR: %s online!\n",name,usr);
        }

    }

    /**
     * Inserisce l'utente nella lista dei membri
     * ed incrementa il numero di utenti online
     * se questo non vi era già.
     *
     * @param usr nuovo utente del gruppo.
     * @return OK se l'aggiunta va a buon fine.
     *         USR_ALRDY_GRP se l'utente faceva
     *                       già parte del gruppo.
     */
    public SSCode joinGroup(String usr){
        SSCode result = SSCode.OK;
        if(member.putIfAbsent(usr,true) != null) result = SSCode.USR_ALRDY_GRP;
        else{
            onlineCounter++;
        }
        return result;
    }

    /**
     * Invia il messaggio sul gruppo multicast.
     *
     * @param sourceUsr mittente del messaggio.
     * @param msg body del messaggio.
     * @param mcs socket multicast su cui inviare il messaggio.
     * @param port porta su cui inviare il messaggio.
     * @return OK se l'invio va a buon fine.
     *         GRP_NO_USR se il mittente non è nel gruppo.
     *         GRP_NO_ON_USR se nessun utente nel gruppo è online.
     *         GRP_SEND_FAIL se l'invio del messaggio fallisce.
     */
    public SSCode sendMsg(String sourceUsr, String msg, MulticastSocket mcs,int port){
        SSCode result = SSCode.OK;
        if(!member.containsKey(sourceUsr)) return SSCode.GRP_NO_USR;
        if(onlineCounter <= 1) return SSCode.GRP_NO_ON_USR;
        JSONObject obj = new JSONObject();
        obj.put(TYPE,GRPMSG);
        obj.put(GRPNAME,name);
        obj.put(FROM,sourceUsr);
        obj.put(MSG,msg);
        if(!send(mcs,port,obj)) result = SSCode.GRP_SEND_FAIL;
        return result;
    }

    /**
     * Notifica la chiusura del gruppo multicast
     * a tutti i partecipanti.
     *
     * @param mcs multicast socket.
     * @param port porta della multicast socket.
     * @return OK se l'invio della notifica va a buon fine.
     *         GRP_SEND_FAIL se l'invio della notifica fallisce.
     */
    public SSCode notifyClose(MulticastSocket mcs,int port){
        SSCode result = SSCode.OK;
        JSONObject obj = new JSONObject();
        obj.put(TYPE,GRPCLOSE);
        obj.put(GRPNAME,name);
        obj.put(MSG,grpClosed(name));
        if(!send(mcs,port,obj)) result = SSCode.GRP_SEND_FAIL;
        return result;
    }

    /**
     * Invia un pacchetto UDP al gruppo multicast.
     *
     * @param mcs multicast socket per il gruppi.
     * @param port porta su cui mandare il pacchetto.
     * @param obj "pacchetto" da mandare.
     * @return true se l'invio va a buon fine,
     *         false altrimenti.
     */
    private boolean send(MulticastSocket mcs,int port, JSONObject obj){
        boolean result = true;
        byte[] json  = obj.toJSONString().getBytes();
        DatagramPacket sndPckt = new DatagramPacket(json, json.length, groupAddr,port);
        try {
            mcs.send(sndPckt);
        } catch (IOException e) {
            System.err.println("IO EXCEPTION MULTICAST SEND");
            result = false;
        }
        return result;
    }
}
