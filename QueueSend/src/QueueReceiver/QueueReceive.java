package QueueReceiver;

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
    public final static String QUEUE = "dynamicQueues/mytest-queue";

    private QueueConnectionFactory qconFactory;
    private QueueConnection qcon;
    private QueueSession qsession;
    private QueueReceiver qreceiver;
    private Queue queue;
    private boolean quit = false;

    public void onMessage(Message msg) {
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
    }

    public void init(Context ctx, String queueName) throws NamingException, JMSException {
        qconFactory = (QueueConnectionFactory) ctx.lookup(JMS_FACTORY);

        //*************** Creating Queue Connection using the UserName & Password *************************
        qcon = qconFactory.createQueueConnection("admin", "admin");            //<------------- Change the UserName & Password

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
        if (args.length != 1) {
            System.out.println("Usage: java QueueReceive URL");
            return;
        }
        InitialContext ic = getInitialContext(args[0]);
        QueueReceive qr = new QueueReceive();
        qr.init(ic, QUEUE);
        System.out.println("JMS Ready To Receive Messages (To quit, send a \"quit\" message from QueueSender.class).");

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
        env.put(Context.SECURITY_PRINCIPAL, "admin");
        env.put(Context.SECURITY_CREDENTIALS, "admin");

        return new InitialContext(env);
    }
}
