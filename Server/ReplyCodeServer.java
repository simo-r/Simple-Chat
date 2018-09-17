interface ReplyCodeServer extends ReplyCode {
    /* Codici e metodi del protocollo di comunicazione usati solo server side. */

    /* TIPI MSG */
    String LOGGED = "Logged in";
    String WRGPASSWORD = "Wrong password";
    String WRGUSRNAME = "Wrong username";
    String ALRDYON = "User already online";
    String ALRDYEXIST = "User already exists";
    String NEWUSR = "User created";
    String LSTFRIEND = "Here is your friends list:";
    String LSTGRP = "Here is your group list:";

    default String newRelationMsg(String usr){
        return "You and " + usr + " are friends now!";
    }

    default String usrExist(String usr){
        return "User " + usr + " exists!";
    }

    default String usrNotExist(String usr){
        return "User " + usr + " does not exist!";
    }

    default String usrAlrdyFriend(String usr){
        return "User " + usr + " is already your friend!";
    }

    default String usrNotFriend(String usr) { return "User " + usr + " is not your friend!";}

    default String usrOffline(String usr) { return "[MSG/FILE] User " + usr + " is offline!";}

    default String senderMsg(String destUsr, String msg) { return "[MSG] You-"+destUsr+": " + msg ;}

    default String senderFile(String destUsr, String fileName){
        return "[FILE] You-"+destUsr+": "+ fileName;
    }

    default String grpAlrdyExists(String groupName){return "[GROUP: "+groupName+"] Group already exists!";}

    default String grpCreationSucc(String groupName) { return "[GROUP: "+groupName+"] Group creation succeed!";}

    default String grpCreationFailed(String groupName) { return "[GROUP: "+groupName+"] Group creation failed!";}

    default String usrNotInGrp(String groupName) { return "[GROUP: "+groupName+"] You are not in this group!";}

    default String usrAlrdyInGrp(String groupName) { return "[GROUP: "+groupName+"] You are already in this group!";}

    default String noOnUsr(String groupName) { return "[GROUP: "+groupName+"] All group members are offline!";}

    default String grpNotExist(String groupName) { return "[GROUP: "+groupName+"] This group does not exists!";}

    default String grpSendFail(String groupName) { return "[GROUP: "+groupName+"] Failed to send the message!";}

    default String grpJoinSucc(String groupName) {return "[GROUP: "+groupName+"] Group join succeed!";}

    default String grpClosed(String groupName) {return "[GROUP: "+groupName+"] Group has been closed!";}

    default String grpUsrNotAdmin(String groupName) {return "[GROUP: "+groupName+"] You can't close this group!"; }
}
