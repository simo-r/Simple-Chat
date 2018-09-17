import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

class RequestServer implements Runnable {
    private final AtomicInteger errorCount;
    private final RequestService requestOp;
    private final ThreadPoolExecutor executor;
    private ServerSocket rs;

    /**
     * Inizializza il gestore delle richieste, la socket per le richieste,
     * il thread pool executor e il numero di errori fin ora raggiunto.
     *
     * @param requestPort porta su cui aprire la socket per le richieste.
     * @param errorCount numero di errori correnti rilevati.
     * @param executor thread pool.
     * @param requestOp oggetto contenente le implementazioni delle operazioni di richiesta.
     * @throws IOException se la creazione della chat socket fallisce.
     */
    public RequestServer(int requestPort, AtomicInteger errorCount,
                         ThreadPoolExecutor executor, RequestService requestOp) throws IOException{
        this.errorCount = errorCount;
        this.executor = executor;
        this.requestOp = requestOp;
        rs = new ServerSocket(requestPort);
    }

    /**
     * Si mette in attesa di ricevere nuove connessioni sulla request socket
     * e le sottomette al thread pool.
     */
    public void run(){
        Socket client;
        System.out.println("REQUEST SERVER UP");
        Thread currentThread = Thread.currentThread();
        while(!currentThread.isInterrupted()){
            try {
                client = rs.accept();
                executor.execute(new RequestTask(client,requestOp));
            } catch (IOException e) {
                errorCount.incrementAndGet();
                System.err.printf("ACCEPT EXCEPTION. ERROR NUMBER: %d",errorCount.get());
            }
        }
        close();
    }

    /**
     * Chiude la request socket.
     */
    public void close() {
        if(rs != null && !rs.isClosed()) {
            try {
                rs.close();
            } catch (IOException e) {
                System.err.print("IO EXCEPTION WHILE CLOSING REQUEST SERVER SOCKET");
            }
        }
    }
}
