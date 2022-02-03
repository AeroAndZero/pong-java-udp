import java.net.*;
import java.io.*;
import java.util.*;

class GameBoard{
    char board[][];
    char sessionBoard[][];
    public int width = 0;
    public int height = 0;
    public int playerSize = 5;
    
    //Physics
    private int bdX = 0;
    private int bdY = 0;
    private int ballX = 0;
    private int ballY = 0;
    
    public GameBoard(int width,int height){
        board = new char[height][width];
        sessionBoard = new char[height][width];
        this.width = width;
        this.height = height;
        
        for(int i = 0; i < height; i++){
            for(int j = 0; j < width; j++){
                board[i][j] = '.';
            }
        }
        
        Random random = new Random(System.currentTimeMillis());
        bdX = -1;
        bdY = (Math.random() <= 0.5) ? -1 : 1;
        
        ballX = (int)width/2;
        ballY = (int)height/2;
    }
    
    public int updatePhysics(){
        int winner = -1;
        
        ballX += bdX;
        ballY += bdY;
        
        if(ballX + bdX >= width || ballX + bdX < 0 || sessionBoard[ballY][ballX + bdX] == 'X'){
            bdX *= -1;
        }
        
        if(ballY + bdY >= height || ballY + bdY < 0 || sessionBoard[ballY + bdY][ballX] == 'X'){
            bdY *= -1;
        }
        
        if(ballX == 0 && sessionBoard[ballY][ballX + bdX] != 'X'){
            winner = 1;
        }else if(ballX == width-1 && sessionBoard[ballY][ballX + bdX] != 'X'){
            winner = 0;
        }
        
        return winner;
    }
    
    /* board rendering methods */
    public void renderGameBoard(){
        for(int i = 0; i < height; i++){
            for(int j = 0; j < width; j++){
                System.out.print(board[i][j] + " ");
            }
            System.out.print('\n');
        }
    }
    
    public void renderGameBoard(int p1pos,int p2pos){
        for(int i = 0; i < height; i++){
            for(int j = 0; j < width; j++){
                
                // rendering ball according to position
                if(i == ballY && j == ballX){
                    System.out.print("O");
                    sessionBoard[i][j] = 'O';
                }
                // rendering player 1 according to position
                else if(j == 0 && i >= p1pos && i < p1pos + playerSize){
                    System.out.print("X");
                    sessionBoard[i][j] = 'X';
                }
                // rendering player 2 according to position
                else if(j == width-1 && i >= p2pos && i < p2pos + playerSize){
                    System.out.print("X");
                    sessionBoard[i][j] = 'X';
                }
                // Actual board
                else{
                    System.out.print(board[i][j]);
                    sessionBoard[i][j] = '.';
                }
                
                // Gotta leave a space everyone
                System.out.print(" ");
            }
            
            System.out.print('\n');
        }
    }
    
    // encodes whole board into single byte array
    public byte[] encodeBytes(){
        byte arr[] = new byte[(width * height) + width];
        int k = 0;
        for(int i = 0; i < height; i++){
            for(int j = 0; j < width; j++){
                arr[k++] = (byte)sessionBoard[i][j];
            }
            arr[k++] = (byte)'\n';
        }
        
        return arr;
    }
    
}

class updatePlayerPosition extends Thread{
    DatagramSocket ds;
    byte rArr[];
    private int p2Pos = 0;
    
    public updatePlayerPosition(DatagramSocket serverSocket, byte arr[]){
        ds = serverSocket;
        rArr = arr;
    }
    
    public int getPosition(){
        return p2Pos;
    }
    
    @Override
    public void run(){
        try {
            while(true){
                DatagramPacket dp = new DatagramPacket(rArr, rArr.length);
                ds.receive(dp);
                
                p2Pos = rArr[0];
            }
        }
        catch(Exception e) {
            System.out.println("error : " + e);
        }
    }
}

class playerController extends Thread{
    private int position = 0;
    private int maxPos = 0;
    private int playerHeight = 0;
    public playerController(int initPos, int maxPos, int playerHeight){
        position = initPos;
        this.maxPos = maxPos;
        this.playerHeight = playerHeight;
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

public class pongServer {
    
    // copied from stackoverflow
    public static void clearScreen() {
        //Clears Screen in java
        try {
            if (System.getProperty("os.name").contains("Windows"))
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            else
                Runtime.getRuntime().exec("clear");
        } catch (IOException | InterruptedException ex) {}
    }
    
    public static void main(String args[]) throws Exception{
        //Game config
        int fps = 2;
        int width = 21;
        int height = 11;
        
        // networking thingy
        DatagramSocket ds = new DatagramSocket(8080); // server socket
        byte rArr[] = new byte[1];  // for receiving player 2 position
        byte sArr[] = new byte[1024];   // for sending the entire game board
            // establish client socket
            updatePlayerPosition upp = new updatePlayerPosition(ds, rArr);
            // Starting client input thread
            upp.start();
            
        // Player controller
        playerController pc = new playerController(4,height,5);
        pc.start(); // Starting another thread of input reader
        
        // start button
        Scanner sc = new Scanner(System.in);
        System.out.println("Press enter to start");
        sc.nextLine();
        
        //init
        GameBoard gb = new GameBoard(width,height);
        
        /* ---- MAIN GAME LOOP ---- */
        int winner = -1;
        while(winner == -1){
            clearScreen();
            winner = gb.updatePhysics();
            gb.renderGameBoard(pc.getPosition(),upp.getPosition());
            
            // Send the entire gameboard to client
            // This means all the physics is handled by the server
            sArr = gb.encodeBytes();
            DatagramPacket dpBoard = new DatagramPacket(sArr, sArr.length, InetAddress.getLocalHost(),8081);
            ds.send(dpBoard);
            
            Thread.sleep((int)1000/fps);
        }
        
        if(winner == 0){
            String serverWon = "----- Server won! -----";
            System.out.println(serverWon);
            sArr = serverWon.getBytes();
            DatagramPacket dpBoard = new DatagramPacket(sArr, sArr.length, InetAddress.getLocalHost(),8081);
            ds.send(dpBoard);
        }else{
            String clientWon = "----- Client won! -----";
            System.out.println(clientWon);
            sArr = clientWon.getBytes();
            DatagramPacket dpBoard = new DatagramPacket(sArr, sArr.length, InetAddress.getLocalHost(),8081);
            ds.send(dpBoard);
        }
    }
}
