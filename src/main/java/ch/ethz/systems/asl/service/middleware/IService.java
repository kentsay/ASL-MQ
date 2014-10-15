package main.java.ch.ethz.systems.asl.service.middleware;

public interface IService {

    public static final int START = 1;
    public static final int STOP = 2;
    public static final int SHUTDOWN = 3;
    
    int getServiceState();
    void setServiceState(int state);
}
