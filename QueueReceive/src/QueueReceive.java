import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class QueueReceive implements MessageListener {
    public final static String JNDI_FACTORY = "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory";

    //*************** Connection Factory JNDI name *************************
    public final static String JMS_FACTORY = "ConnectionFactory";

    //*************** Remote enabled Queue JNDI name *************************
    public final static String QUEUE = "dynamicQueues/mytest-queue-b3";

    private QueueConnectionFactory qconFactory;
    private QueueConnection qcon;
    private QueueSession qsession;
    private QueueReceiver qreceiver;
    private Queue queue;
    private boolean quit = false;
    private int counter = 1;
    private String brokerURL = null;
    private String username = null;
    private String password = null;
    private String userQueue = null;
    private long msgCount = -1;

    /*public void onMessage(Message msg) {
        try {
            String msgText;
            if (msg instanceof TextMessage) {
                msgText = ((TextMessage) msg).getText();
            } else {
                msgText = msg.toString();
            }
            if (msgText.equalsIgnoreCase("hello world")) {
                System.out.println("\n\t " + msgText + ", hence rolling back");
                // Here we are rolling back the session.
                // The "Hello World" message which we received is not committed, hence it's undelivered and goes back to the TestQ
                qsession.rollback();
            } else {
                System.out.println("\n\t " + msgText);
                // Here we are committing the session to acknowledge that we have received the message from the TestQ
                qsession.commit();
            }

            if (msgText.equalsIgnoreCase("quit")) {
                synchronized (this) {
                    quit = true;
                    this.notifyAll();
                }
            }
        } catch (JMSException jmse) {
            jmse.printStackTrace();
        }
    }*/

    public String getBrokerURL() {
        return brokerURL;
    }

    public void setBrokerURL(String brokerURL) {
        this.brokerURL = brokerURL;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserQueue() {
        return userQueue;
    }

    public void setUserQueue(String userQueue) {
        this.userQueue = "dynamicQueues/"+userQueue;
    }

    public long getMsgCount() {
        return msgCount;
    }

    public void setMsgCount(long msgCount) {
        this.msgCount = msgCount;
    }

    @Override
    public void onMessage(Message message) {
        try {
            System.out.println("Message Received : " + counter);
            qsession.commit();
            if (counter==this.getMsgCount()){
                synchronized (this) {
                    quit = true;
                    this.notifyAll();
                }
            }
            counter++;
        }catch (JMSException e){
            e.printStackTrace();
        }
    }

    public void init(Context ctx, String queueName) throws NamingException, JMSException {
        qconFactory = (QueueConnectionFactory) ctx.lookup(JMS_FACTORY);

        //*************** Creating Queue Connection using the UserName & Password *************************
        qcon = qconFactory.createQueueConnection(this.getUsername(), this.getPassword());            //<------------- Change the UserName & Password

        //Creating a *transacted* JMS Session to test our DLQ
        qsession = qcon.createQueueSession(true, 0);

        queue = (Queue) ctx.lookup(queueName);
        qreceiver = qsession.createReceiver(queue);
        qreceiver.setMessageListener(this);
        qcon.start();
    }

    public void close() throws JMSException {
        qreceiver.close();
        qsession.close();
        qcon.close();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java QueueReceive URL");
            return;
        }

        QueueReceive qr = new QueueReceive();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--url"))
                qr.setBrokerURL(args[i+1]);
            if (args[i].equalsIgnoreCase("--user"))
                qr.setUsername(args[i+1]);
            if (args[i].equalsIgnoreCase("--password"))
                qr.setPassword(args[i+1]);
            if (args[i].equalsIgnoreCase("--queue"))
                qr.setUserQueue(args[i+1]);
            if ("--message-count".equalsIgnoreCase(args[i]))
                qr.setMsgCount(Long.valueOf(args[i+1]));
            i++;
        }


        try {
            if (null == qr.getBrokerURL()) {
                qr.setBrokerURL(qr.readAndReturn("Please Enter Broker URL"));
            }
            if (null == qr.getUsername()) {
                qr.setUsername(qr.readAndReturn("Please enter the username"));
            }
            if (null == qr.getPassword())
                qr.setPassword(qr.readAndReturn("Please enter the password"));
            if (null == qr.getUserQueue())
                qr.setUserQueue(qr.readAndReturn("Please enter the queue name"));
            if (qr.getMsgCount() == -1)
                qr.setMsgCount(1000);
        } catch (IOException e){
            e.printStackTrace();
        }

        InitialContext ic = getInitialContext(qr.getBrokerURL());
        qr.init(ic, qr.getUserQueue());
        System.out.println("JMS Ready To Receive Messages from "+qr.getUserQueue());

        synchronized (qr) {
            while (!qr.quit) {
                try {
                    qr.wait();
                } catch (InterruptedException ie) {
                }
            }
        }
        qr.close();
    }

    private static InitialContext getInitialContext(String url) throws NamingException {
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);
        env.put(Context.PROVIDER_URL, url);

//*************** UserName & Password for the Initial Context for JNDI lookup *************************
        env.put(Context.SECURITY_PRINCIPAL, "amqadm");
        env.put(Context.SECURITY_CREDENTIALS, "amqadm");

        return new InitialContext(env);
    }

    private String readAndReturn(String quest) throws IOException {
        System.out.println(quest + ": ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String msg = br.readLine();
        br.close();
        return msg;
    }
}
