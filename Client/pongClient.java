import java.net.*;
import java.util.*;
import java.io.*;

class playerController extends Thread{
    private int position = 0;
    private int maxPos = 0;
    private int playerHeight = 0;
    public playerController(int initPos, int maxPos, int playerHeight){
        position = initPos;
        this.maxPos = maxPos;
        this.playerHeight = playerHeight;
    }
    
    public playerController(int initPos){
        position = initPos;
    }
    
    public void run(){
        while(true){
            if(position < 0){
                position = 0;
            }
            if(position > maxPos - playerHeight){
                position = maxPos - playerHeight;
            }
            
            try{
                Scanner sc = new Scanner(System.in);
                String input = sc.next();
                if(input.equals("w")){
                    position--;
                }else if(input.equals("s")){
                    position++;
                }
            }catch(Exception e){
                System.out.println("Input ma error chhe : " + e);
            }
        }
    }
    
    public int getPosition(){
        return position;
    }
}

class GameBoard extends Thread{
    DatagramSocket ds;
    byte rArr[];
    public GameBoard(DatagramSocket ds, byte arr[]){
        this.ds = ds;
        rArr = arr;
    }
    
    // copied from stackoverflow
    public void clearScreen() {
        //Clears Screen in java
        try {
            if (System.getProperty("os.name").contains("Windows"))
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            else
                Runtime.getRuntime().exec("clear");
        } catch (IOException | InterruptedException ex) {}
    }
    
    public void renderGameBoard(){
        clearScreen();
        System.out.print(" ");
        for(int i = 0; i < rArr.length; i++){
            if((char)rArr[i] != '\0'){
                System.out.print((char)rArr[i] + " ");
            }
        }
    }
    
    @Override
    public void run(){
        try{
            while(true){
                rArr = new byte[1024];
                DatagramPacket dp = new DatagramPacket(rArr, rArr.length);
                ds.receive(dp);
                
                renderGameBoard();
            }
        }catch(Exception e){
            System.out.println("Network ma locha thaya : " + e);
        }
    }
}

public class pongClient {
    
    public static void main(String args[]) throws Exception{
        // game config
        // FPS needs to be same at client and server. This is also known as server tick in other games.
        int fps = 2;
        
        
        // defining network socket
        DatagramSocket ds = new DatagramSocket(8081);
        
        // Need to send only 1 byte for the position
        byte sArr[] = new byte[1];
        // For receive we need width*height amount of bytes
        byte rArr[] = new byte[1024];
        
        // Making a player controller thread for input
        playerController pc = new playerController(0, 11, 5);
        pc.start();
        
        // GameBoard object for receiving and rendering game board
        GameBoard gb = new GameBoard(ds, rArr);
        gb.start();
        
        while(true){
            sArr[0] = (byte)pc.getPosition();
            //Sending player position
            DatagramPacket dp = new DatagramPacket(sArr, sArr.length, InetAddress.getLocalHost(), 8080);
            ds.send(dp);
            Thread.sleep((int)1000/fps);
        }
        
    }
}
