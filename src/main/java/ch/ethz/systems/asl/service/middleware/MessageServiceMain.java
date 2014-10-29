package main.java.ch.ethz.systems.asl.service.middleware;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import main.java.ch.ethz.systems.asl.bean.ResponseCode;

public class MessageServiceMain implements IService {
    
    public ServerSocket msgServer = null;
    public Socket client = null;
    int numConnections = 0;
    int port;
    ExecutorService threadpool = Executors.newFixedThreadPool(100);
    
    private volatile int state = -1;
    
    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        MessageServiceMain service = new MessageServiceMain(port);
        service.startService();
    }
    
    public MessageServiceMain(int port) {
        this.port = port;
    }
    
    public void startService() {
        try {
            msgServer = new ServerSocket(port);
            setServiceState(START);
            System.out.println("Message server running");
        } catch (Exception e) {
            e.printStackTrace();
            setServiceState(STOP);
            System.out.println("[Error]: " + ResponseCode.ERROR_SERVER_FAIL.value());
        }
        while (getServiceState() == 1) {
            try {
                client = msgServer.accept();
                numConnections ++;
                //System.out.println("Accept message from client");
                MessageService msgSer = new MessageService(client, numConnections, this);
                this.threadpool.execute(msgSer);
            } catch (Exception e) {
                setServiceState(STOP);
                System.out.println("[Error]: " + ResponseCode.ERROR_SERVER_ACCEPT_FAIL.value());
            }
        }
        threadpool.shutdown();
        System.out.println("Message server shutdown");
    }
    
    public void stopService() throws IOException {
        setServiceState(STOP);
    }

    @Override
    public int getServiceState() {
        return state;
    }

    @Override
    public void setServiceState(int state) {
        this.state = state;
        
    }
}
