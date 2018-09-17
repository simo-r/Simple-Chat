import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.ArrayList;

public class OperationImpl implements Operation,ReplyCode  {
    private BufferedWriter writer;
    private BufferedReader reader;

    private final ChatImpl chatImpl;
    private final GroupImpl groupImpl;
    private final RequestImpl requestImpl;

    private JSONObject obj;
    private final JSONParser parser;

    private UserGroupInfo userInfo;
    private final RemChat rc;

    /**
     * Inizializza le informazioni necessarie per eseguire
     * le operazioni richieste dal client ed apre la connessione
     * di richiesta con il server.
     *
     * @param requestServer socket per le richieste.
     * @param chatImpl Implementazioni delle funzionalità di chat.
     * @param rc RMI per registrarsi alla ricezione di notifiche.
     */
    public OperationImpl(Socket requestServer, ChatImpl chatImpl,
                         GroupImpl groupImpl,RequestImpl requestImpl,
                         RemChat rc){
        this.rc = rc;
        parser = new JSONParser();
        this.requestImpl = requestImpl;
        this.chatImpl = chatImpl;
        this.groupImpl = groupImpl;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(requestServer.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(requestServer.getInputStream()));
        } catch (IOException e) {
            System.err.println("IO EXCEPTION");
            close(1);
        }
    }

    /* Descrizione nell'interfaccia. */
    public void register(String usr, String psw, String lang){
        if(usr == null || psw == null || lang == null){
            System.out.println("Empty field not allowed.");
        }else {
            usr = usr.replaceAll(" ","");
            psw = psw .replaceAll(" ","");
            lang = lang.replaceAll(" ","");
            if (usr.equals("") || psw.equals("") || lang.equals("")) {
                System.out.println("Empty field not allowed.");
            } else if (!ISO_LANG.contains(lang)) {
                System.out.println("Language does not exists");
            } else {
                obj = new JSONObject();
                obj.put(TYPE, REG);
                obj.put(USR, usr);
                obj.put(PSW, psw);
                obj.put(LANG, lang);
                send(obj);
                receive();
            }
        }
    }

    /* Descrizione nell'interfaccia. */
    public boolean login(String usr, String psw){
        boolean isLogged = false;
        if(usr == null || psw == null ){
            System.out.println("Missing username or password!");
        }else {
            if (usr.equals("") || psw.equals("")) {
                System.out.println("Missing username or password!");
            } else {
                usr = usr.replaceAll(" ","");
                psw = psw .replaceAll(" ","");
                obj = new JSONObject();
                obj.put(TYPE, LOG);
                obj.put(USR, usr);
                obj.put(PSW, psw);
                send(obj);
                isLogged = receive();
            }
            if (isLogged) {
                /* Inizializzo le funzionalità delle richieste. */
                if(!requestImpl.init(reader,writer,userInfo)) close(1);
                /* Inizializzo le funzionalità della chat. */
                if (!chatImpl.init(userInfo)) close(1);
                /* Inizializzo le funzionalità dei gruppi. */
                if(!groupImpl.init(writer)) close(1);
                sendStub(usr);
            }
        }
        return isLogged;
    }

    /* Descrizione nell'interfaccia. */
    public void addFriend(String usr){
        if(usr == null){
            System.out.println("Null user not allowed!");
        }else {
            usr = usr.replaceAll(" ","");
            if (usr.equals(""))
                System.out.println("Empty username not allowed");
            else if (usr.equals(userInfo.getUsr())) {
                System.out.println("You are already your friend!");
            } else {
                if(!requestImpl.addFriend(usr)) close(1);
            }
        }
    }

    /* Descrizione nell'interfaccia. */
    public void searchUser(String usr) {
        if(usr == null){
            System.out.println("Null user not allowed!");
        }else {
            usr = usr.replaceAll(" ", "");
            if (usr.equals("")) {
                System.out.println("Empty username not allowed");
            } else if (usr.equals(userInfo.getUsr())) {
                System.out.println("You exist!");
            } else {
                if(!requestImpl.searchUser(usr)) close(1);
            }
        }
    }

    /* Descrizione nell'interfaccia. */
    public void friendsList(){
        if(!requestImpl.friendsList()) close(1);
    }

    /* Descrizione nell'interfaccia. */
    public void msgToUsr(String destUsr, String msg){
        if(destUsr == null || msg == null){
            System.out.println("Null receiver or message not allowed!");
        }else {
            destUsr = destUsr.replaceAll(" ", "");
            if (destUsr.equals("") || msg.equals("")) {
                System.out.println("Empty receiver or message not allowed");
            } else if (destUsr.equals(userInfo.getUsr())) {
                System.out.println("You can not talk with yourself!");
            } else {
                if (!chatImpl.sendUsrMsg(destUsr, msg)) close(1);
            }
        }
    }

    /* Descrizione nell'interfaccia. */
    public void fileTo(String destUsr, String fileName){
        if(destUsr == null || fileName == null){
            System.out.println("Null receiver or path not allowed!");
        }else{
            destUsr = destUsr.replaceAll(" ","");
            if(destUsr.equals("") || fileName.equals("")){
                System.out.println("Empty receiver or message not allowed!");
            }else if (destUsr.equals(userInfo.getUsr())){
                System.out.println("You can not send file to yourself!");
            }else{
                fileName = fileName.trim();
                if(!chatImpl.sendFileRequest(destUsr, fileName)) close(1);
            }
        }
    }

    /* Descrizione nell'interfaccia. */
    public void createGroup(String groupName) {
        if (groupName == null) {
            System.out.println("Null group name not allowed!");
        } else {
            groupName = groupName.trim();
            if (groupName.equals("")) {
                System.out.println("Empty group name not allowed!");
            } else {
                if (!groupImpl.createGroup(groupName)) close(1);
            }
        }
    }

    /* Descrizione nell'interfaccia. */
    public void joinGroup(String groupName){
        if(groupName == null){
            System.out.println("Null group name not allowed!");
        }else {
            groupName = groupName.trim();
            if(groupName.equals("")){
                System.out.println("Empty group name now allowed!");
            }else{
                if(!groupImpl.joinGroup(groupName)) close(1);
            }
        }
    }

    /* Descrizione nell'interfaccia. */
    public void msgToGroup(String destGrp, String msg){
        if(destGrp == null || msg == null){
            System.out.println("Null receiver or message not allowed!");
        }else {
            destGrp = destGrp.trim();
            if (destGrp.equals("") || msg.equals("")) {
                System.out.println("Empty receiver or message not allowed");
            }else {
                if (!groupImpl.sendGrpMsg(userInfo.getUsr(),destGrp, msg)) close(1);
            }
        }
    }

    /* Descrizione nell'interfaccia. */
    public void groupList(){
        if(!groupImpl.groupList()) close(1);
    }

    /* Descrizione nell'interfaccia. */
    public void closeGroup(String groupName){
        groupName = groupName.trim();
        if(!groupImpl.closeGroup(groupName)) close(1);
    }

    /* Descrizione nell'interfaccia. */
    public void close(int status){
        requestImpl.close();
        chatImpl.close();
        groupImpl.close();
        System.out.println("CLOSING CLIENT");
        System.exit(status);
    }

    /**
     * Manda una richiesta al server in formato JSON
     * secondo il protocollo prestabilito.
     *
     * @param obj richiesta da mandare al server.
     * @implNote questo metodo è usato solamente da questa classe,
     *           solo per le operazioni di register e login e solo
     *           in maniera sincrona.
     */
    private void send(JSONObject obj){
        try{
            writer.write(obj.toJSONString());
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            /* Eccezione della write. */
            System.err.println("IO EXCEPTION SEND");
            close(1);
        }
    }

    /**
     * Riceve una risposta dal server in formato JSON
     * secondo il protocollo prestabilito.
     * Riceve in maniera sincrona risposte per
     * la registrazione ed il login.
     *
     * @return true se ricevo una risposta positiva, false altrimenti.
     * @implNote questo metodo è usato solamente da questa classe,
     *           solo per le operazioni di register e login e solo
     *           in maniera sincrona.
     */
    private boolean receive(){
        boolean result = false;
        String msg;
        String json;
        try{
            json = reader.readLine();
            obj = (JSONObject) parser.parse(json);
            /* Il messaggio è sempre presente in tutte le risposte. */
            msg = (String) obj.get(MSG);
            System.out.println(msg);
            switch ((String) obj.get(TYPE)){
                case ACK:
                    result = true;
                    break;
                case NACK:
                    result = false;
                    break;
                case LOGACK:
                    String name = (String) obj.get(USR);
                    String lang = (String) obj.get(LANG);
                    /* Ricavo nomi gruppi e IP per poi joinarli. */
                    ArrayList<String> group= (ArrayList<String>) obj.get(GRPNAME);
                    ArrayList<String> groupIp= (ArrayList<String>) obj.get(IP);
                    userInfo = new UserGroupInfo(name,lang);
                    /* Se genera eccezione termina. */
                    for(int i = 0;i<group.size();i++){
                        InetAddress grpAddr = InetAddress.getByName(groupIp.get(i));
                        userInfo.addGroup(group.get(i),grpAddr);
                    }
                    result = true;
                    break;
                default:
                    System.err.println("DEFAULT "+ obj.get(TYPE));
                    break;
            }
        }catch (IOException e) {
            System.err.println("IO EXCEPTION RECEIVE");
            close(1);
        } catch (ParseException e) {
            System.err.println("PARSE EXCEPTION");
            close(1);
        }
        return result;

    }

    /**
     * Invia lo stub le notifiche.
     */
    private void sendStub(String usr){
        try {
            ChatEvent stubCE = new ChatEventImpl();
            rc.registerLogin(usr,stubCE);
        } catch (RemoteException e) {
            System.err.println("REMOTE EXCEPTION SEND STUB");
            close(1);
        }
    }
}
