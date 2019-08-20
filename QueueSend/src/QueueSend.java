import java.io.*;

public class QueueSend {

    private String brokerURL = null;
    private String username = null;
    private String password = null;
    private String userQueue = null;
    private long msgCount = 1000;
    private int msgSize = 3000;
    private int threadSize = 1;
    private int connections = 3;
    private static Thread t;

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

    public int getMsgSize() {
        return msgSize;
    }

    public void setMsgSize(int msgSize) {
        this.msgSize = msgSize;
    }

    public int getThreadSize() {
        return threadSize;
    }

    public void setThreadSize(int threadSize) {
        this.threadSize = threadSize;
    }

    public int getConnections() {
        return connections;
    }

    public void setConnections(int connections) {
        this.connections = connections;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java QueueSend URL");
            return;
        }
        QueueSend qs = new QueueSend();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--url"))
                    qs.setBrokerURL(args[i+1]);
            if (args[i].equalsIgnoreCase("--user"))
                    qs.setUsername(args[i+1]);
            if (args[i].equalsIgnoreCase("--password"))
                qs.setPassword(args[i+1]);
            if (args[i].equalsIgnoreCase("--queue"))
                qs.setUserQueue(args[i+1]);
            if ("--message-size".equalsIgnoreCase(args[i]))
                qs.setMsgSize(Integer.valueOf(args[i+1]));
            if ("--message-count".equalsIgnoreCase(args[i]))
                qs.setMsgCount(Long.valueOf(args[i+1]));
            if ("--thread-size".equalsIgnoreCase(args[i]))
                qs.setThreadSize(Integer.valueOf(args[i+1]));
            if ("--connections".equalsIgnoreCase(args[i]))
                qs.setConnections(Integer.valueOf(args[i+1]));
            i++;
        }


        try {
            if (null == qs.getBrokerURL()) {
                qs.setBrokerURL(qs.readAndReturn("Please Enter Broker URL"));
            }
            if (null == qs.getUsername()) {
                qs.setUsername(qs.readAndReturn("Please enter the username"));
            }
            if (null == qs.getPassword())
                qs.setPassword(qs.readAndReturn("Please enter the password"));
            if (null == qs.getUserQueue())
                qs.setUserQueue(qs.readAndReturn("Please enter the queue name"));
        } catch (IOException e){
                e.printStackTrace();
        }

        for (int i = 0; i < qs.getThreadSize(); i++) {
            System.out.println("Thread "+(i+1)+" started sending " + qs.getMsgCount() + " messages.");
            t = new Thread(new SenderThread(qs));
            t.start();

        }

    }

    private String readAndReturn(String quest) throws IOException {
        System.out.println(quest + ": ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String msg = br.readLine();
        br.close();
        return msg;
    }
}
