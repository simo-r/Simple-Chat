import java.io.IOException;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class RequestService extends Service{
    public final int MULTICAST_PORT;
    private final MulticastSocket mcs;

    /**
     * Inizializza le variabili d'istanza per le operazioni di richiesta
     * e le strutture dati condivise richieste.
     *
     * @param users struttura dati contenente le info sugli utenti.
     * @param groups struttura dati contenente le info sui gruppi.
     * @param mcs socket per i gruppi multicast.
     * @param mcPort porta per i gruppi multicast.
     */
    public RequestService(ConcurrentHashMap<String, UserOperation> users,
                          ConcurrentHashMap<String,Group> groups,
                          MulticastSocket mcs,int mcPort){
        super(users,groups);
        this.mcs = mcs;
        this.MULTICAST_PORT = mcPort;
    }

    /**
     * Inserisce un nuovo utente,se questo non è già presente,
     * all'interno della struttura dati contenente le info sugli
     * utenti.
     *
     * @param usr utente da registrare.
     * @return true se l'utente è stato registrato,
     *         false se l'username dell'utente era già stato registrato.
     */
    public boolean register(UserOperation usr){
        return users.putIfAbsent(usr.getUsr(), usr) == null;
    }

    /**
     * Setta il campo status dell'utente a LOGGING, cioè
     * l'utente non è ancora online ma sta completando la procedura
     * di login.
     *
     * @param usr utente da loggare.
     * @param psw password dell'utente da loggare.
     * @return OK se l'utente è stato loggato correttamente.
     *          WRGUSR se l'username non esiste.
     *          WRGPSW se la psw dell'user è non corretta.
     *          ALREADY_ONLINE se l'user è già online.
     */
    public SSCode login(String usr, String psw) {
        AtomicReference<SSCode> result = new AtomicReference<>(SSCode.WRGUSR);
        users.computeIfPresent(usr,(k,v)-> {
                    if(!v.checkPsw(psw))
                        result.set(SSCode.WRGPSW);
                    else if(!(v.getUsrStat().equals(UserStatus.OFFLINE)))
                        result.set(SSCode.ALREADY_ONLINE);
                    else {
                        /*
                         *  Setto come LOGGING e verrà messo ONLINE una volta ricevuto lo stub.
                         *  Questo evita che altri client con lo stesso username possano loggare
                         *  mentre ancora devo ricevere lo stub ma allo stesso tempo evita di mandare
                         *  notifiche su stub o chat socket vecchie/null.
                         */
                        v.setUsrStat(UserStatus.LOGGING);
                        result.set(SSCode.OK);
                    }
                    return v;
                }
        );
        return result.get();
    }

    /**
     * Aggiunge una relazione tra la sorgente e la destinazione
     * della richiesta e notifica la destinazione della nuova relazione.
     *
     * @param sourceUsr utente che invia la richiesta.
     * @param destUsr utente destinatario della richiesta.
     * @return OK se la richiesta è andata a buon fine.
     *         ALREADY_FRIENDS se sourceUsr e destUsr avevano già una relazione.
     *         WRGUSR se destUsr non esiste.
     */
    public SSCode addRelation(String sourceUsr, String destUsr){
        AtomicReference<SSCode> code = new AtomicReference<>(SSCode.ALREADY_FRIENDS);
        AtomicReference<String> sourceLang = new AtomicReference<>();
        if(searchUsr(destUsr)){
            /* Se esiste l'utente */
            users.computeIfPresent(sourceUsr,(k,v)->{
                sourceLang.set(v.getLang());
                if(v.addFriend(destUsr)){
                    code.set(SSCode.OK);
                }
                return v;
            });

        }else{
            code.set(SSCode.WRGUSR);
        }
        if(code.get().equals(SSCode.OK)){
            /* Aggiungo il follower e notifico la nuova amicizia. */
            users.computeIfPresent(destUsr,(k,v) ->{
                v.addFollower(sourceUsr);
                v.notifyNewFriend(sourceUsr,sourceLang.get());
                return v;
            });
        }
        return code.get();
    }

    /**
     * Controlla se l'utente esiste.
     *
     * @param usr utente da ricercare.
     * @return true se l'utente esiste, false altrimenti.
     */
    public boolean searchUsr(String usr){
        return users.containsKey(usr);
    }

    /**
     * Ritorna la lingua predefinita dell'utente.
     *
     * @param usr nome dell'utente.
     * @return lingua predefinita dell'utente.
     */
    public String getUserLang(String usr){
        AtomicReference<String> lang = new AtomicReference<>();
        users.computeIfPresent(usr,(k,v)->{
            lang.set(v.getLang());
            return v;
        });
        return lang.get();
    }

    /**
     * Ritorna la lista degli amici dell'utente.
     *
     * @param usr utente da cui prelevare la lista amici.
     * @return lista degli amici.
     */
    public List<String> getFriendsList(String usr) {
        ArrayList<String> tmpFriends = new ArrayList<>();
        users.computeIfPresent(usr,(k,v)->{
            tmpFriends.addAll(v.getFriendsList());
            return v;
        });
        return tmpFriends;
    }

    /**
     * Crea un nuovo gruppo ed imposta l'utente
     * creatore come admin.
     *
     * @param admin amministratore del gruppo.
     * @param groupName nome del gruppo.
     * @return OK se la creazione del gruppo va a buon fine.
     *         ALRDY_EXISTS se il nome del gruppo esiste già.
     *         GRP_FAIL se viene sollevata un'eccezione nella
     *                  creazione del gruppo.
     * @implNote se viene catturata un'eccezione non viene
     *           inserito alcuna chiave e/o valore all'interno
     *           della tabella hash.
     */
    public SSCode createGroup(String admin, String groupName){
        AtomicReference<SSCode> result = new AtomicReference<>(SSCode.ALRDY_EXISTS);
        groups.computeIfAbsent(groupName, (String v) -> {
            Group tmp = null;
            try {
                tmp = new Group(admin,groupName);
                mcs.joinGroup(tmp.getInetAddr());
                result.set(SSCode.OK);
            } catch (IOException e) {
                result.set(SSCode.GRP_FAIL);
                System.err.printf("IO EXCEPTION IN GROUP CREATION: %s\n",groupName);
            }
            return tmp;

        });
        /* Se l'aggiunta è andata a buon fine, aggiungo il gruppo all'utente. */
        if(result.get().equals(SSCode.OK)){
            users.computeIfPresent(admin,(k,v)->{
                v.addGroup(groupName);
                return v;
            });
        }else if(result.get().equals(SSCode.GRP_FAIL)){
            groups.remove(groupName);
        }
        return result.get();
    }

    /**
     * Aggiunge l'utente al gruppo.
     *
     * @param usr nuovo utente del gruppo.
     * @param groupName nome del gruppo in cui vuole entrare.
     * @return OK se l'aggiunta va a buon fine.
     *         GRP_NOT_EXIST se il gruppo non esiste.
     *         USR_ALRDY_GRP se l'utente faceva
     *                       già parte del gruppo.
     *
     */
    public SSCode joinGroup(String usr, String groupName){
        AtomicReference<SSCode> result = new AtomicReference<>(SSCode.GRP_NOT_EXIST);
        groups.computeIfPresent(groupName,(k,v)->{
            result.set(v.joinGroup(usr));
            return v;
        });
        if(result.get().equals(SSCode.OK)){
            users.computeIfPresent(usr,(k,v)->{
                v.addGroup(groupName);
                return v;
            });
        }
        return result.get();
    }

    /**
     * Ritorna la lista dei gruppi a cui appartiene
     * l'utente.
     *
     * @param usr nome dell'utente.
     * @return lista contenente i gruppi di cui fa
     *         parte.
     */
    public ArrayList<String> getUsrGroupList(String usr){
        ArrayList<String> tmpList = new ArrayList<>();
        users.computeIfPresent(usr,(k,v)->{
            tmpList.addAll(v.getGroups());
            return v;
        });
        return tmpList;
    }

    /**
     * Ritorna una tabella hash contenente i nomi
     * dei gruppi esistenti.
     *
     * @return hashset contenente i nomi di tutti i gruppi.
     * @implNote il forEach garantisce che sia thread safe
     *           anche se weakly consistent.
     */
    public HashSet<String> getGrpList(){
        HashSet<String> tmpGroups = new HashSet<>();
        groups.forEach((k,v) -> tmpGroups.add(k));
        return tmpGroups;
    }

    /**
     * Chiude (elimina) il gruppo e notifica tutti i membri.
     *
     * @param usr utente che richiede l'operazione.
     * @param groupName nome del gruppo.
     * @return OK se l'operazione di chiusura va a buon fine.
     *         GRP_NOT_EXIST se il gruppo non esiste.
     *         GRP_SEND_FAIL se l'invio della notifica ai membri fallisce.
     *         GRP_USR_NOT_ADMIN se l'utente che richiede la
     *                           chiusura non è l'admin.
     */
    public SSCode closeGroup(String usr, String groupName){
        AtomicReference<SSCode> result = new AtomicReference<>(SSCode.GRP_NOT_EXIST);
        groups.computeIfPresent(groupName,(k,v)->{
            if(v.getAdmin().equals(usr)){
                result.set(v.notifyClose(mcs,MULTICAST_PORT));
                if(result.get().equals(SSCode.OK)){
                    /* Rimuovo il gruppo da ogni membro. */
                    for(String member : v.getMembers()){
                        users.computeIfPresent(member,(k1,v1)->{
                            v1.removeGroup(k);
                            return v1;
                        });
                    }
                }
            }else{
                result.set(SSCode.GRP_USR_NOT_ADMIN);
            }
            return v;
        });
        if(result.get().equals(SSCode.OK)){
            groups.remove(groupName);
        }
        return result.get();
    }

    /**
     * Ritorna l'indirizzo IP multicast del gruppo.
     *
     * @param groupName nome del gruppo.
     * @return stringa contenente l'IP multicast del gruppo.
     */
    public String getGroupIp(String groupName) {
        AtomicReference<String> ip = new AtomicReference<>();
        groups.computeIfPresent(groupName,(k,v) ->{
            ip.set(v.getMulticastIp());
            return v;
        });
        return ip.get();
    }

    /**
     * Incrementa il numero di utenti online nel gruppo.
     *
     * @param grp gruppo su cui inserirlo.
     * @param usr utente da inserire come online.
     */
    public void incrOnlineUsrGrp(String grp,String usr) {
        groups.computeIfPresent(grp,(k,v)->{
            v.incrOnline(usr);
            return v;
        });
    }

    /**
     * Imposta lo status dell'utente ad offline e
     * decrementa il numero di utenti online all'interno
     * dei gruppi di cui fa parte.
     *
     * @param usr utente che si è disconnesso.
     */
    public void putUsrOffline(String usr){
        if(usr != null) {
            AtomicBoolean result = new AtomicBoolean();
            ArrayList<String> followers = new ArrayList<>();
            users.computeIfPresent(usr, (k, v) -> {
                result.set(v.setUsrStat(UserStatus.OFFLINE));
                if (result.get()) {
                    System.out.println("User: " + v.getUsr() + " Status: " + v.getUsrStat());
                    followers.addAll(v.getFollowers());
                    for (String grp : v.getGroups()) {
                        groups.computeIfPresent(grp, (k1, v1) -> {
                            v1.decrOnline(usr);
                            return v1;
                        });
                    }
                }
                return v;
            });
            if (result.get()) {
                friendStatusChange(usr, followers, UserStatus.OFFLINE.toString());
            }
        }
    }
}
