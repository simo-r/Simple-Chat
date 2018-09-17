import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class UserInfo {
    /* Contiene le info dell'utente lato client. */
    private final String usr;
    private final String lang;
    final Set<String> friends;

    /**
     * Inizializza le informazioni sull'utente.
     *
     * @param usr nome dell'utente.
     * @param lang lingua predefinita dell'utente.
     */
    UserInfo(String usr, String lang){
        this.usr = usr;
        this.lang = lang;
        friends = new HashSet<>();

    }

    /**
     * Ritorna il nome dell'utente.
     *
     * @return nome dell'utente.
     */
    public String getUsr(){
        return this.usr;
    }

    /**
     * Ritorna la lingua predefinita dell'utente.
     *
     * @return lingua predefinita dell'utente.
     */
    public String getLang(){
        return this.lang;
    }

    /**
     * Ritorna la lista degli amici dell'utente.
     *
     * @return lista degli amici.
     */
    public ArrayList<String> getFriendsList(){
        return new ArrayList<>(friends);
    }

    /**
     * Inizializza la lista degli amici dell'utente.
     *
     * @param tmpFriendsList lista degli amici.
     */
    public void setFriendsList(List<String> tmpFriendsList){
        friends.addAll(tmpFriendsList);
    }

    /**
     * Controlla se usr compare all'interno della lista amici
     * dell'utente.
     *
     * @param usr nome dell'utente da controllare.
     * @return true se l'utente e usr sono amici,
     *         false altrimenti.
     */
    public boolean checkFriend(String usr){
        return friends.contains(usr);
    }
}
