import java.io.IOException;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class Server {
    private final ThreadPoolExecutor executor;
    private Registry registry;
    private RequestServer rs;
    private ChatServer cs;
    private GroupServer gs;
    private final AtomicInteger errorCount;
    private MulticastSocket mcs=null;
    private final ConcurrentHashMap<String,Group> groups;

    /**
     * Inizializza i vari servizi forniti dal server.
     *
     * @param requestPort porta per il servizio (TCP) di gestione richieste.
     * @param registryPort porta per il servizio (RMI/TCP)di registry.
     * @param chatPort porta per il servizio (TCP) di messaggistica amici/file.
     * @param groupPort porta per il servizio (UDP) di messaggistica sui gruppi.
     * @param mcGroupPort porta per il servizio (MULTICAST) di invio messaggi sui gruppi.
     */
    public Server(int requestPort,int registryPort,int chatPort,int groupPort,int mcGroupPort){
        this.errorCount = new AtomicInteger(0);
        executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        executor.allowCoreThreadTimeOut(true);
        executor.setKeepAliveTime(1,TimeUnit.MINUTES);
        ConcurrentHashMap<String, UserOperation> users = new ConcurrentHashMap<>();
        groups = new ConcurrentHashMap<>();
        try {
            mcs = new MulticastSocket(mcGroupPort);
            mcs.setReuseAddress(true);
        } catch(SocketException e){
            System.err.println("SOCKET EXCEPTION IMPOSSIBLE TO CREATE SOCIAL GRAPH");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("IO EXCEPTION IMPOSSIBLE TO CREATE SOCIAL GRAPH");
            System.exit(1);
        }
        System.out.println("MULTICAST SERVER UP");
        RequestService requestOp = new RequestService(users,groups,mcs,mcGroupPort);
        try {
            rs = new RequestServer(requestPort, errorCount, executor, requestOp);
        }catch (IOException e){
            closeMulticastSocket();
            System.err.println("IMPOSSIBLE TO START REQUEST SERVER");
            System.exit(1);
        }
        ChatService chatOp = new ChatService(users);
        try{
            cs = new ChatServer(chatPort,errorCount,executor,chatOp);
        }catch (IOException e){
            System.err.println("IMPOSSIBLE TO START CHAT SERVER");
            System.exit(1);
            rs.close();
            closeMulticastSocket();
        }
        GroupService groupOp = new GroupService(groups,mcs,mcGroupPort);
        try{
            gs = new GroupServer(groupPort,errorCount,executor,groupOp);
        }catch (IOException e){
            e.printStackTrace();
            System.err.println("IMPOSSIBLE TO START GROUP SERVER");
            System.exit(1);
            rs.close();
            cs.close();
            closeMulticastSocket();
        }
        RemChatService remChatOp = new RemChatService(users);
        try {
            RemChat stubRC = new RemChatTask(remChatOp);
            LocateRegistry.createRegistry(registryPort);
            registry = LocateRegistry.getRegistry(registryPort);
            registry.rebind(RemChat.SERVICE_NAME,stubRC);
        } catch (RemoteException e) {
            System.err.println("IMPOSSIBLE TO START NOTIFICATION SERVER");
            cs.close();
            rs.close();
            gs.close();
            closeMulticastSocket();
            System.exit(1);
        }
        System.out.println("RMI SERVER UP");
    }

    /**
     * Fa partire i vari servizi offerti dal server
     * e gestisce la loro terminazione.
     */
    public void run(){
        final int MAX_ERROR_COUNT = 10;
        final int JOIN_TIME = 120000;
        Thread rsThread = new Thread(rs);
        Thread csThread = new Thread(cs);
        Thread gsThread = new Thread(gs);
        rsThread.start();
        csThread.start();
        gsThread.start();
        while(errorCount.get() < MAX_ERROR_COUNT) {
            try {
                if(!rsThread.isAlive() || !csThread.isAlive() || !gsThread.isAlive()) {
                    break;
                }
                rsThread.join(JOIN_TIME);
                csThread.join(JOIN_TIME);
                gsThread.join(JOIN_TIME);
            } catch (InterruptedException e) {
                System.err.println("SERVER INTERRUPTED");
            }
        }
        poolTermination();
        csThread.interrupt();
        rsThread.interrupt();
        gsThread.interrupt();
        closeMulticastSocket();
        try {
            registry.unbind(RemChat.SERVICE_NAME);
        } catch (RemoteException e) {
            System.err.println("UNABLE TO UNBIND RMI REGISTRY");
        } catch (NotBoundException e) {
            System.err.println("RMI REGISTRY NOT BOUNDED");
        }
        System.out.println("SERVER TERMINATO");
    }

    /**
     * Termina il pool di thread (gracefully).
     */
    private void poolTermination(){
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            System.err.println("INTERRUPTED WHILE WAITING POOL TERMINATION");
        }
        if(!executor.isTerminated()) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                System.err.println("INTERRUPTED WHILE WAITING POOL TERMINATION");
            }
        }
        if(!executor.isTerminated()){
            System.err.println("POOL TERMINATION FAILED");
        }
    }

    /**
     * Esce da tutti i gruppi multicast che aveva joinato
     * e chiude la socket multicast.
     *
     * @implNote questo metodo viene invocato solo dal thread
     *           che sta eseguendo la classe Server e solo quando
     *           tutti gli altri thread che possono accedere alla multicast socket
     *           sono stati interrotti quindi posso effettuare l'iterazione
     *           e le operazioni senza alcun problema.
     */
    private void closeMulticastSocket(){
        if(mcs!=null) {
            for (Group g : groups.values()) {
                try {
                    mcs.leaveGroup(g.getInetAddr());
                } catch (IOException e) {
                    System.err.printf("IO EXCEPTION LEAVING GROUP: %s\n", g.getGroupName());
                }
            }
            mcs.close();
        }
    }
}
