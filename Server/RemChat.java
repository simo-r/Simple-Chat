import java.rmi.Remote;
import java.rmi.RemoteException;

interface RemChat extends Remote {
    /* Interfaccia per la registrazione di un client alle callback. */
    String SERVICE_NAME = "REM_CHAT";

    /**
     * Registra il client per ricevere notifiche.
     *
     * @param usr nome dell'utente a cui associare lo stub.
     * @param client stub del client.
     * @throws RemoteException errore nella comunicazione remota.
     */
    void registerLogin(String usr, ChatEvent client) throws RemoteException;
}
