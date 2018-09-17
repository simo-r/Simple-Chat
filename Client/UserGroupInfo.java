import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

class UserGroupInfo extends UserInfo {
    /* Estensione delle info dell'utente con i gruppi. */
    private final ConcurrentHashMap<String,InetAddress> groups;

    /**
     * Inizializza le informazioni sull'utente.
     *
     * @param usr nome dell'utente.
     * @param lang lingua predefinita dall'utente.
     */
    public UserGroupInfo(String usr, String lang) {
        super(usr,lang);
        groups = new ConcurrentHashMap<>();
    }

    /**
     * Ritorna una lista degli INET address dei gruppi
     * di cui l'utente fa parte.
     *
     * @return lista di INET address.
     */
    public ArrayList<InetAddress> getGroupsAddr(){
        return new ArrayList<>(groups.values());
    }

    /**
     * Aggiunge un nuovo gruppo associandogli un
     * indirizzo multicast.
     *
     * @param groupName nome del gruppo da aggiungere.
     * @param addr INET address del gruppo.
     */
    public void addGroup(String groupName, InetAddress addr) {
        groups.put(groupName,addr);
    }

    /**
     * Rimuove un gruppo dalla lista e ritorna
     * il suo INET address.
     *
     * @param grpName nome del gruppo da rimuovere.
     * @return INET address del gruppo, null se
     *         non vi era alcun gruppo con quel nome.
     */
    public InetAddress removeGroup(String grpName){
        return groups.remove(grpName);
    }
}
