import org.json.simple.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

class ChatImpl extends Sender implements ReplyCode {
    private Long FILE_COUNT = 0L;
    private final String serverName;
    private final int chatPort;
    private JSONObject obj;
    private final ConcurrentHashMap<Long,String> files;

    /**
     * Inizializza le possibili operazioni di chat.
     *
     * @param serverName indirizzo IP del server.
     * @param chatPort porta del server per le richieste di chat.
     */
    public ChatImpl(String serverName, int chatPort){
        super();
        this.serverName = serverName;
        this.chatPort = chatPort;
        this.files = new ConcurrentHashMap<>();
    }

    /**
     * Apre la chat socket verso il server, inizializza reader e writer e starta
     * un thread che sarà in ricezione sulla chat socket costantemente.
     * Invia la richiesta di inizializzazione al server.
     *
     * @param userInfo utente corrente.
     * @return true se la creazione del thread receiver e l'invio
     *         della richiesta al server vanno a buon fine, false altrimenti.
     */
    public boolean init(UserGroupInfo userInfo){
        boolean result = true;
        try {
            server = new Socket(serverName, chatPort);
            writer = new BufferedWriter(new OutputStreamWriter(server.getOutputStream()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(server.getInputStream()));
            /* Nuovo thread che si mette in ricezione sulla chat socket */
            receiverT = new Thread(new ChatReceiver(reader,writer,files));
            receiverT.start();
            obj = new JSONObject();
            obj.put(TYPE,INIT);
            obj.put(USR,userInfo.getUsr());
            result = send(obj);
        } catch (IOException e) {
            /* Non può lanciare UnknownHostExc. perché già aperta request socket. */
            System.err.println("IO EXCEPTION IN CHAT SERVER");
            result = false;
        }
        return result;

    }

    /**
     * Invia un messaggio all'amico destinatario.
     *
     * @param destUsr destinatario del messaggio.
     * @param msg body del messaggio.
     * @return true se l'invio del messaggio è andato
     *         a buon fine, false altrimenti.
     */
    public boolean sendUsrMsg(String destUsr, String msg){
        obj = new JSONObject();
        obj.put(TYPE, CHATMSG);
        obj.put(TO, destUsr);
        obj.put(MSG, msg);
        return send(obj);
    }

    /**
     * Invia una richiesta di invio file ad un amico
     * al server.
     *
     * @param destUsr destinatario della richiesta.
     * @param path path del file da spedire.
     * @return true se l'invio della richiesta è andato
     *         a buon fine, false altrimenti.
     */
    public boolean sendFileRequest(String destUsr, String path){
        boolean result = true;
        File file = new File(path);
        if(file.isFile() && file.canRead()){
            obj = new JSONObject();
            obj.put(TYPE,FILEMSG);
            obj.put(TO,destUsr);
            obj.put(FILENAME,file.getName());
            obj.put(IDE,FILE_COUNT);
            obj.put(LEN,file.length());
            result = send(obj);
            if(result){
                files.put(FILE_COUNT,path);
                FILE_COUNT++;
            }
        }else{
            System.out.printf("[FILE] Invalid path: %s\n",path);
        }
        return result;
    }
}
