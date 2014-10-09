package main.java.ch.ethz.systems.asl.service.middleware;

public interface IService {

    public static final int INIT = 1;
    public static final int START = 2;
    public static final int STOP = 3;
    public static final int SHUTDOWN = 4;
    
    int getServiceState();
    void setServiceState(int state);
}
