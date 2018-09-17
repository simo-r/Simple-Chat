import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;

class Task implements ReplyCodeServer  {
    final Socket client;
    protected BufferedWriter writer;
    private BufferedReader reader;
    protected String currentUser;
    protected JSONObject obj;

    /**
     * Inizializza i lati in lettura/scrittura della socket
     * che verranno condivisi fra tutti gli utilizzatori in modo
     * da garantire la thread-safety.
     *
     * @param client socket del client.
     */
    Task(Socket client){
        this.client = client;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
        } catch (IOException e) {
            System.err.println("UNABLE TO OPEN I/O WITH CLIENT");
            close();
        }
    }

    /**
     * Riceve dalla socket.
     *
     * @return JSONObject contenente la richiesta del client,
     *         null se la fine dello stream è stato raggiunta.
     * @throws IOException se la lettura dalla socket fallisce.
     * @throws ParseException se il parsing della richiesta fallisce.
     */
    JSONObject receive() throws IOException,ParseException{
        String json = reader.readLine();
        JSONParser parser = new JSONParser();
        if(json == null){
            return null;
        }
        return (JSONObject) parser.parse(json);
    }

    /**
     * Invia sulla socket.
     *
     * @param obj risposta da inviare al client.
     * @implNote il line separator va aggiunto in questo modo
     *           altrimenti l'operazione non è atomica/sincronizzata
     *           e potrebbe creare inconsistenze.
     */
    void send(JSONObject obj){
        try{
            writer.write(obj.toJSONString() +System.getProperty("line.separator"));
            writer.flush();
            System.out.printf("Send to: %s JSON: %s\n",currentUser,obj.toJSONString());
        } catch (IOException e) {
            System.err.printf("FAILED TO SEND TO: %s\n",currentUser);
        }
    }

    /**
     * Cambia lo status dell'utente associato a questa
     * socket con OFFLINE e chiude la socket.
     */
    void close(){
        if(client != null && !client.isClosed()) {
            try {
                client.close();
            } catch (IOException e1) {
                System.err.printf("FAILED TO CLOSE SOCKET WITH: %s\n", currentUser);
            }
        }
    }
}
