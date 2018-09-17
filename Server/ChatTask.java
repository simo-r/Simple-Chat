import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

class ChatTask extends Task implements Runnable{
    /* Thread che gestisce le richieste di invio messaggi e file di un client */
    private final ChatService chatOp;

    /**
     * Inizializza i lati in lettura e scrittura della socket.
     *
     * @param client socket del client da servire.
     * @param chatOp implementazione delle operazioni di chat.
     */
    public ChatTask(Socket client, ChatService chatOp){
        super(client);
        this.chatOp = chatOp;
    }

    /**
     * Riceve dalla socket una richiesta ed
     * esegue la rispettiva operazione.
     */
    public void run() {
        try{
            String destUsr;
            String msg;
            Long ide;
            System.out.println("New chat task activated");
            Thread currentThread = Thread.currentThread();
            while(!currentThread.isInterrupted()){
                try {
                    obj = receive();
                }catch (ParseException e){
                    System.err.println("PARSE EXCEPTION CHAT TASK");
                    /* Non termino ma riprovo a leggere */
                    continue;
                }
                if (obj == null) break;
                switch ((String) obj.get(TYPE)) {
                    case INIT:
                        currentUser = (String) obj.get(USR);
                        chatOp.initChatSocket(currentUser, writer);
                        System.out.printf("[CHAT INIT] from: %s socket: %s\n",currentUser, client.getRemoteSocketAddress().toString());
                        break;
                    case CHATMSG:
                        destUsr = (String) obj.get(TO);
                        msg = (String) obj.get(MSG);
                        chatMsg(destUsr, msg);
                        break;
                    case FILEMSG:
                        destUsr = (String) obj.get(TO);
                        msg = (String) obj.get(FILENAME);
                        ide = (Long) obj.get(IDE);
                        Long len = (Long) obj.get(LEN);
                        fileMsg(destUsr, msg, len, ide);
                        break;
                    case SOCKETINFO:
                        destUsr = (String) obj.get(USR);
                        String ip = (String) obj.get(IP);
                        Long port = (Long) obj.get(PORT);
                        ide = (Long) obj.get(IDE);
                        System.out.printf("[FILE TRANFER SOCKET INFO] from: %s to: %s \n",currentUser, destUsr);
                        chatOp.sendSocketInfo(destUsr, ip, port, ide);
                        break;
                    default:
                        System.out.printf("CHAT TASK DEFAULT: %s",currentUser);
                        break;
                }
            }

        } catch (EOFException e){
            System.err.println("EOF EXCEPTION CHAT TASK");
            if(client.isClosed()) System.err.println("CLOSED CHAT TASK");
        } catch (SocketException e){
            System.err.println("SOCKET EXCEPTION CHAT TASK");
        } catch (IOException e){
            System.err.println("IO EXCEPTION CHAT TASK");
        } finally {
            close();
        }
    }

    /**
     * Invia il messaggio al destinatario ed una risposta
     * che identifica il risultato dell'operazione al mittente.
     *
     * @param destUsr utente destinatario del messaggio.
     * @param msg body del messaggio.
     */
    private void chatMsg(String destUsr, String msg){
        SSCode result =chatOp.sendChatMsg(currentUser,destUsr,msg);
        obj = new JSONObject();
        switch (result){
            case OK:
                System.out.printf("[CHAT MSG] from: %s to: %s msg: %s RESULT: %s\n",currentUser,destUsr,msg,ACK);
                obj.put(TYPE,ACK);
                obj.put(MSG,senderMsg(destUsr,msg));
                break;
            case OFFLINE:
                System.out.printf("[CHAT MSG] from: %s to: %s msg: %s RESULT: OFFLINE\n",currentUser,destUsr,msg);
                obj.put(TYPE,NACK);
                obj.put(MSG,usrOffline(destUsr));
                break;
            case WRGUSR:
                System.out.printf("[CHAT MSG] from: %s to: %s msg: %s RESULT: NOT EXIST\n",currentUser,destUsr,msg);
                obj.put(TYPE,NACK);
                obj.put(MSG,usrNotExist(destUsr));
                break;
            case NOT_FRIENDS:
                System.out.printf("[CHAT MSG] from: %s to: %s msg: %s RESULT: NOT FRIENDS\n",currentUser,destUsr,msg);
                obj.put(TYPE,NACK);
                obj.put(MSG,usrNotFriend(destUsr));
                break;
        }
        send(obj);
    }

    /**
     * Invia la richiesta di invio file al destinatario e una
     * risposta che identifica il risultato dell'operazione
     * al mittente.
     *
     * @param destUsr destinatario della richiesta.
     * @param fileName nome del file.
     * @param len lunghezza del file.
     * @param ide identificatore univoco del file.
     */
    private void fileMsg(String destUsr, String fileName, Long len, Long ide){
        SSCode result = chatOp.requestFileMsg(currentUser,destUsr,fileName,len,ide);
        obj = new JSONObject();
        switch (result){
            case OK:
                System.out.printf("[FILE MSG] from: %s to: %s file: %s RESULT: ACK\n",currentUser,destUsr,fileName);
                obj.put(TYPE,ACK);
                obj.put(MSG,senderFile(destUsr,fileName));
                break;
            case OFFLINE:
                System.out.printf("[FILE MSG] from: %s to: %s file: %s RESULT: OFFLINE\n",currentUser,destUsr,fileName);
                obj.put(TYPE,NACK);
                obj.put(MSG,usrOffline(destUsr));
                break;
            case WRGUSR:
                System.out.printf("[FILE MSG] from: %s to: %s file: %s RESULT: NOT EXIST\n",currentUser,destUsr,fileName);
                obj.put(TYPE,NACK);
                obj.put(MSG,usrNotExist(destUsr));
                break;
            case NOT_FRIENDS:
                System.out.printf("[FILE MSG] from: %s to: %s file: %s RESULT: NOT FRIENDS\n",currentUser,destUsr,fileName);
                obj.put(TYPE, NACK);
                obj.put(MSG,usrNotFriend(destUsr));
                break;
        }
        send(obj);
    }
}
