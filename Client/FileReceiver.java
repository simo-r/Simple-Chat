import org.json.simple.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
/**
 * Si occupa di aprire una nuova socket, di comunicarla al
 * mittente del file tramite il server e della ricezione del
 * file.
 */
class FileReceiver implements Runnable,ReplyCode {

    private static final int TIMEOUT = 600000;
    private static final int BUFFER_SIZE= 512;
    private final Long ide;
    private Long len;
    private final String sender;
    private final String fileName;
    private final BufferedWriter chatWriter;

    /**
     * Inizializza un nuovo "ricevitore" di file.
     *
     * @param sender mittente del file.
     * @param fileName nome del file.
     * @param len lunghezza del file da leggere.
     * @param ide identificatore del file.
     * @param chatWriter lato in scrittura con il chat server.
     */
    public FileReceiver(String sender, String fileName, Long len, Long ide, BufferedWriter chatWriter){
        this.sender = sender;
        this.fileName = fileName;
        this.chatWriter = chatWriter;
        this.ide = ide;
        this.len = len;
    }

    /**
     * Apre una nuova socket su una porta libera,
     * comunica IP e porta al server e si mette in
     * attesa (max TIMEOUT) che il mittente del file
     * si connetta.
     * Una volta connesso inizia la lettura a chunk
     * del file fin quando non raggiunge la dimensione
     * prefissata.
     *
     * @implNote il file ricevuto viene salvato nella
     *           directory in cui è stato eseguito il client.
     */
    public void run() {
        ServerSocketChannel ssc = null;
        try {
            ssc = ServerSocketChannel.open();
            ServerSocket ss = ssc.socket();
            /* Setto un tempo massimo per l'inizio della ricezione. */
            ss.setSoTimeout(TIMEOUT);
            ss.bind(new InetSocketAddress(0));
            sendInfo(InetAddress.getLocalHost().getHostAddress(),ss.getLocalPort());
            /* Devo fare per forza così altrimenti il SoTimeout non funziona. */
            SocketChannel senderSocket = ss.accept().getChannel();
            /* Non-direct buffer perché avrei un overhead in allocazione e deallocazione inutile. */
            ByteBuffer bBuff = ByteBuffer.allocate(BUFFER_SIZE);
            Thread currentThread = Thread.currentThread();
            File file = new File(fileName);
            file.createNewFile();
            try(FileChannel writeChannel = FileChannel.open(
                    Paths.get(file.toURI()), StandardOpenOption.WRITE)) {
                /* Questo serve a farlo terminare quando ho poolTermination(). */
                while (len > 0 && !currentThread.isInterrupted()) {
                    len -= senderSocket.read(bBuff);
                    bBuff.flip();
                    while (bBuff.hasRemaining()) {
                        writeChannel.write(bBuff);
                    }
                    bBuff.clear();
                }
            }
            if(!currentThread.isInterrupted()) {
                System.out.printf("[FILE RECEIVED] %s: %s\n", sender, fileName);
            }
        } catch(SocketTimeoutException | ClosedByInterruptException e) {
            /* Lancia ClosedByInterExc. anche se dovrebbe lanciare SocketTimeoutExc. */
            System.err.printf("[FILE] TIMEOUT REACHED: %s FROM: %s\n",fileName,sender);
        } catch (IOException e){
            e.printStackTrace();
            System.err.printf("[FILE] UNABLE TO RECEIVE: %s FROM: %s\n",fileName,sender);
        }finally {
            close(ssc);
        }

    }

    /**
     * Invia le informazioni (IP e porta) al server
     * per la ricezione del file.
     *
     * @param ip ip del destinatario del file.
     * @param port porta del destinatario del file.
     * @throws IOException se l'invio delle info al server fallisce.
     */
    private void sendInfo(String ip, int port) throws IOException{
        JSONObject obj = new JSONObject();
        obj.put(TYPE, SOCKETINFO);
        obj.put(USR,sender);
        obj.put(IP,ip);
        /* JSONSimple accetta solo Long. */
        obj.put(PORT,new Long(port));
        obj.put(IDE,ide);
        send(chatWriter,obj);
    }

    /**
     * Invia un messaggio al chat server.
     *
     * @param writer writer con il server.
     * @param obj richiesta da inviare.
     * @throws IOException se l'invio della richiesta al server fallisce.
     * @implNote L'eccezione viene fatta rimbalzare fino al metodo run().
     */
    private void send(BufferedWriter writer,JSONObject obj) throws IOException{
        writer.write(obj.toJSONString());
        writer.newLine();
        writer.flush();
    }

    /**
     * Chiude la socket per la ricezione del file (p2p).
     *
     * @param ssc socket da chiudere.
     */
    private void close(ServerSocketChannel ssc){
        if(ssc!=null) {
            try {
                ssc.close();
            } catch (IOException e) {
                System.err.println("UNABLE TO CLOSE FILE SOCKET RECEIVER");
            }
        }
    }
}
