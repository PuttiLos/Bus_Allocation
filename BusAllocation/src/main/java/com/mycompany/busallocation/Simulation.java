/**
 * This source code is for Project 2.
 * Puttimait Viwatthara    EGCO 6213130
 * Napat     Cheepmuangman EGCO 6213200
 */

import java.util.*;
import java.util.concurrent.*;
import java.io.*;

class BusLine{
    private String destination;
    private int MaxSeat;
    private int NumberOfBus;
    private ArrayList<Bus> BusInLine;
    public BusLine(String DE,int MS,int NOB){destination=DE; MaxSeat=MS; NumberOfBus=NOB; BusInLine = new ArrayList<Bus>();}
    public int getNumberOfBus(){return BusInLine.size();}
    public Bus getBus(int i){return BusInLine.get(i);}
    class Bus{
        private String BusName;
        private int AvailableSeat;
        private int GroupNumber;
        private ArrayList<Group> GroupInBus;
        public int getNumberGroupInBus(){return GroupInBus.size();}
        public Group getGroup(int i){return GroupInBus.get(i);}
        public String getBusName(){return BusName;}
        protected Bus(String BN,int AS){BusName=BN; AvailableSeat=AS; GroupNumber=0; GroupInBus = new ArrayList<Group>();}
        protected void addGroup(Group G){GroupInBus.add(G); GroupNumber++;}
        protected void addTourist(int t){AvailableSeat-=t;}
        
    }
    class Group{
            private String GroupName;
            private int TouristInGroup;
            public String getGroupName(){return GroupName;}
            public int getTouristInGroup(){return TouristInGroup;}
            public Group(String GN,int TIG){GroupName=GN; TouristInGroup=TIG;}
        }
    synchronized public void allocateBus(Data D){
        //System.out.printf("Before %s %d %s\n",Thread.currentThread().getName(),BusInLine.size(),destination);
        //for(int i=0;i<BusInLine.size();++i){System.out.printf("%s %d ",destination,BusInLine.get(i).AvailableSeat);}
    if(BusInLine.size()==0||BusInLine.get(BusInLine.size()-1).AvailableSeat<D.getNumberOfTourist())
    {
        BusInLine.add(new Bus(destination+BusInLine.size(),MaxSeat));
        
        
    }
    for(int i=0;i<BusInLine.size();++i){
        if(BusInLine.get(i).AvailableSeat>=D.getNumberOfTourist()){//Can Book All
           BusInLine.get(i).addGroup(new Group(D.getTourName(),D.getNumberOfTourist()));
           //BusInLine.get(i).AvailableSeat-=D.getNumberOfTourist();
           BusInLine.get(i).addTourist(D.getNumberOfTourist());
           System.out.printf("%8s  >>  Transaction %2d : %20s (%3d seats) bus %4s\n",Thread.currentThread().getName(),D.getTransactionNumber(),D.getTourName(),D.getNumberOfTourist(),BusInLine.get(i).BusName);
           D.booked(D.getNumberOfTourist());
           break;
        }
        else if(BusInLine.get(i).AvailableSeat!=0){
            BusInLine.get(i).addGroup(new Group(D.getTourName(), BusInLine.get(i).AvailableSeat));
            D.booked(BusInLine.get(i).AvailableSeat);
            System.out.printf("%8s  >>  Transaction %2d : %20s (%3d seats) bus %4s\n",Thread.currentThread().getName(),D.getTransactionNumber(),D.getTourName(),BusInLine.get(i).AvailableSeat,BusInLine.get(i).BusName);
            //BusInLine.get(i).AvailableSeat=0;
            BusInLine.get(i).addTourist(BusInLine.get(i).AvailableSeat);
            break;
        }
    }
    //System.out.printf("After %s %d %s\n",Thread.currentThread().getName(),BusInLine.size(),destination);
    }
    
}
        
class Data {

    private int TransactionNumber;
    private String TourName;
    private int NumberOfTourist;
    private String Destination;
    public int getTransactionNumber(){return TransactionNumber;}
    public String getTourName(){return TourName;}
    public int getNumberOfTourist(){return NumberOfTourist;}
    public void booked(int b){NumberOfTourist-=b;}
    public String getDestination(){return Destination;}
    public Data(int NO,String N,int NOS,String DE){TransactionNumber=NO; TourName=N; NumberOfTourist=NOS; Destination=DE;}

    
}
        
class TicketCounter implements Runnable{
    private String name;
    private String filename;
    private ArrayList<Data> TransactionData;
    private BusLine AirBL;
    private BusLine CityBL;
    protected CyclicBarrier Barrier;
    private int checkpoint;
    private boolean check;
    public TicketCounter(String n,BusLine ABL,BusLine CBL,String fn ,int CP,CyclicBarrier C){
        name = n;
        AirBL = ABL;
        CityBL = CBL;
        filename =fn;
        checkpoint = CP;
        Barrier = C;
        check = false;
    }
    synchronized public ArrayList<Data> LoadData() {
        ArrayList<Data> ALD = new ArrayList<Data>();
        try {
                System.out.println(Thread.currentThread().getName() +" >> "+filename+" successfully loaded to " + Thread.currentThread().getName());
                try ( Scanner FileReader = new Scanner(new File(filename))) {
                    while (FileReader.hasNext()) {
                        String line = FileReader.nextLine();
                        String[] buf = line.split(",");
                        ALD.add(new Data(Integer.parseInt(buf[0].trim()), buf[1], Integer.parseInt(buf[2].trim()), buf[3]));
                    }  
                } catch (Exception e) {
                    System.out.println(e + "\nFile not found\n"); //NeverHappend
                }
        } catch (Exception e) {
            System.out.println(e);
        }
        return ALD;

    }
    public void run() {
        TransactionData = this.LoadData();
        for (int i = 0; i < TransactionData.size(); ++i) {
            boolean rerun = false;
            do {
                rerun = false;
                if (i != checkpoint-1 || check) {
                    if (TransactionData.get(i).getDestination().equals(" A")) {
                        AirBL.allocateBus(TransactionData.get(i));
                    } else {
                        CityBL.allocateBus(TransactionData.get(i));
                    }
                } 
                else {
                    try {
                        Barrier.await();
                        check = true;
                        Barrier.await();
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
                if (TransactionData.get(i).getNumberOfTourist() != 0) {
                    rerun = true;
                }
            } while (rerun);
        }
    }
}

public class Simulation {
    private Thread[] T;
    private boolean filemissing;
    private String[] filename;
    private CyclicBarrier CheckpointBarrier;
    private int maxseat,checkpoint;
    private BusLine Airport,City;
    
    
    public Simulation(){
        T = new Thread[3];
        filemissing = false;
        String[] fn = {"T1.txt","T2.txt","T3.txt"}; 
        filename = fn;
        CheckpointBarrier = new CyclicBarrier(4);
        AskMaxSeatAndCheckpoint();
        Airport = new BusLine(" A", maxseat, 0);
        City = new BusLine(" C", maxseat, 0);
        Scanner KB = new Scanner(System.in);
        for (int i = 0; i < 3; ++i) {
                boolean found = false;
                while (!found) {
                    try {
                        Scanner FileFinder = new Scanner(new File(filename[i]));
                        found = true;
                        T[i] = new Thread(new TicketCounter(("T" + i), Airport, City, filename[i], checkpoint, CheckpointBarrier));
                        //T[i].start();
                    } catch (Exception e) {
                        System.out.println("\nFile NO."+ (i+1) +" not found.\nWaiting for new file name...");
                        filename[i] = KB.nextLine();
                    }
                }
            }
            for (int i = 0; i < 3; ++i) {
                T[i].start();
            }
            try {
                CheckpointBarrier.await();
                System.out.printf("\n%8s  >>  %3d airport-bound buses have been allocated.\n%8s  >>  %3d city-bound buses have been allocated.\n\n", 
                        Thread.currentThread().getName(), Airport.getNumberOfBus(), Thread.currentThread().getName(), City.getNumberOfBus());
                CheckpointBarrier.await();

            } catch (Exception e) {
            }
            //PrintingSummation
            try{ T[0].join();T[1].join();T[2].join();}catch(Exception e){}
        System.out.printf("\n\n%8s >>  ===== Airport Bound =====\n",Thread.currentThread().getName());
        for(int i=0;i<Airport.getNumberOfBus();++i)
        {
            System.out.printf("%8s >>  %3s : ",Thread.currentThread().getName(),Airport.getBus(i).getBusName());
            for(int j=0;j<Airport.getBus(i).getNumberGroupInBus();++j)
            {
                System.out.printf("%20s  (%2d seats)",Airport.getBus(i).getGroup(j).getGroupName(),Airport.getBus(i).getGroup(j).getTouristInGroup());
                if(j!=Airport.getBus(i).getNumberGroupInBus()-1){
                    System.out.printf(", ");
                }
            }
            System.out.println("");
        }
        
        System.out.printf("\n\n%8s >>  ===== City Bound =====\n",Thread.currentThread().getName());
        for(int i=0;i<City.getNumberOfBus();++i)
        {
            System.out.printf("%8s >>  %3s : ",Thread.currentThread().getName(),City.getBus(i).getBusName());
            for(int j=0;j<City.getBus(i).getNumberGroupInBus();++j)
            {
                System.out.printf("%20s  (%2d seats)",City.getBus(i).getGroup(j).getGroupName(),City.getBus(i).getGroup(j).getTouristInGroup());
                if(j!=City.getBus(i).getNumberGroupInBus()-1){
                    System.out.printf(", ");
                }
            }
            System.out.println("");
        }
    }
    
    public void AskMaxSeatAndCheckpoint(){
        Scanner KB = new Scanner(System.in);
        System.out.printf("%8s   >>  Enter Maximum Seats : \n", Thread.currentThread().getName());
        maxseat = KB.nextInt();
        System.out.printf("%8s   >>  Enter Checkpoint    : \n", Thread.currentThread().getName());
        checkpoint = KB.nextInt();
        KB.nextLine();
    }
    
    public static void main(String[] Args) {
        new Simulation();
    }

}
