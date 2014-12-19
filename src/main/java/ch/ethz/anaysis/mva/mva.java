package main.java.ch.ethz.anaysis.mva;

import java.text.DecimalFormat;

public class mva {
    public int N = 50; //number of users
    public int Z = 0; //think time
    public int M = 2; //number of devices
    public Double[] S, V, R, Q; 
    
    public void init(int size) {
        S = new Double[size]; //service rate array
        V = new Double[size]; //visitors array
        Q = new Double[size]; //jobs in queue array
        R = new Double[size]; //response time array
        //initialize array
        for(int i=0; i< size; i++) {
            S[i] = 0.0;
            R[i] = 0.0;
            Q[i] = 0.0;
        }
    }
    
    public void setup() {
        S[0] = 0.05; S[1] = 0.001; //S[2] = 0.049;
        V[0] =545.0;V[1] = 545.0; //V[2] = 102.0;
    }
    
    private Double responeTime(double service, double qlength) {
        double resp = 0.0;
        resp = service*(1+qlength);
        return resp;
    }
        
    public void simulation() {
        init(M);
        setup();
        DecimalFormat df = new DecimalFormat("#.###");
        System.out.println("No\tRp_1\tRp_2\tSystem\tT_put\tQ_1\tQ_2");
        System.out.println("------------------------------------------------------");
        for(int i =1; i<= N; i++) {
            System.out.print(i +"\t");
            double responseTime = 0.0; 
            double throughtput = 0.0;
            for(int j=0; j<M; j++) {
                R[j] = responeTime(S[j],Q[j]);
                responseTime = responseTime + R[j]*V[j];
            }
            throughtput = i/(responseTime+Z);
            for(int j=0; j<M; j++) {
                Q[j] = throughtput*R[j]*V[j];
                System.out.print(df.format(R[j]) + "\t");
            }
            System.out.print(df.format(responseTime ) + "\t");
            System.out.print(df.format(throughtput) + "\t");
            for(int j=0; j<M; j++) {
                System.out.print(df.format(Q[j]) + "\t");
            }
            System.out.println();
        }
    }
    
    public static void main(String[] args) {
        mva sim = new mva();
        sim.simulation();
    }
}
