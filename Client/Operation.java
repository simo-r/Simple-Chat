import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

interface Operation {
    Set<String> ISO_LANG = new HashSet<>(Arrays.asList(Locale.getISOLanguages()));

    /**
     * Invia la richiesta al server per la creazione
     * di un nuovo utente con nome, password e lingua predefinita.
     *
     * @param usr nome dell'utente da registrare.
     * @param psw password associata all'utente.
     * @param lang lingua associata all'utente.
     */
    void register(String usr, String psw, String lang);

    /**
     * Invia la richiesta al server per il login
     * dell'utente. Se il login va a buon fine
     * inizializza i vari receiver e le altre operazioni
     * consentite ed invia lo stub per le notifiche (RMI).
     *
     * @param usr nome dell'utente da loggare.
     * @param psw password dell'utente da loggare.
     * @return true se il login va a buon fine, false altrimenti.
     */
    boolean login(String usr, String psw);

    /**
     * Invia la richiesta al server per l'aggiunta di
     * un nuovo amico alla lista amici.
     *
     * @param usr nome dell'utente da aggiungere agli amici.
     */
    void addFriend(String usr);

    /**
     * Invia una richiesta al server per la ricerca
     * di un username.
     *
     * @param usr nome dell'utente da ricercare.
     */
    void searchUser(String usr);

    /**
     * Stampa la lista aggiornata degli amici dell'utente.
     */
    void friendsList();

    /**
     * Invia un messaggio di chat all'amico destinatario.
     *
     * @param destUsr destinatario del messaggio.
     * @param msg body del messaggio.
     */
    void msgToUsr(String destUsr, String msg);

    /**
     * Invia una richiesta al server di invio file
     * all'amico destinatario.
     *
     * @param destUsr destinatario del file.
     * @param fileName nome del file.
     */
    void fileTo(String destUsr, String fileName);

    /**
     * Invia una richiesta al server per creare un
     * gruppo con il nome specificato dal parametro.
     *
     * @param groupName nome del nuovo gruppo da creare.
     */
    void createGroup(String groupName);

    /**
     * Invia una richiesta al server per entrare
     * a far parte del gruppo.
     *
     * @param groupName nome del gruppo in cui si vuole entrare.
     */
    void joinGroup(String groupName);

    /**
     * Invia una richiesta al server
     * per ricevere la lista dei gruppi
     * di cui l'utente fa parte.
     */
    void groupList();

    /**
     * Invia una richiesta al server
     * per eliminare il gruppo.
     *
     * @param groupName nome del gruppo.
     */
    void closeGroup(String groupName);

    /**
     * Invia una richiesta al server di inoltro
     * del messaggio sul gruppo indicato.
     *
     * @param destGrp gruppo destinatario del messaggio.
     * @param msg body del messaggio.
     */
    void msgToGroup(String destGrp, String msg);

    /**
     * Chiude le socket e tutti i thread definiti
     * nel client e lo termina con un exit status.
     * Se lo status è diverso da 0 il client è
     * terminato in maniera anomala.
     *
     * @param status status della terminazione.
     */
    void close(int status);
}
