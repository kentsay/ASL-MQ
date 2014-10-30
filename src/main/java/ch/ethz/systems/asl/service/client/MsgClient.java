package main.java.ch.ethz.systems.asl.service.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import main.java.ch.ethz.systems.asl.bean.Message;
import main.java.ch.ethz.systems.asl.bean.MsgFunc;
import main.java.ch.ethz.systems.asl.bean.MsgType;
import main.java.ch.ethz.systems.asl.util.CommonUtil;

public class MsgClient {
    
    String hostname = "localhost";
    Socket clientSocket = null;
    int port = 1999;
    int timeout = 5000;
    ArrayList<String> message = new ArrayList<>();
    
    public MsgClient(String type, String msg) {
        try {
            /*
             * Provide two types to read message:
             * 1. -m: by message content
             * 
             * Append the message content directly.  
             * example: -m <message content>  
             * 
             * 2. -f: by message file
             * 
             * Append multiple message contents in a file
             * example: -f <message file path>
             * 
             * All the two types will store the message into a ArrayList
             */
            switch(type) {
                case "-m":
                    message.add(msg);
                    break;
                case "-f":
                    File fname = new File(msg);
                    if(fname.exists() && !fname.isDirectory()) {
                        BufferedReader br = new BufferedReader(new FileReader(fname));
                        String line = br.readLine();
                        while (line != null) {
                            message.add(line);
                            line = br.readLine();
                        }
                        br.close();
                    }
                    break;
                default:
                    usage();
            }
        } catch (FileNotFoundException e) {
            System.err.println("FileNotFound:  " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IOException:  " + e.getMessage());
        }
    }
    
    public void usage() {
        System.out.println("Unknow type");
        System.out.println("[Usage]: java MsgClient <type> <message data or data file path>");
    }
    
    public void execute() {
        try {
            //Read message content and set into message object
            for (String data: message ) {
                if (data.startsWith("#") || data.equals("")) {
                    //skip the line starting with # or empty, treat as comments 
                    continue;
                } else {
                    //clientSocket = new Socket(hostname, port);
                    clientSocket = new Socket();
                    SocketAddress sockaddr = new InetSocketAddress(hostname, port);
                    clientSocket.connect(sockaddr, timeout);
                    
                    System.out.println("Message Client is action:");
                    ObjectOutputStream objOut = new ObjectOutputStream(clientSocket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

                    Message msg = new Message();
                    System.out.println("Prepare Send out Message");
                    StringTokenizer st = new StringTokenizer(data, ",");
                    System.out.println("sending: " + data);
                    
                    while (st.hasMoreTokens()) {
                        String msgRaw = st.nextToken().trim();
                        String[] msgArray = msgRaw.split(":");
                        switch (msgArray[0]) {
                            case "MsgType":
                                switch (msgArray[1]) {
                                    case "SEND":
                                        msg.setMsgType(MsgType.SEND);
                                        break;
                                    case "RECEIVE":
                                        msg.setMsgType(MsgType.RECEIVE);
                                        break;
                                    case "QUERY":
                                        msg.setMsgType(MsgType.QUERY);
                                        break;
                                }
                                break;
                            case "MsgFunc":
                                switch (msgArray[1]) {
                                    case "CTR_CREATEQ":
                                        msg.setMsgFunc(MsgFunc.CTR_CREATEQ);
                                        break;
                                    case "CTR_DELETEQ":
                                        msg.setMsgFunc(MsgFunc.CTR_DELETEQ);
                                        break;
                                    case "SEND_TOUSER":
                                        msg.setMsgFunc(MsgFunc.SEND_TOUSER);
                                        break;
                                    case "SEND_TOALL":
                                        msg.setMsgFunc(MsgFunc.SEND_TOALL);
                                        break;
                                    case "READ_BYRMVMESSAGE":
                                        msg.setMsgFunc(MsgFunc.READ_BYRMVMESSAGE);
                                        break;
                                    case "READ_BYLOOKMESSAGE":
                                        msg.setMsgFunc(MsgFunc.READ_BYLOOKMESSAGE);
                                        break;
                                    case "QUERY_BYSENDER":
                                        msg.setMsgFunc(MsgFunc.QUERY_BYSENDER);
                                        break;
                                    case "QUERY_BYQUEUE":
                                        msg.setMsgFunc(MsgFunc.QUERY_BYQUEUE);
                                        break;
                                }
                                break;
                            case "MsgSender":
                                msg.setSender(msgArray[1]);
                                break;
                            case "MsgReceiver":
                                msg.setReceiver(msgArray[1]);
                                break;
                            case "MsgQueue":
                                msg.setMsgQueue(msgArray[1]);
                                break;
                            case "MsgDetail":
                                msg.setMsgDetail(msgArray[1]);
                                break;
                        }
                        msg.setMid(CommonUtil.genUUID());
                    }
                    System.out.println("Message Send out");
                    
                    objOut.writeObject(msg);
                    Message r = (Message) in.readObject();
                    System.out.println("Return Message: " + r.getMsgDetail());
                    clientSocket.close();                        
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("Trying to connect to unknown host: " + e.getMessage());
        } catch (SocketTimeoutException e) {
            System.err.println("Connection timeout: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IOException:  " + e.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        for (int i =0 ; i<800; i++) {
            MsgClient sender = new MsgClient("-f", "data/sender");
            sender.execute();
            //MsgClient receiver = new MsgClient("-f", "data/receiver");
            //receiver.execute();   
        }
    }
}
