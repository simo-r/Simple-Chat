import java.rmi.Remote;
import java.rmi.RemoteException;

interface ChatEvent extends Remote {
    /* Interfaccia per RMI. */

    /**
     * Notifica l'utente che è stato aggiunto alla lista
     * amici di un altro utente.
     *
     * @param usrname nome del nuovo amico.
     * @param usrLang lingua del nuovo amico.
     * @throws RemoteException errore nella comunicazione remota.
     */
    void notifyNewFriend(String usrname, String usrLang) throws RemoteException;

    /**
     * Notifica l'utente che uno dei suoi amici
     * ha cambiato status.
     *
     * @param usrname nome dell'utente.
     * @param status status dell'utente.
     * @throws RemoteException errore nella comunicazione remota.
     */
    void notifyFriendStatusChange(String usrname, String status) throws RemoteException;
}
