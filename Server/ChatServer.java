import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

class ChatServer implements Runnable {
    /* Thread in attesa di nuove connessioni sulla chat socket. */
    private ServerSocket cs;
    private final AtomicInteger errorCount;
    private final ThreadPoolExecutor executor;
    private final ChatService chatOp;

    /**
     * Crea una nuova istanza inizializzando gli oggetti condivisi:
     * errorCount, executor,chatOp ed apre la server socket per le
     * operazioni di chat.
     *
     * @param chatPort porta su cui aprire la socket per le chat.
     * @param errorCount numero di errori correnti rilevati.
     * @param executor thread pool.
     * @param chatOp oggetto contenente le informazioni sullo stato del server.
     * @throws IOException se la creazione della chat socket fallisce.
     */
    public ChatServer(int chatPort, AtomicInteger errorCount,
                      ThreadPoolExecutor executor, ChatService chatOp) throws IOException{
        this.errorCount = errorCount;
        this.executor = executor;
        this.chatOp = chatOp;
        cs = new ServerSocket(chatPort);
    }

    /**
     * Si mette in attesa di ricevere nuove connessioni sulla chat socket
     * e le sottomette al thread pool.
     */
    public void run() {
        Socket client;
        System.out.println("CHAT SERVER UP");
        Thread currentThread = Thread.currentThread();
        while(!currentThread.isInterrupted()){
            try {
                client = cs.accept();
                executor.execute(new ChatTask(client,chatOp));
            } catch (IOException e) {
                errorCount.incrementAndGet();
                System.err.printf("ACCEPT EXCEPTION. ERROR NUMBER: %d",errorCount.get());
            }
        }
        close();
    }

    /**
     * Chiude la chat socket.
     */
    public void close() {
        if(cs != null && !cs.isClosed()) {
            try {
                cs.close();
            } catch (IOException e) {
                System.err.print("IO EXCEPTION WHILE CLOSING CHAT SERVER SOCKET");
            }
        }
    }
}
