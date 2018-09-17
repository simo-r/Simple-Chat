interface ReplyCode {
    /* Codici del protocollo di comunicazione usati sia server che client side. */

    /* CAMPI */
    String TYPE = "Type";
    String USR = "Usr";
    String PSW = "Psw";
    String LANG = "Lang";
    String MSG = "Msg";
    String LIST = "List";
    String INIT = "Init";
    String FROM = "From";
    String TO = "To";
    String FILENAME = "FileName";
    String IP = "Ip";
    String PORT = "Port";
    String IDE = "Ide";
    String LEN = "Len";
    String GRPNAME = "GroupName";
    String LISTUSRGRP = "ListUserGroup";

    /* TIPI RICHIESTA */
    String REG = "Register";
    String LOG = "Login";
    String ADDFRIEND = "AddFriend";
    String SEARCHUSR = "SearchUser";
    String FRNDLST="FriendsList";
    String CHATMSG ="ChatMessage";
    String FILEMSG = "FileMessage";
    String SOCKETINFO = "SockInfo";
    String GRPCREATE = "GroupCreate";
    String GRPJOIN = "GroupJoin";
    String GRPLST = "GroupList";
    String GRPMSG = "GroupMsg";
    String GRPCLOSE = "GroupClose";

    /* TIPI RISPOSTA */
    String ACK = "ACK";
    String NACK = "NACK";
    /* Login ack */
    String LOGACK = "LACK";
    /* Friends ack */
    String FRNDACK = "FACK";
    /* Group creation/join ack */
    String GRPACK = "GACK";
}
