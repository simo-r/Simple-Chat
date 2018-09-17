import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

class FileSender implements Runnable {
    private static final int BUFFER_SIZE= 512;
    private final String ip;
    private final Long port;
    private final String path;

    /**
     * Inizializza le informazioni per l'apertura
     * della connessione p2p per l'invio del file.
     *
     * @param ip ip del destinatario.
     * @param port porta del destinatario.
     * @param path percorso del file da spedire.
     */
    public FileSender(String ip, Long port, String path){
        this.ip = ip;
        this.port = port;
        this.path = path;
    }

    /**
     * Si connette alla socket <ip,porta> fornita dal server
     * ed inizia il trasferimento del file.
     */
    public void run() {
        /* Non-direct buffer perché avrei un overhead in allocazione e deallocazione inutile. */
        ByteBuffer bBuf = ByteBuffer.allocate(BUFFER_SIZE);
        File file = new File(path);
        SocketChannel sc=null;
        try(FileChannel readChannel = FileChannel.open(
                Paths.get(file.toURI()), StandardOpenOption.READ)){
            sc = SocketChannel.open();
            sc.connect(new InetSocketAddress(ip,port.intValue()));
            Thread currentThread = Thread.currentThread();
            /* Questo serve a farlo terminare quando ho poolTermination(). */
            while(readChannel.read(bBuf)!= -1 && !currentThread.isInterrupted()){
                bBuf.flip();
                while(bBuf.hasRemaining()){
                    sc.write(bBuf);
                }
                bBuf.clear();
            }
            if(!currentThread.isInterrupted())
                System.out.printf("[FILE SENT] %s\n",file.getName());
            else System.out.printf("[FILE] INTERRUPTED: %s\n",file.getName());
        }catch (IOException e){
            System.err.printf("[FILE] UNABLE TO SEND: %s\n",file.getName());
        }finally {
            close(sc);
        }
    }

    /**
     * Chiude la socket per l'invio del file (p2p).
     *
     * @param sc socket da chiudere.
     */
    private void close(SocketChannel sc){
        if(sc != null) {
            try {
                sc.close();
            }catch (IOException e) {
                System.err.println("UNABLE TO CLOSE FILE SOCKET SENDER");
            }
        }
    }
}
