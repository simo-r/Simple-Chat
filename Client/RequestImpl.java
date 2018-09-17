import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;

class RequestImpl extends Sender implements ReplyCode{
    private final int mcGroupPort;
    private JSONObject obj;

    /**
     * Inizializza le informazioni sulla socket per le richieste
     * e la porta per i gruppi multicast.
     *
     * @param requestServer socket per le richieste.
     * @param mcGroupPort porta per i gruppi multicast.
     */
    public RequestImpl(Socket requestServer, int mcGroupPort){
        super();
        server = requestServer;
        this.mcGroupPort = mcGroupPort;
    }

    /**
     * Inizializza le funzionalità per le richieste al server
     * e starta un thread che riceverà le risposte che riguardano
     * le operazioni di richiesta.
     *
     * @param reader lato in lettura sulla socket per le richieste.
     * @param writer lato in scrittura sulla socket per le richieste.
     * @param userInfo informazioni sull'utente corrente.
     * @return true se l'inizializzazione è andata a buon fine,
     *         false altrimenti.
     */
    public boolean init(BufferedReader reader, BufferedWriter writer, UserGroupInfo userInfo){
        boolean result = true;
        this.writer = writer;
        RequestReceiver rr;
        try {
            rr = new RequestReceiver(reader,userInfo,mcGroupPort);
            receiverT = new Thread(rr);
            receiverT.start();
        } catch (IOException e) {
            System.err.println("IOEXCEPTION REQEST RECEIVER INIT");
            result = false;
        }
        return result;
    }

    /**
     * Invia una richiesta al server per aggiungere un utente
     * alla lista amici.
     *
     * @param usr utente da aggiungere come amico.
     * @return true se l'invio della richiesta va a buon fine,
     *         false altrimenti.
     */
    public boolean addFriend(String usr){
        obj = new JSONObject();
        obj.put(TYPE, ADDFRIEND);
        obj.put(USR, usr);
        return send(obj);
    }

    /**
     * Invia una richiesta al server
     * per ricercare un utente.
     *
     * @param usr utente da ricercare.
     * @return true se l'invio della richiesta va a buon fine,
     *         false altrimenti.
     */
    public boolean searchUser(String usr) {
        obj = new JSONObject();
        obj.put(TYPE, SEARCHUSR);
        obj.put(USR, usr);
        return send(obj);
    }

    /**
     * Invia una richiesta al server per ricevere la lista
     * degli amici.
     *
     * @return true se l'invio della richiesta va a buon fine,
     *         false altrimenti.
     */
    public boolean friendsList() {
        obj = new JSONObject();
        obj.put(TYPE, FRNDLST);
        return send(obj);
    }
}
