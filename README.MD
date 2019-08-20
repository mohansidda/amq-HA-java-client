

## Testing 

Download the jar's from this repo. run these commands from the same directory :

**```Sender ```** :
java -jar QueueSend.jar --url (tcp://<host1a>:61616,tcp://<host1b>:61626,tcp://<host2a>:61616,tcp://<host2b>:61626,tcp://<host3a>:61616,tcp://<host3b>:61626)?ha=true&amp;reconnectAttempts=-1&amp;retryInterval=5000&amp;retryIntervalMultiplier=1.0 --user amqadm --password **** --queue mytest-queue --message-size 1000 --message-count 10 --thread-size 2 --connections 2

**```Receiver ```**:
java -jar QueueReceive.jar --url (tcp://<host1a>:61616,tcp://<host1b>:61626,tcp://<host2a>:61616,tcp://<host2b>:61626,tcp://<host3a>:61616,tcp://<host3b>:61626)?ha=true&amp;reconnectAttempts=-1&amp;retryInterval=5000&amp;retryIntervalMultiplier=1.0 --user amqadm --password **** --queue mytest-queue --message-count 1000
