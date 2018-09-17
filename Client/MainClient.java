class MainClient {
    private static final int STD_REQUEST_PORT = 10000;
    private static final int STD_REGISTRY_PORT = 10001;
    private static final int STD_CHAT_PORT = 10002;
    private static final int STD_GROUP_PORT =10003;
    private static final int STD_MC_GROUP_PORT = 10004;
    private static final int MIN_PORT_NUMBER = 1024;
    private static final int MAX_PORT_NUMBER = 65535;
    private static final String SERVER_NAME = "localhost";

    public static void main(String[] args) {
        if(args.length >0 && args[0].equals("TEST")){
            testMode(args);
        }else{
            clientMode(args);
        }
    }

    /**
     * Crea un client in "test mode" che utilizza le info standard del server
     * ed esegue le operazioni contenute dentro args.
     *
     * @param args operazioni da testare.
     */
    private static void testMode(String[] args){
        Client c = new Client(SERVER_NAME,STD_REQUEST_PORT,STD_REGISTRY_PORT,STD_CHAT_PORT,STD_GROUP_PORT,STD_MC_GROUP_PORT);
        c.test(args);
    }

    /**
     * Crea un client in "normal mode".
     *
     * @param args address e porte del server.
     */
    private static void clientMode(String[] args){
        int requestPort = STD_REQUEST_PORT;
        int registryPort = STD_REGISTRY_PORT;
        int chatPort = STD_CHAT_PORT;
        int groupPort = STD_GROUP_PORT;
        /* Porta usata dal multicast. */
        int mcGroupPort = STD_MC_GROUP_PORT;
        String serverName = SERVER_NAME;
        try {
            switch (args.length) {
                case 6:
                    mcGroupPort = Integer.parseInt(args[5]);
                case 5:
                    groupPort = Integer.parseInt(args[4]);
                case 4:
                    chatPort = Integer.parseInt(args[3]);
                case 3:
                    registryPort = Integer.parseInt(args[2]);
                case 2:
                    requestPort = Integer.parseInt(args[1]);
                case 1:
                    serverName = args[0];
                    break;
                default:
                    break;
            }
        }catch(NumberFormatException e){
            System.out.println("java MainClient [serverName] [requestPort]" +
                    " [registryPort] [chatPort] [groupPort] [mcGroupPort]");
            System.exit(1);
        }
        if(requestPort < MIN_PORT_NUMBER || registryPort < MIN_PORT_NUMBER ||
                chatPort < MIN_PORT_NUMBER || groupPort < MIN_PORT_NUMBER ||
                mcGroupPort < MIN_PORT_NUMBER || requestPort > MAX_PORT_NUMBER ||
                registryPort > MAX_PORT_NUMBER || chatPort > MAX_PORT_NUMBER ||
                groupPort > MAX_PORT_NUMBER || mcGroupPort > MAX_PORT_NUMBER){
            System.out.println("Insert port between [1024,65535]");
            System.exit(1);
        }
        System.setProperty("java.net.preferIPv4Stack", "true");
        Client c = new Client(serverName,requestPort,registryPort,chatPort,groupPort,mcGroupPort);
        c.start();
    }

}
