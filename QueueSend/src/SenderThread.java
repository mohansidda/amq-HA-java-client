import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;

public class SenderThread implements Runnable {

    public final static String JNDI_FACTORY = "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory";
    //*************** Connection Factory JNDI name *************************
    public final static String JMS_FACTORY = "ConnectionFactory";

    private QueueConnectionFactory qconFactory;
    private QueueConnection qcon;
    private QueueSession qsession;
    private QueueSender qsender;
    private Queue queue;
    private TextMessage msg;
    private BytesMessage byteMsg;
    private QueueSend qs;
    private Thread t;

    public SenderThread( QueueSend queueSend) {
        qs = queueSend;
    }

    private void init(Context ctx, String queueName, QueueSend qs) throws NamingException, JMSException {
        qconFactory = (QueueConnectionFactory) ctx.lookup(JMS_FACTORY);
        qcon = qconFactory.createQueueConnection(qs.getUsername(), qs.getPassword());
        qsession = qcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        queue = (Queue) ctx.lookup(queueName);
        qsender = qsession.createSender(queue);
        msg = qsession.createTextMessage();
        qcon.start();
    }

    public void send(String message) throws JMSException {
        msg.setText(message);
//msg.setStringProperty("JMSXGroupID", "RHN");

        qsender.send(msg);
//        qsender.send(byteMsg);
    }

    public void close() throws JMSException {
        qsender.close();
        qsession.close();
        qcon.close();
    }

    private void sendMsgs(QueueSend queueSend) throws JMSException {
        for (int i = 0; i < qs.getMsgCount(); i++) {
            byteMsg = qsession.createBytesMessage();
            byteMsg.writeBytes(new byte[queueSend.getMsgSize()]);
            qsender.send(byteMsg);
        }
    }

    private void readAndSend(QueueSend qs) throws IOException, JMSException {
        String line = "Test Message Body with counter = ";
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        boolean readFlag = true;
        System.out.println("ntStart Sending Messages (Enter QUIT to Stop):n");
        while (readFlag) {
            System.out.print("<Msg_Sender> ");
            String msg = br.readLine();
            if (msg.equals("QUIT") || msg.equals("quit")) {
                send(msg);
                System.exit(0);
            }
            send(msg);
            System.out.println();
        }
        br.close();
    }

    private InitialContext getInitialContext(String url) throws NamingException {
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);
        env.put(Context.PROVIDER_URL, url);
        return new InitialContext(env);
    }

    @Override
    public void run() {
        for (int i = 0; i < qs.getConnections(); i++) {
            InitialContext ic = null;
            try {
                ic = getInitialContext(qs.getBrokerURL());
//            qs = new QueueSend();
                init(ic, qs.getUserQueue(), qs);
//            readAndSend(qs);
                sendMsgs(qs);
                close();
            } catch (NamingException e) {
                e.printStackTrace();
            } catch (JMSException jme) {
                jme.printStackTrace();
            }
        }
    }
}
