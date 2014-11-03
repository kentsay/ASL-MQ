package main.java.ch.ethz.systems.asl.util;

import java.util.concurrent.CopyOnWriteArrayList;

public class DataCollector {
    
    public static CopyOnWriteArrayList<Long> msgMidTimeJar  = new CopyOnWriteArrayList<>();
    public static CopyOnWriteArrayList<Long> cliSendTimeJar = new CopyOnWriteArrayList<>();
    public static CopyOnWriteArrayList<Long> cliRecvTimeJar = new CopyOnWriteArrayList<>();
    
    public static void putMsgMidTimeJar(String name, long execTime) {
        switch (name) {
        case "msgMid":
            msgMidTimeJar.add(execTime);    
            break;
        case "cliSend":
            cliSendTimeJar.add(execTime);
            break;
        case "cliRecv":
            cliRecvTimeJar.add(execTime);
            break;
        default:
            break;
        }
    }
    
    public static CopyOnWriteArrayList<Long> getMsgMidTimeJar(String name) {
        CopyOnWriteArrayList<Long> result = new CopyOnWriteArrayList<Long>();
        switch (name) {
        case "msgMid":
            result =  msgMidTimeJar;    
            break;
        case "cliSend":
            result =  cliSendTimeJar;
            break;
        case "cliRecv":
            result =  cliRecvTimeJar;
            break;
        default:
            break;
        }
        return result;
    }

    public static void getStaticData(String name, int reqNumber, long totalTime) {
        long sum = 0;
        for (long time : DataCollector.getMsgMidTimeJar(name))  
            sum += time;
        //System.out.println("Req#\t Time\t Resp\t Throughput\t Recv#");
        System.out.println(reqNumber + "\t " + 
                           sum + "\t " + 
                           sum/reqNumber + "\t " + 
                           reqNumber/(sum/1000) + "\t " + 
                           (DataCollector.getMsgMidTimeJar(name).size()+1) + "\t " + 
                           reqNumber/(totalTime/1000));
    }
}
