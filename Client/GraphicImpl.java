import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.io.OutputStream;
import java.io.PrintStream;

class GraphicImpl {
    private final JFrame win ;
    private final Operation op;

    /**
     * Setta le proprietà base della GUI ed imposta
     * il listener per la chiusura della finestra.
     *
     * @param op implementazione delle operazioni che possono essere effettuate.
     */
    public GraphicImpl(Operation op){
        win = new JFrame("Register/Login");
        this.op = op;
        win.pack();
        win.setMinimumSize(new Dimension(500, 400));
        win.addWindowListener(new WindowAdapter(){
            public void windowClosing(java.awt.event.WindowEvent e) {
                win.setVisible(false);
                op.close(0);
            }

        });
    }

    /**
     * Crea l'interfaccia per register/login ed imposta
     * i listener per le funzioni di register e login.
     */
    public void createLogin(){
        JPanel logPanel = new JPanel();
        JButton log = new JButton("Login");
        JButton reg = new JButton("Register");
        JLabel usrLab = new JLabel("Username");
        JLabel pswLab = new JLabel("Password");
        JLabel langLab = new JLabel("Language");
        JTextField usr = new JTextField();
        JTextField psw = new JTextField();
        JTextField lang = new JTextField();
        logPanel.setLayout(new GridLayout(4,2,10,10));
        logPanel.add(usrLab);
        logPanel.add(usr);
        logPanel.add(pswLab);
        logPanel.add(psw);
        logPanel.add(langLab);
        logPanel.add(lang);
        logPanel.add(reg);
        logPanel.add(log);

        win.add(logPanel);

        /* Listener per il pulsante registrazione. */
        reg.addMouseListener(new MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e){
                String tmpUsr = usr.getText();
                String tmpPsw = psw.getText();
                String tmpLang = lang.getText();
                op.register(tmpUsr,tmpPsw,tmpLang);
            }
        });
        /* Listener per il pulsante login. */
        log.addMouseListener(new MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e){
                String tmpUsr = usr.getText();
                String tmpPsw = psw.getText();

                if(op.login(tmpUsr,tmpPsw)) createChatGraphics();
            }
        });
        win.setVisible(true);
    }

    /**
     * Crea la GUI per la chat e imposta i listener
     * per le varie funzioni richieste.
     * Inoltre imposta la JTextArea di notifica come standard output.
     */
    public void createChatGraphics(){
        win.setVisible(false);
        win.setTitle("Social Gossip");
        win.getContentPane().removeAll();
        win.repaint();

        /* Pannello delle richieste. */
        JPanel requestPanel = new JPanel();
        requestPanel.setLayout(new GridLayout(2,4,10,10));

        /* Pannello delle notifiche con autoscroll. */
        JPanel notiPanel = new JPanel();
        notiPanel.setLayout(new GridLayout(1,1,10,10));

        /* Pannello per l'invio dei messaggi. */
        JPanel sendPanel = new JPanel();
        sendPanel.setLayout(new GridLayout(2,3,10,10));

        JTextField searchText = new JTextField();
        searchText.setToolTipText("Search user");

        JButton searchButton = new JButton("Search");

        JButton addFriendButton = new JButton("Add friend");

        JButton friendListButton = new JButton("Friend list");

        JButton createGroupButton = new JButton("Create group");

        JButton joinGroupButton = new JButton("Join group");

        JButton groupListButton = new JButton("Chat list");

        JButton closeGroupButton = new JButton("Close group");

        JTextArea msgText = new JTextArea();
        msgText.setLineWrap(true);
        msgText.setWrapStyleWord(true);

        /* Text area di notifica richieste. */
        JTextArea requestNotiText = new JTextArea();
        JScrollPane scrollReqPane = new JScrollPane(requestNotiText);
        requestNotiText.setWrapStyleWord(true);
        requestNotiText.setLineWrap(true);
        requestNotiText.setEditable(false);

        JTextField msgToText = new JTextField();
        msgToText.setToolTipText("Insert friend's name");

        JButton sendMsgButton = new JButton("Send chat message");

        JButton sendFileButton = new JButton("Send file");

        JButton sendGrpButton = new JButton("Send group message");

        requestPanel.add(searchText);
        requestPanel.add(searchButton);
        requestPanel.add(addFriendButton);
        requestPanel.add(friendListButton);
        requestPanel.add(createGroupButton);
        requestPanel.add(closeGroupButton);
        requestPanel.add(joinGroupButton);
        requestPanel.add(groupListButton);

        notiPanel.add(scrollReqPane);

        sendPanel.add(msgToText);
        sendPanel.add(sendMsgButton);
        sendPanel.add(sendFileButton);
        sendPanel.add(msgText);
        sendPanel.add(sendGrpButton);

        win.add(requestPanel, BorderLayout.NORTH);
        win.add(notiPanel,BorderLayout.CENTER);
        win.add(sendPanel,BorderLayout.SOUTH);

        /* Imposto lo standard output alla text area di notifica. */
        System.setOut(new PrintStream(new OutputStream() {
            public void write(int b) {
                requestNotiText.append(String.valueOf((char)b));
                requestNotiText.setCaretPosition(requestNotiText.getDocument().getLength());
                requestNotiText.update(requestNotiText.getGraphics());
            }
        }));
        /* Listener per il bottone di aggiunta amici. */
        addFriendButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e){
                String tmpUsr = searchText.getText();
                op.addFriend(tmpUsr);
            }
        });
        /* Listener per il bottone di ricerca utenti. */
        searchButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e){
                String tmpUsr = searchText.getText();
                op.searchUser(tmpUsr);
            }
        });
        /* Listener per il bottone per la lista amici. */
        friendListButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e){
                op.friendsList();
            }
        });
        /* Listener per il bottone di invio messaggi. */
        sendMsgButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e){
                String destUsr = msgToText.getText();
                String msg = msgText.getText();
                op.msgToUsr(destUsr,msg);
            }
        });
        /* Listener per il bottone di invio file. */
        sendFileButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e){
                String destUsr = msgToText.getText();
                String path = msgText.getText();
                op.fileTo(destUsr,path);
            }
        });
        /* Listener per il bottone di creazione gruppo. */
        createGroupButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e){
                String groupName = searchText.getText();
                op.createGroup(groupName);
            }
        });
        /* Listener per il bottone di invio messaggio sul gruppo. */
        sendGrpButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e){
                String destGrp = msgToText.getText();
                String msg = msgText.getText();
                op.msgToGroup(destGrp,msg);
            }
        });
        /* Listener per il bottone di iscrizione ad un gruppo. */
        joinGroupButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e){
                String groupName = searchText.getText();
                op.joinGroup(groupName);
            }
        });
        /* Listener per il bottone della lista dei gruppi. */
        groupListButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e){
                op.groupList();
            }
        });
        /* Listener per il bottone di chiusura di un gruppo. */
        closeGroupButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e){
                String groupName = searchText.getText();
                op.closeGroup(groupName);
            }
        });
        win.setVisible(true);
    }
}
