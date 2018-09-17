import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ChatEventImpl extends UnicastRemoteObject implements ChatEvent {

    /**
     * Genera i metodi base ed esporta l'oggetto automaticamente
     * in quanto estende UnicastRemoteObject.
     *
     * @throws RemoteException se c'è stato un errore nella comunicazione remota.
     */
    public ChatEventImpl()throws RemoteException {
        super();
    }

    /* Specifica nell'interfaccia. */
    public void notifyNewFriend(String usrname, String usrLang) {
        System.out.printf("%s (%s) added you as friend!\n",usrname,usrLang);
    }

    /* Specifica nell'interfaccia. */
    public void notifyFriendStatusChange(String usrname, String status){
        System.out.printf("%s is now %s!\n",usrname,status);
    }
}
