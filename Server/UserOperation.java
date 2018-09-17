import org.json.simple.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
/**
 * Contiene le info dell'utente lato server, nonchè la socket
 * per i messaggi di chat e lo stub per le notifiche RMI.
 */
class UserOperation extends UserInfo implements ReplyCodeServer{

    private final String psw;
    private final HashSet<String> followers;
    private final HashSet<String> groups;
    private volatile UserStatus status;
    private BufferedWriter chatWriter;
    private ChatEvent stubCE;

    /**
     * Inizializza le informazioni sull'utente.
     *
     * @param usr nome dell'utente.
     * @param psw password dell'utente.
     * @param lang lingua predefinita dell'utente.
     */
    public UserOperation(String usr, String psw, String lang){
        super(usr,lang);
        this.psw = psw;
        this.status = UserStatus.OFFLINE;
        this.groups = new HashSet<>();
        this.followers = new HashSet<>();
    }

    /**
     * Ritorna lo status dell'utente.
     *
     * @return status dell'utente.
     */
    public UserStatus getUsrStat(){
        return this.status;
    }

    public Set<String> getFollowers(){
        return this.followers;
    }

    /**
     * Imposta lo status dell'utente.
     *
     * @param status nuovo status.
     * @return true se lo status è stato cambiato,
     *         false se era offline e il nuovo status è offline.
     * @implNote la guardia serve ad evitare che più di un thread esegua
     *           la procedura per mettere un utente offline.
     */
    public boolean setUsrStat(UserStatus status){
        boolean result;
        if(this.status.equals(UserStatus.OFFLINE) &&
                status.equals(UserStatus.OFFLINE))
            result = false;
        else {
            this.status = status;
            result = true;
        }
        return result;
    }

    /**
     * Controlla che la password corrisponda
     * al parametro.
     *
     * @param psw possibile password dell'utente.
     * @return true se le password combaciano,
     *         false altrimenti.
     */
    public boolean checkPsw(String psw) {
        return this.psw.equals(psw);
    }

    /**
     * Inizializza il lato in scrittura della chat socket.
     *
     * @param chatWriter lato in scrittura della chat socket.
     */
    public void setChatWriter(BufferedWriter chatWriter){
        this.chatWriter = chatWriter;
    }

    /**
     * Modifica lo status dell'utente ad ONLINE e
     * aggiorna lo stub per le callback.
     *
     * @param loginCallBack stub del client per le callback.
     * @implNote l'utente va impostato ONLINE quando ricevo il
     *           nuovo stub altrimenti potrei avere delle inconsistenze
     *           con stub o chatWriter vecchi/nulli.
     */
    public void setLoginCallBack(ChatEvent loginCallBack) {
        this.stubCE = loginCallBack;
        this.status = UserStatus.ONLINE;
    }

    /**
     * Aggiunge, se non è già presente, l'utente
     * passato come parametro alla lista amici.
     *
     * @param usr utente da aggiungere alla lista amici.
     * @return true se l'utente è stato aggiunto alla lista amici
     *         false se l'utente è già nella lista amici.
     */
    public boolean addFriend(String usr){
        boolean result = false;
        if(!friends.contains(usr)){
            friends.add(usr);
            result = true;
        }
        return result;
    }

    /**
     * Aggiunge un follower all'utente cioè
     * un utente che lo ha tra gli amici.
     *
     * @param usr follower da aggiungere.
     */
    public void addFollower(String usr){
        if(!followers.contains(usr)){
            followers.add(usr);
        }
    }

    /**
     * Notifica, attraverso callback RMI, l'utente corrente che è stato aggiunto da un altro
     * utente come amico.
     *
     * @param newFriend utente che ha aggiunto una nuova relazione.
     * @param newFriendLang lingua dell'utente che ha aggiunto una nuova relazione.
     */
    public void notifyNewFriend(String newFriend,String newFriendLang) {
        if(status.equals(UserStatus.ONLINE) && stubCE != null){
            try {
                stubCE.notifyNewFriend(newFriend,newFriendLang);
                System.out.printf("[NEW FRIEND] user: %s new friend: %s\n",getUsr(),newFriend);
            } catch (RemoteException e) {
                System.out.println("EXCEPTION IN NOTIFY NEW FRIEND TO :" + getUsr() + " NEW FRIEND : " + newFriend);
            }
        }
    }

    /**
     * Notifica, attraverso callback RMI, l'utente corrente che
     * un amico ha cambiato status.
     *
     * @param usr utente amico che ha cambiato status.
     * @param newStatus nuovo status dell'amico.
     */
    public void notifyFriendStatusChange(String usr, String newStatus){
        if(status.equals(UserStatus.ONLINE) && stubCE != null){
            try{
                stubCE.notifyFriendStatusChange(usr,newStatus);
                System.out.printf("[FRIEND STATUS CHANGE] user: %s friend: %s\n",getUsr(),usr);
            } catch (RemoteException e) {
                System.out.println("EXCEPTION IN NOTIFY STATUS CHANGE TO :" + getUsr() + " USER : " + usr);
            }
        }
    }

    /**
     * Manda un messaggio di chat dal mittente
     * al destinatario (this).
     *
     * @param sourceUsr mittente del messaggio.
     * @param msg body del messaggio.
     * @return OK se l'invio va a buon fine.
     *         OFFLINE se il destinatario (this) è offline.
     *         NOT_FRIENDS se il destinatario (this) e
     *                     il mittente non sono amici.
     */
    public SSCode sendChatMsg(String sourceUsr, String msg){
        /* Ho preferito un'early return in quanto gli if sarebbero meno leggibili. */
        if(status.equals(UserStatus.OFFLINE)) return SSCode.OFFLINE;
        if(!friends.contains(sourceUsr)) return SSCode.NOT_FRIENDS;
        JSONObject obj = new JSONObject();
        obj.put(TYPE,CHATMSG);
        obj.put(FROM,sourceUsr);
        obj.put(MSG,msg);
        if(!send(obj)) return SSCode.OFFLINE;
        return SSCode.OK;

    }

    /**
     * Manda una richiesta di invio file dal mittente
     * al destinatario (this).
     *
     * @param sourceUsr mittente della richiesta.
     * @param fileName nome del file da inviare.
     * @param len lunghezza del file.
     * @param ide identificatore univoco del file.
     * @return OK se l'invio della richiesta va a buon fine.
     *         OFFLINE se il destinatario (this) è offline.
     *         NOT_FRIENDS se il destinatario (this) e
     *                     il mittente non sono amici.
     */
    public SSCode requestFileMsg(String sourceUsr, String fileName, Long len, Long ide){
        if(status.equals(UserStatus.OFFLINE)) return SSCode.OFFLINE;
        if(!friends.contains(sourceUsr)) return SSCode.NOT_FRIENDS;
        JSONObject obj = new JSONObject();
        obj.put(TYPE,FILEMSG);
        obj.put(FROM,sourceUsr);
        obj.put(FILENAME,fileName);
        obj.put(IDE,ide);
        obj.put(LEN,len);
        if(!send(obj)) return SSCode.OFFLINE;
        return SSCode.OK;
    }

    /**
     * Se l'utente è online manda le informazioni
     * su come connettersi al destinatario del file.
     *
     * @param ip ip del destinatario.
     * @param port porta del destinatario.
     * @param ide identificatore univoco del file.
     * @implNote se il mittente (this) è andato offline
     *           non informo il destinatario del file in
     *           quanto rimarrà bloccato sulla accept()
     *           per al più TIMEOUT tempo.
     */
    public void sendSocketInfo(String ip, Long port, Long ide) {
        if(!status.equals(UserStatus.OFFLINE)){
            JSONObject obj = new JSONObject();
            obj.put(TYPE,SOCKETINFO);
            obj.put(IP,ip);
            obj.put(PORT,port);
            obj.put(IDE,ide);
            send(obj);
        }
    }

    /**
     * Aggiunge un gruppo alla lista dei gruppi di cui
     * fa parte l'utente.
     *
     * @param groupName nome del gruppo da aggiungere.
     */
    public void addGroup(String groupName){
        groups.add(groupName);
    }

    /**
     * Rimuove, se esiste, il gruppo dalla lista dei gruppi
     * a cui appartiene l'utente.
     *
     * @param groupName nome del gruppo da rimuovere.
     */
    public void removeGroup(String groupName) { groups.remove(groupName);}

    /**
     * Ritorna la lista contenente i nomi dei gruppi
     * di cui l'utente fa parte.
     *
     * @return lista contenente i nomi dei gruppi.
     */
    public ArrayList<String> getGroups(){
        return new ArrayList<>(groups);
    }

    /**
     * Invia un messaggio sulla chat socket.
     *
     * @param obj messaggio di chat da inviare al destinatario (this).
     * @return true se l'invio va a buon fine, false altrimenti.
     * @implNote il line separator va aggiunto in questo modo
     *           altrimenti l'operazione non è atomica/sincronizzata
     *           e potrebbe creare inconsistenze.
     */
    private boolean send(JSONObject obj){
        boolean result = true;
        try{
            chatWriter.write(obj.toJSONString() +System.getProperty("line.separator"));
            chatWriter.flush();
            System.out.printf("Send to: %s JSON: %s\n",getUsr(),obj.toJSONString());
        } catch (IOException e) {
            System.err.printf("FAILED TO SEND TO: %s\n",getUsr());
            result = false;
        }
        return result;
    }
}
