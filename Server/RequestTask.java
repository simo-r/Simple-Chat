import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.List;

class RequestTask extends Task implements Runnable {
    /* Thread che gestisce le richieste di un client. */

    private final RequestService requestOp;

    /**
     * Inizializza un task che si occuperà di
     * rispondere alle richieste di un client.
     *
     * @param client socket del client da servire.
     */
    public RequestTask(Socket client, RequestService requestOp){
        super(client);
        this.requestOp = requestOp;
    }

    /**
     * Riceve dalla socket una richiesta in formato JSON ed
     * esegue l'operazione richiesta.
     */
    public void run(){
        String psw;
        String groupName;
        try {
            System.out.println("New request task activated");
            Thread currentThread = Thread.currentThread();
            /* Questo serve a farlo terminare quando ho poolTermination(). */
            while (!currentThread.isInterrupted()) {
                try {
                    obj = receive();
                }catch (ParseException e){
                    System.err.println("PARSE EXCEPTION CHAT TASK");
                    /* Non termino ma riprovo a leggere. */
                    continue;
                }
                if(obj == null) break;
                switch ((String) obj.get(TYPE)) {
                    case REG:
                        currentUser = (String) obj.get(USR);
                        psw = (String) obj.get(PSW);
                        String lang = (String) obj.get(LANG);
                        register(currentUser,psw,lang);
                        break;
                    case LOG:
                        currentUser = (String) obj.get(USR);
                        psw = (String) obj.get(PSW);
                        login(currentUser,psw);
                        break;
                    case ADDFRIEND:
                        String tmpFriend = (String) obj.get(USR);
                        newRelation(tmpFriend);
                        break;
                    case SEARCHUSR:
                        String tmpUsr = (String) obj.get(USR);
                        searchUser(tmpUsr);
                        break;
                    case FRNDLST:
                        friendList();
                        break;
                    case GRPCREATE:
                        groupName = (String) obj.get(GRPNAME);
                        createGroup(currentUser,groupName);
                        break;
                    case GRPJOIN:
                        groupName = (String) obj.get(GRPNAME);
                        joinGroup(currentUser,groupName);
                        break;
                    case GRPLST:
                        groupList();
                        break;
                    case GRPCLOSE:
                        groupName = (String) obj.get(GRPNAME);
                        closeGroup(currentUser,groupName);
                        break;
                    default:
                        System.out.println("REQUEST TASK DEFAULT: " + currentUser);
                        break;
                }
            }
        } catch(EOFException e){
            System.err.println("EOF EXCEPTION REQUEST TASK");
            if(client.isClosed()) System.err.println("CLOSED REQUEST TASK");
        } catch (SocketException e){
            System.err.println("SOCKET EXCEPTION REQUEST TASK");
        } catch (IOException e) {
            System.err.println("IO EXCEPTION REQUEST TASK");
        } finally{
            close();
            requestOp.putUsrOffline(currentUser);
        }
    }

    /**
     * Registra l'utente e manda la risposta al client.
     *
     * @param currentUser utente da registrare.
     * @param psw password dell'utente da registrare.
     * @param lang lingua predefinita dell'utente da registrare.
     */
    private void register(String currentUser, String psw, String lang) {
        obj = new JSONObject();
        boolean result =requestOp.register(new UserOperation(currentUser, psw, lang));
        if (result) {
            System.out.printf("[REGISTER] from: %s psw: %s lang: %s RESULT: ACK\n",currentUser,psw,lang);
            obj.put(TYPE,ACK);
            obj.put(MSG,NEWUSR);
        } else {
            System.out.printf("[REGISTER] from: %s psw: %s lang: %s RESULT: ALREADY EXIST\n",currentUser,psw,lang);
            obj.put(TYPE,NACK);
            obj.put(MSG,ALRDYEXIST);
        }
        send(obj);
    }

    /**
     * Logga l'utente e manda la risposta al client.
     *
     * @param currentUser utente da loggare.
     * @param psw password dell'utente da loggare.
     */
    private void login(String currentUser, String psw){
        obj = new JSONObject();
        switch(requestOp.login(currentUser,psw)) {
            case OK:
                System.out.printf("[LOGIN] from: %s psw: %s RESULT: ACK\n",currentUser,psw);
                /* Se l'operazione va a buon fine, ricavo la lista dei gruppi e IP. */
                JSONArray groupName = new JSONArray();
                JSONArray groupIp = new JSONArray();
                groupName.addAll(requestOp.getUsrGroupList(currentUser));
                for(Object group : groupName){
                    requestOp.incrOnlineUsrGrp((String) group,currentUser);
                    groupIp.add(requestOp.getGroupIp((String) group));
                }
                obj.put(TYPE,LOGACK);
                obj.put(MSG,LOGGED);
                obj.put(LANG,requestOp.getUserLang(currentUser));
                obj.put(USR, currentUser);
                /* Potrei anche mandargli delle liste vuote. */
                obj.put(GRPNAME,groupName);
                obj.put(IP,groupIp);
                break;
            case WRGUSR:
                System.out.printf("[LOGIN] from: %s psw: %s RESULT: WRONG USERNAME\n",currentUser,psw);
                obj.put(TYPE,NACK);
                obj.put(MSG,WRGUSRNAME);
                break;
            case WRGPSW:
                System.out.printf("[LOGIN] from: %s psw: %s RESULT: WRONG PASSWORD\n",currentUser,psw);
                obj.put(TYPE,NACK);
                obj.put(MSG,WRGPASSWORD);
                break;
            case ALREADY_ONLINE:
                System.out.printf("[LOGIN] from: %s psw: %s RESULT: ALREADY ONLINE\n",currentUser,psw);
                obj.put(TYPE,NACK);
                obj.put(MSG,ALRDYON);
                break;
        }
        send(obj);
    }

    /**
     * Aggiunge una relazione tra l'utente corrente e l'utente
     * di destinazione e manda la risposta al l'utente corrente.
     *
     * @param destUsr utente a cui aggiungere la nuova relazione.
     * @implNote Ogni instanza (thread) ha associato il nome dell'utente a cui
     *           da servizio. La sorgente della nuova relazione è
     *           l'utente associato all'istanza (currentUser).
     */
    private void newRelation(String destUsr){
        obj = new JSONObject();
        SSCode result = requestOp.addRelation(currentUser,destUsr);
        String msg;
        switch (result) {
            case OK:
                System.out.printf("[ADD FRIEND] from: %s to: %s RESULT: ACK\n",currentUser,destUsr);
                msg = newRelationMsg(destUsr);
                obj.put(TYPE,ACK);
                obj.put(MSG,msg);
                break;
            case WRGUSR:
                System.out.printf("[ADD FRIEND] from: %s to: %s RESULT: NOT EXISTS\n",currentUser,destUsr);
                msg = usrNotExist(destUsr);
                obj.put(TYPE,NACK);
                obj.put(MSG,msg);
                break;
            case ALREADY_FRIENDS:
                System.out.printf("[ADD FRIEND] from: %s to: %s RESULT: ALREADY FRIENDS\n",currentUser,destUsr);
                msg = usrAlrdyFriend(destUsr);
                obj.put(TYPE,NACK);
                obj.put(MSG,msg);
                break;
        }
        send(obj);
    }

    /**
     * Ricerca il nome dell'utente se esiste
     * altrimenti manda una risposta di errore.
     *
     * @param tmpUsr nome dell'utente da ricercare.
     */
    private void searchUser(String tmpUsr) {
        obj = new JSONObject();
        if(requestOp.searchUsr(tmpUsr)){
            System.out.printf("[SEARCH USER] from: %s to: %s RESULT: ACK\n",currentUser,tmpUsr);
            obj.put(TYPE,ACK);
            obj.put(MSG,usrExist(tmpUsr));
        }else{
            System.out.printf("[SEARCH FRIEND] from: %s to: %s RESULT: NOT EXISTS\n",currentUser,tmpUsr);
            obj.put(TYPE, NACK);
            obj.put(MSG,usrNotExist(tmpUsr));
        }
        send(obj);
    }

    /**
     * Manda come risposta la lista degli amici dell'utente
     * corrente.
     *
     * @implNote Ogni instanza (thread) ha associato il nome
     *           dell'utente a cui da servizio. Quindi invia
     *           la lista degli amici dell'utente corrente.
     */
    private void friendList() {
        System.out.printf("[FRIEND LIST] from: %s RESULT: ACK\n",currentUser);
        obj = new JSONObject();
        List<String> tmpFriends = requestOp.getFriendsList(currentUser);
        obj.put(TYPE,FRNDACK);
        obj.put(MSG,LSTFRIEND);
        JSONArray arr = new JSONArray();
        arr.addAll(tmpFriends);
        obj.put(LIST,arr);
        send(obj);
    }

    /**
     * Crea il gruppo e invia una risposta
     * che riflette il risultato dell'operazione
     * all'utente.
     *
     * @param currentUser utente creatore del gruppo.
     * @param groupName nome del gruppo.
     */
    private void createGroup(String currentUser,String groupName) {
        obj = new JSONObject();
        SSCode result = requestOp.createGroup(currentUser,groupName);
        switch (result) {
            case OK:
                System.out.printf("[CREATE GROUP] from: %s group: %s RESULT: ACK\n",currentUser,groupName);
                Long port = new Long(requestOp.MULTICAST_PORT);
                String ip = requestOp.getGroupIp(groupName);
                obj.put(TYPE, GRPACK);
                obj.put(MSG, grpCreationSucc(groupName));
                obj.put(GRPNAME, groupName);
                obj.put(IP,ip);
                obj.put(PORT, port);
                break;
            case ALRDY_EXISTS:
                System.out.printf("[CREATE GROUP] from: %s group: %s RESULT: ALREADY EXISTs\n",currentUser,groupName);
                obj.put(TYPE, NACK);
                obj.put(MSG, grpAlrdyExists(groupName));
                break;
            case GRP_FAIL:
                System.out.printf("[CREATE GROUP] from: %s group: %s RESULT: CREATION FAILED\n",currentUser,groupName);
                obj.put(TYPE, NACK);
                obj.put(MSG, grpCreationFailed(groupName));
                break;
        }
        send(obj);
    }

    /**
     * Inserisce l'utente all'interno del gruppo
     * ed invia una risposta che riflette il risultato
     * dell'operazione al mittente della richiesta.
     *
     * @param currentUser utente che effettua la richiesta.
     * @param groupName nome del gruppo.
     */
    private void joinGroup(String currentUser, String groupName){
        obj = new JSONObject();
        SSCode result = requestOp.joinGroup(currentUser,groupName);
        switch (result){
            case OK:
                System.out.printf("[JOIN GROUP] from: %s group: %s RESULT: ACK\n",currentUser,groupName);
                Long port = new Long(requestOp.MULTICAST_PORT);
                String ip = requestOp.getGroupIp(groupName);
                obj.put(TYPE,GRPACK);
                obj.put(MSG,grpJoinSucc(groupName));
                obj.put(GRPNAME,groupName);
                obj.put(IP,ip);
                obj.put(PORT,port);
                break;
            case GRP_NOT_EXIST:
                System.out.printf("[JOIN GROUP] from: %s group: %s RESULT: NOT EXISTS\n",currentUser,groupName);
                obj.put(TYPE, NACK);
                obj.put(MSG,grpNotExist(groupName));
                break;
            case USR_ALRDY_GRP:
                System.out.printf("[JOIN GROUP] from: %s group: %s RESULT: ALREADY IN GROUP\n",currentUser,groupName);
                obj.put(TYPE,NACK);
                obj.put(MSG,usrAlrdyInGrp(groupName));
                break;
        }
        send(obj);
    }

    /**
     * Invia all'utente corrente la lista di tutti
     * i gruppi presenti al momento marcando quelli
     * a cui appartiene.
     */
    private void groupList(){
        System.out.printf("[GROUP LIST] from: %s RESULT: ACK\n",currentUser);
        obj = new JSONObject();
        List<String> tmpUsrGroups = requestOp.getUsrGroupList(currentUser);
        HashSet<String> tmpGroups = requestOp.getGrpList();
        /* Rimuovo l'intersezione tra le due liste. */
        for (String usrGroup : tmpUsrGroups){
            tmpGroups.remove(usrGroup);
        }
        obj.put(TYPE,GRPLST);
        obj.put(MSG,LSTGRP);
        JSONArray usrGroups = new JSONArray();
        JSONArray groups = new JSONArray();
        usrGroups.addAll(tmpUsrGroups);
        groups.addAll(tmpGroups);
        obj.put(LISTUSRGRP,usrGroups);
        obj.put(LIST,groups);
        send(obj);
    }

    /**
     * Effettua la richiesta ed invia una risposta che riflette
     * il risultato dell'operazione all'utente.
     *
     * @param currentUser utente che richiede l'operazione.
     * @param groupName gruppo da chiudere.
     */
    private void closeGroup(String currentUser, String groupName) {
        SSCode result = requestOp.closeGroup(currentUser,groupName);
        obj = new JSONObject();
        switch (result){
            case OK:
                System.out.printf("[CLOSE GROUP] from: %s group: %s RESULT: ACK\n",currentUser,groupName);
                /* La risposta è stata inviata tramite multicast. */
                break;
            case GRP_USR_NOT_ADMIN:
                System.out.printf("[CLOSE GROUP] from: %s group: %s RESULT: NOT ADMIN\n",currentUser,groupName);
                obj.put(TYPE,NACK);
                obj.put(MSG,grpUsrNotAdmin(groupName));
                break;
            case GRP_NOT_EXIST:
                System.out.printf("[CLOSE GROUP] from: %s group: %s RESULT: NOT EXISTS\n",currentUser,groupName);
                obj.put(TYPE,NACK);
                obj.put(MSG,grpNotExist(groupName));
                break;
        }
        if(!result.equals(SSCode.OK)) send(obj);
    }
}
