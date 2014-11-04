package main.java.ch.ethz.systems.asl.service.middleware;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import main.java.ch.ethz.systems.asl.bean.ResponseCode;
import main.java.ch.ethz.systems.asl.util.DataCollector;
import main.java.ch.ethz.systems.asl.util.StopWatch;

public class MessageServiceMain implements IService {
    
    public ServerSocket msgServer = null;
    public Socket client = null;
    int numConnections = 0;
    int port;
    String dbFarm;
    ExecutorService threadpool = Executors.newFixedThreadPool(100);
    
    private volatile int state = -1;
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("[Usage]: java MessageServiceMain <Port Number> <DB farm Name>");
        } else {
            int port = Integer.parseInt(args[0]);
            String dbFarm = args[1];
            MessageServiceMain service = new MessageServiceMain(port, dbFarm);
            service.startService();
        }
    }
    
    public MessageServiceMain(int port, String db) {
        this.port = port;
        this.dbFarm = db;
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
        System.out.println("########## Middleware Report" + " ##########");
        System.out.println("Req#\t Time\t Resp\t Rate\t Recv#\t Throughput");
        StopWatch sw = new StopWatch();
        sw.on();
        while (getServiceState() == 1) {
            try {
                client = msgServer.accept();
                numConnections ++;
                MessageService msgSer = new MessageService(client, numConnections, this, dbFarm);
                this.threadpool.execute(msgSer);
                if (numConnections % 1000 == 0) {
                    DataCollector.getStaticData("msgMid", numConnections, sw.off());
                }
                if (sw.off() > 1800000) {
                    DataCollector.getStaticData("msgMid", numConnections, sw.off());
                    setServiceState(STOP);
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
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
