import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class ChatService extends Service {

    /**
     * Inizializza una nuova istanza del servizio di chat
     * che contiene tutti i metodi definiti.
     *
     * @param users struttura dati condivisa che contiene le info degli utenti.
     */
    public ChatService(ConcurrentHashMap<String, UserOperation> users){
        super(users);
    }

    /**
     * Inizializza la socket per l'invio dei messaggi di chat
     * ad un utente.
     *
     * @param usr utente a cui associare la chat socket.
     * @param clientChatWriter writer della chat socket.
     */
    public void initChatSocket(String usr,BufferedWriter clientChatWriter) {
        users.computeIfPresent(usr,(k,v) ->{
            v.setChatWriter(clientChatWriter);
            return v;
        });
    }

    /**
     * Esegue i controlli sul mittente e destinatario,
     * manda il messaggio al destinatario.
     *
     * @param sourceUsr mittente del messaggio.
     * @param destUsr destinatario del messaggio.
     * @param msg body del messaggio.
     * @return OK se l'invio va a buon fine.
     *         OFFLINE se il destinatario è offline.
     *         WRGUSR se il destinatario non esiste.
     *         NOT_FRIENDS se il mittente non è amico.
     *                     del destinatario e viceversa.
     * @implNote ho preferito un'early return in quanto gli if
     *           sarebbero stati meno leggibili.
     */
    public SSCode sendChatMsg(String sourceUsr, String destUsr, String msg){
        /* Costruisco una parte della stringa di richiesta di traduzione. */
        StringBuilder translateRequest = new StringBuilder("https://api.mymemory.translated.net/get?of=json&q=");
        translateRequest.append(msg);
        translateRequest.append("&langpair=");

        if(!users.containsKey(destUsr)){
            /* Se il destinatario non esiste. */
            return SSCode.WRGUSR;
        }
        AtomicBoolean tmpResult = new AtomicBoolean(false);
        users.computeIfPresent(sourceUsr,(k,v) ->{
            /* Controllo se mittente e destinatario sono amici. */
            tmpResult.set(v.checkFriend(destUsr));
            translateRequest.append(v.getLang());
            return v;
        });

        if(!tmpResult.get()){
            /* Se mittente e destinatario NON sono amici. */
            return SSCode.NOT_FRIENDS;
        }
        /* Aggiungo la pipe per la lingua: linguaMittente|linguaDestinazione. */
        translateRequest.append("|");
        users.computeIfPresent(destUsr,(k,v)->{
            translateRequest.append(v.getLang());
            return v;
        });
        String translationResult = doTranslationRequest(translateRequest.toString());
        /* Se la traduzione fallisce mando il messaggio nella lingua del mittente. */
        AtomicReference<SSCode> result = new AtomicReference<>(SSCode.WRGUSR);
        /*
         * La lambda expression non mi permette di usare variabili
         * NON effettivamente final (non posso riassegnare msg)
         * quindi uso un if all'interno.
         */
        users.computeIfPresent(destUsr,(k,v)->{
            if(translationResult != null) result.set(v.sendChatMsg(sourceUsr,translationResult));
            else result.set(v.sendChatMsg(sourceUsr,msg));
            return v;
        });
        return result.get();
    }

    /**
     * Effettua la richiesta di traduzione del messaggio
     * e ritorna la risposta.
     *
     * @param translateRequest url della richiesta di traduzione.
     * @return il messaggio tradotto se la richiesta va a buon fine,
     *         null altrimenti.
     */
    private String doTranslationRequest(String translateRequest){
        String translatedMsg = null;
        StringBuilder result2 = new StringBuilder();
        try {
            URL url = new URL(translateRequest);
            /* HttpURLConnection per singola richiesta ma la TCP sotto può essere riutilizzata. */
            HttpURLConnection uc = (HttpURLConnection) url.openConnection();
            BufferedReader rd = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                result2.append(line);
            }
            rd.close();
            /* Può chiudere la connessione TCP sottostante. */
            uc.disconnect();
            JSONParser parser = new JSONParser();
            JSONObject obj = (JSONObject) parser.parse(result2.toString());
            String status = obj.get("responseStatus").toString();
            if(status.equals("200")){
                JSONObject responseDate = (JSONObject) obj.get("responseData");
                if(responseDate != null) {
                    translatedMsg = (String) responseDate.get("translatedText");
                }
            }
        } catch (MalformedURLException e) {
            System.err.println("MALFORMED URL FOR TRANSLATION");
            translatedMsg = null;
        } catch (IOException e) {
            System.err.println("IO EXCEPTION MESSAGE TRANSLATION");
            translatedMsg = null;
        } catch (ParseException e) {
            System.err.println("PARSING TRANSLATION REPLY EXCEPTION");
            translatedMsg = null;
        }
        return translatedMsg;
    }

    /**
     * Esegue i controlli sul mittente e destinatario,
     * manda la richiesta di invio file al destinatario.
     *
     * @param sourceUsr mittente della richiesta.
     * @param destUsr destinatario della richiesta.
     * @param fileName nome del file da inviare.
     * @param len lunghezza del file da inviare.
     * @param ide identificatore univoco del file.
     * @return OK se l'invio della richiesta va a buon fine.
     *         OFFLINE se il destinatario è offline.
     *         WRGUSR se il destinatario non esiste.
     *         NOT_FRIENDS se il mittente non è amico
     *                     del destinatario o viceversa.
     */
    public SSCode requestFileMsg(String sourceUsr, String destUsr, String fileName, Long len, Long ide){
        if(!users.containsKey(destUsr)) return SSCode.WRGUSR;
        AtomicBoolean tmpResult = new AtomicBoolean(false);
        users.computeIfPresent(sourceUsr,(k,v)->{
            tmpResult.set(v.checkFriend(destUsr));
            return v;
        });
        if(!tmpResult.get()) return SSCode.NOT_FRIENDS;
        AtomicReference<SSCode> result = new AtomicReference<>(SSCode.WRGUSR);
        users.computeIfPresent(destUsr,(k,v)->{
            result.set(v.requestFileMsg(sourceUsr,fileName,len,ide));
            return v;
        });
        return result.get();
    }

    /**
     * Invia le informazioni della socket (IP,porta)
     * per l'invio del file al mittente della richiesta
     * di invio file.
     *
     * @param destUsr mittente della richiesta di invio file.
     * @param ip ip del destinatario.
     * @param port porta del destinatario.
     * @param ide identificatore univoco del file.
     */
    public void sendSocketInfo(String destUsr, String ip, Long port, Long ide) {
        users.computeIfPresent(destUsr,(k,v) ->{
            v.sendSocketInfo(ip,port,ide);
            return v;
        });
    }
}
