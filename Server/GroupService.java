import java.net.MulticastSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

class GroupService {
    private final int MULTICAST_PORT;
    private final ConcurrentHashMap<String,Group> groups;
    private final MulticastSocket mcs;

    /**
     * Inizializza le operazioni multicast sui gruppi, la socket multicast
     * e la porta da utilizzare.
     *
     * @param groups struttura dati condivisa contenente le info sui gruppi.
     * @param mcs socket multicast per l'invio dei messaggi ai gruppi.
     * @param mcPort porta della socket multicast sulla quale inviare i messaggi ai gruppi.
     */
    public GroupService(ConcurrentHashMap<String,Group> groups,
                        MulticastSocket mcs, int mcPort){
        this.groups = groups;
        this.mcs = mcs;
        this.MULTICAST_PORT = mcPort;
    }

    /**
     * Invia il messaggio sul gruppo multicast
     * definito dal nome del gruppo.
     *
     * @param sourceUsr mittente del messaggio.
     * @param destGrp gruppo di destinazione.
     * @param msg body del messaggio.
     * @return OK se l'invio va a buon fine.
     *         GRP_NO_USR se il mittente non è nel gruppo.
     *         GRP_NO_ON_USR se nessun utente nel gruppo è online.
     *         GRP_SEND_FAIL se l'invio del messaggio fallisce.
     *         GRP_NOT_EXIST se il gruppo non esiste.
     */
    public SSCode sendGrpMsg(String sourceUsr,String destGrp, String msg){
        AtomicReference<SSCode> result = new AtomicReference<>(SSCode.GRP_NOT_EXIST);
        groups.computeIfPresent(destGrp,(k,v)->{
            result.set(v.sendMsg(sourceUsr,msg,mcs,MULTICAST_PORT));
            return v;
        });
        return result.get();
    }
}
