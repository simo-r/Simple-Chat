import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

class Service {
    final ConcurrentHashMap<String, UserOperation> users;
    ConcurrentHashMap<String,Group> groups;

    /**
     * Inizializza le variabili d'istanza con la struttura dati contenente
     * le info degli utenti e le info dei gruppi.
     *
     * @param users struttura dati contenente le info degli utenti.
     * @param groups struttura dati contenente le info dei gruppi.
     */
    Service(ConcurrentHashMap<String, UserOperation> users,
            ConcurrentHashMap<String, Group> groups){
        this(users);
        this.groups = groups;
    }

    /**
     * Inizializza la variabile d'istanza con la struttura dati contenente
     * le info degli utenti.
     *
     * @param users struttura dati contenente le info degli utenti.
     */
    Service(ConcurrentHashMap<String, UserOperation> users){
        this.users = users;
    }

    /**
     * Notifica tutti i follower (chi lo ha tra gli amici)
     * che l'utente ha cambiato status.
     *
     * @param usr utente che ha cambiato status.
     * @param followers lista dei follower dell'utente.
     * @param newStatus nuovo status.
     */
    void friendStatusChange(String usr, List<String> followers, String newStatus){
        for(String follower : followers){
            users.computeIfPresent(follower,(k,v)->{
                v.notifyFriendStatusChange(usr,newStatus);
                return v;
            });
        }
    }
}
