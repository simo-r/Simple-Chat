import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

class RemChatService extends Service{

    /**
     * Inizializza la struttura dati condivisa contenente le informazioni
     * sugli utenti.
     *
     * @param users struttura dati contenente le info degli utenti.
     */
    public RemChatService(ConcurrentHashMap<String, UserOperation> users){
        super(users);
    }

    /**
     * Aggiunge lo stub per le notifiche all'utente e
     * notifica tutti gli amici che l'utente è online.
     *
     * @param usr utente a cui associare la callback.
     * @param client stub che contiene le callback.
     */
    public void addLoginCallback(String usr, ChatEvent client) {
        ArrayList<String> followers = new ArrayList<>();
        users.computeIfPresent(usr,(k,v) ->{
            v.setLoginCallBack(client);
            followers.addAll(v.getFollowers());
            return v;
        });
        super.friendStatusChange(usr,followers,UserStatus.ONLINE.toString());
    }
}
