import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;

class Receiver implements ReplyCode {
    private final BufferedReader reader;

    /**
     * Inizializza le informazioni per la lettura
     * dalla socket.
     *
     * @param reader lato in lettura sulla socket.
     */
    Receiver(BufferedReader reader){
        this.reader = reader;
    }

    /**
     * Legge dalla socket.
     *
     * @return JSONObject contenente la risposta del server.
     * @throws IOException se la lettura dalla socket fallisce.
     * @throws ParseException se il parsing della richiesta fallisce.
     */
    JSONObject receive() throws IOException,ParseException{
        String json = reader.readLine();
        JSONParser parser = new JSONParser();
        if(json == null){
            System.out.println("CHAT EOS");
            return null;
        }
        return (JSONObject) parser.parse(json);
    }
}
