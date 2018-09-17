import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

class Client {

    public static final int JOIN_TIME = 60000;
    private Socket requestServer;
    private final GraphicImpl g;
    private RemChat rc;
    private Operation op;

    /**
     * Inizializza un generico client con GUI, possibili operazioni e callbacks.
     *
     * @param serverName nome simbolico o indirizzo IP del server che offre i servizi.
     * @param requestPort porta del server per le richieste.
     * @param registryPort porta del servizio di registry.
     * @param groupPort porta del server per la ricezione dei messaggi nei gruppi.
     */
    public Client(String serverName,int requestPort, int registryPort,int chatPort,int groupPort,int mcGroupPort){
        /* Inizializzo la socket di richiesta per le operazioni di register/login. */
        try{
            requestServer = new Socket(serverName,requestPort);
        } catch (UnknownHostException e) {
            System.err.println("UNKNOWN HOST EXCEPTION");
            closeSocket();
        } catch (IOException e) {
            System.err.println("IO EXCEPTION IN REQUEST SERVER");
            closeSocket();
        }
        /* Prelevo l'oggetto remoto per effettuare RMI. */
        try {
            Registry r = LocateRegistry.getRegistry(serverName,registryPort);
            rc = (RemChat) r.lookup(RemChat.SERVICE_NAME);
        } catch (RemoteException e) {
            System.err.println("UNABLE TO FIND REGISTRY");
            closeSocket();
        } catch (NotBoundException e) {
            System.err.println("UNABLE TO FIND SERVICE NAME");
            closeSocket();
        }
        /* Implementazione delle operazioni di richiesta. */
        RequestImpl requestImpl = new RequestImpl(requestServer,mcGroupPort);
        /* Implementazione delle operazioni di chat. */
        ChatImpl chatImpl = new ChatImpl(serverName,chatPort);
        /* Implementazione delle operazioni dei gruppi. */
        GroupImpl groupImpl = new GroupImpl(requestServer.getInetAddress(),groupPort);
        /* Wrapper che contiene gli oggetti per ogni operazione. */
        op = new OperationImpl(requestServer,chatImpl,groupImpl, requestImpl, rc);
        g = new GraphicImpl(op);

    }

    /**
     * Inizializza la GUI.
     */
    public void start(){
        g.createLogin();
    }

    /**
     * Funzione per il testing dell'applicazione.
     * Una volta finite tutte le operazioni aspetta
     * 60 secondi e termina automaticamente.
     *
     * @param args argomenti per il testing.
     */
    public void test(String[] args){
        g.createLogin();
        try{
            /* Dò un po' di tempo per startare gli altri client. */
            Thread.sleep(5000);
        }catch (InterruptedException e){
            /* Non mi interessa. */
        }
        for(int i = 1;i < args.length;i++){
            switch (args[i]){
                case "reg":
                    op.register(args[i+1],args[i+2],args[i+3]);
                    break;
                case "log":
                    if(op.login(args[i+1],args[i+2]))
                        g.createChatGraphics();
                    else{
                        System.err.println("LOGIN NON RIUSCITO");
                        System.exit(1);
                    }
                    break;
                case "addfriend":
                    op.addFriend(args[i+1]);
                    break;
                case "searchusr":
                    op.searchUser(args[i+1]);
                    break;
                case "flist":
                    op.friendsList();
                    break;
                case "msgto":
                    op.msgToUsr(args[i+1],args[i+2]);
                    break;
                case "fileto":
                    op.fileTo(args[i+1],args[i+2]);
                    break;
                case "creategrp":
                    op.createGroup(args[i+1]);
                    break;
                case "joingrp":
                    op.joinGroup(args[i+1]);
                    break;
                case "grplist":
                    op.groupList();
                    break;
                case "closegrp":
                    op.closeGroup(args[i+1]);
                    break;
                case "msggrp":
                    op.msgToGroup(args[i+1],args[i+2]);
                    break;
                case "close":
                    try{
                        Thread.sleep(60000);
                    }catch (InterruptedException e){
                        /* Non è importante. */
                    }
                    op.close(0);
                    break;
            }
        }
        try{
            Thread.sleep(60000);
        }catch (InterruptedException e){
            /* Non è importante. */
        }
        op.close(1);
    }

    /**
     * Chiude la socket con il server nel caso di errori
     * nella comunicazione e termina il client.
     */
    private void closeSocket(){
        if(requestServer != null) {
            try {
                requestServer.close();
            } catch (IOException e) {
                System.err.println("UNABLE TO CLOSE SOCKET");
            }
        }
        System.exit(1);
    }
}
