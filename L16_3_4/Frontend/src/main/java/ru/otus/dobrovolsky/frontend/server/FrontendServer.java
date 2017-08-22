package ru.otus.dobrovolsky.frontend.server;

import ru.otus.dobrovolsky.message.Msg;
import ru.otus.dobrovolsky.message.channel.SocketClientChannel;
import ru.otus.dobrovolsky.message.server.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FrontendServer {
    private static final Logger LOGGER = Logger.getLogger(FrontendServer.class.getName());
    private final Address address;
    private final SocketClientChannel client;
    private final Address addressMessageServer = new Address("MsgServerService");
    private Map<String, Object> cacheMap = new HashMap<>();
    private int num;
    private final Address addressDBServer;
    private static final int DELAY = 500;
    private volatile boolean isRegistered = false;

    public FrontendServer(int num, SocketClientChannel client) {
        this.num = num;
        this.address = new Address("Frontend" + this.num);
        this.client = client;

        this.addressDBServer = new Address("DBServer" + num);
    }

    public void start() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        executorService.submit(() -> {
            try {
                while (!isRegistered) {
                    LOGGER.info("Sending registration message:  not registered yet");
                    client.send(new MsgRegistration(address, addressMessageServer));
                    Thread.sleep(DELAY);
                }
            } catch (InterruptedException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
            }
        });

        executorService.submit(this::getMessage);
    }

    @SuppressWarnings("InfiniteLoopStatement")
    private void getMessage() {
        try {
            while (true) {
                Msg receivedMsg = client.take();
                LOGGER.info("RECEIVED MESSAGE:  " + receivedMsg.getClass() + "   from:   " + receivedMsg.getFrom() + " to:   " + receivedMsg.getTo());
                if ((!isRegistered) && (receivedMsg.getClass().getName().equals(MsgRegistrationAnswer.class.getName()))) {
                    LOGGER.info("Receiving registration message answer:  not registered yet");
                    LOGGER.info("Registered on MsgServer successfully");
                    isRegistered = true;
                }
                if (receivedMsg.getClass().getName().equals(MsgGetCacheAnswer.class.getName())) {
                    LOGGER.info("Getting cache information");
                    cacheMap = receivedMsg.getValue();
                }
                if (receivedMsg.getClass().getName().equals(MsgUpdateCacheAnswer.class.getName())) {
                    LOGGER.info("Cache info updated successfully");
                }
                Thread.sleep(DELAY);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Object> getCacheMap() {
        return cacheMap;
    }

    public Address getFrontendAddress() {
        return address;
    }

    public Address getDBServerAddress() {
        return addressDBServer;
    }
}