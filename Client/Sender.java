import org.json.simple.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;

class Sender {
    protected BufferedWriter writer;
    protected Thread receiverT;
    protected Socket server;

    /**
     * Costruisce un'istanza vuota, le variabili d'istanza
     * verranno inizializzate successivamente.
     */
    public Sender(){ }

    /**
     * Manda una richiesta al server in formato JSON
     * secondo il protocollo prestabilito.
     *
     * @param obj richiesta da mandare al server.
     * @return true se l'invio è riuscito, false altrimenti.
     * @implNote il line separator va aggiunto in questo modo
     *           altrimenti l'operazione non è atomica/sincronizzata
     *           e potrebbe creare inconsistenze.
     */
    boolean send(JSONObject obj){
        if(!checkReceiver()) return false;
        boolean result = true;
        try{
            writer.write(obj.toJSONString()+System.getProperty("line.separator"));
            writer.flush();
        } catch (IOException e) {
            System.err.println("IO EXCEPTION SEND");
            close();
            result = false;
        }
        return result;
    }

    /**
     * Controlla se il receiver thread è terminato.
     * Serve a capire se c'è stato qualche errore nella connessione
     * e terminare il client.
     *
     * @return true se il thread receiver è ancora vivo, false altrimenti.
     */
    private boolean checkReceiver(){
        boolean result = true;
        if(!receiverT.isAlive()){
            System.err.println("RECEIVER THREAD IS NO LONGER ALIVE");
            result = false;
        }
        return result;
    }

    /**
     * Chiude la socket e aspetta un tempo limitato
     * che il receiver thread termini.
     */
    public void close(){
        if (server != null && !server.isClosed()) {
            try {
                server.close();
            } catch (IOException e) {
                System.err.println("UNABLE TO CLOSE SOCKET");
            }
        }
        if (receiverT != null) {
            try {
                receiverT.interrupt();
                receiverT.join(Client.JOIN_TIME);
            } catch (InterruptedException e) {
                System.err.println("INTERRUPTED WHILE JOINING CHAT RECEIVER THREAD");
            }
        }
    }
}
