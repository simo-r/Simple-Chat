import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemChatTask extends UnicastRemoteObject implements RemChat {
    private transient RemChatService remChatOp;

    /**
     * Inizializza ed esporta l'istanza dell'Unicast Remote Object
     * utilizzando una porta anonima.
     *
     * @param remChatOp implementazioni delle operazioni remote.
     * @throws RemoteException se fallisce la comunicazione remota.
     */
    public RemChatTask(RemChatService remChatOp) throws RemoteException {
        super();
        this.remChatOp = remChatOp;
    }

    /* Descrizione nell'interfaccia. */
    public void registerLogin(String usr, ChatEvent client){
        System.out.printf("[LOGIN CALLBACK] from: %s stub: %s\n",usr,client.toString());
        remChatOp.addLoginCallback(usr,client);
    }

}
