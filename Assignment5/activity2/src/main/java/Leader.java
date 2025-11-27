import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import org.json.*;

public class Leader {
    private static Socket socket;
    private static int port;
    private static List<Socket> workers = Collections.synchronizedList(new ArrayList<>());
    private static int workerCount;
    private static Map<Integer, List<Integer>> results = Collections.synchronizedMap(new HashMap<>());
    private static int taskId = 0;

    public static void main(String[] args){
        if (args.length != 1){
            System.out.println("No arguments given, setting to default port: 9000");
            port = 9000;
        }
        else {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Port must be an integer");
                System.exit(1);
            }
        }

        try {
            ServerSocket serv = new ServerSocket(port);
            workerCount = 0;
            System.out.println("Waiting for at least 3 Workers to connect...");
            while (true) {
                socket = serv.accept();
                workerCount++;
                System.out.println("Worker " + workerCount + " connected");
                workers.add(socket);

                Thread t = new Thread(() -> handleClient(socket, workerCount));
                t.start();
                if (workerCount == 3) break;
            }
            Thread serverThread = new Thread(() -> serverInput());
            serverThread.start();
            while (serverThread.isAlive()) {
                socket = serv.accept();
                workerCount++;
                System.out.println("Worker " + workerCount + " connected");
                workers.add(socket);

                Thread t = new Thread(() -> handleClient(socket, workerCount));
                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    private static void handleClient(Socket clientSocket, int workerId){
        BufferedReader in = null;
        PrintWriter out = null;

        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            JSONObject welcome = new JSONObject();
            String hello = "You connected Successfully! We are waiting the next instruction.";
            welcome.put("type", "welcome");
            welcome.put("message", hello);
            sendToOneClient(out, welcome);
            while (true) {
                String response = in.readLine();
                JSONObject rep = new JSONObject(response);
                String type = rep.getString("type");
                if (type.equals("quit")){
                    return;
                }
                if (type.equals("add") || type.equals("sub") || type.equals("mul") || type.equals("div")){
                    int result = rep.getInt("result");
                    List<Integer> list = results.get(taskId);
                    if (list != null){
                        list.add(result);
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Worker " + workerId + " had an error:" + e.getMessage());
        } finally {
            workers.remove(clientSocket);
            workerCount--;
            if (clientSocket != null){
                try {
                    clientSocket.close();
                }
                catch (IOException e) {e.printStackTrace();}
            }
        }
    }

    private static void sendToOneClient(PrintWriter out, JSONObject message){
        try {
            out.println(message.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void serverInput(){
        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.println("What would you like to do?");
            System.out.printf("1. Addition \n2. Subtraction\n3. Multiplication\n4. Division\n5. Quit\n");
            int choice = in.nextInt();
            int num1 = 0, num2 = 0;
            String opt = null;
            switch (choice) {
                case 1:
                    opt = "add";
                    System.out.println("What is the first number?");
                    num1 = in.nextInt();
                    System.out.println("What is the second number?");
                    num2 = in.nextInt();
                    break;
                case 2:
                    opt = "sub";
                    System.out.println("What is the first number?");
                    num1 = in.nextInt();
                    System.out.println("What is the second number?");
                    num2 = in.nextInt();
                    break;
                case 3:
                    opt = "mul";
                    System.out.println("What is the first number?");
                    num1 = in.nextInt();
                    System.out.println("What is the second number?");
                    num2 = in.nextInt();
                    break;
                case 4:
                    opt = "div";
                    System.out.println("What is the first number?");
                    num1 = in.nextInt();
                    System.out.println("What is the second number?");
                    num2 = in.nextInt();
                    break;
                case 5:
                    System.out.println("Quitting");
                    sendRequestToAll("quit");
                    System.exit(0);
                default:
                    System.out.println("Invalid choice");
            }
            if (opt != null) {
                taskId++;
                results.put(taskId, Collections.synchronizedList(new ArrayList<>()));
                sendRequestToAll(num1, num2, opt);
                System.out.println("Tallying up the responses...");
                String result = findConsensus();
                System.out.println(result);
                sendConsensusToAll(result);
            }
        }

    }

    private static void sendRequestToAll(int num1, int num2, String opt){
        JSONObject request = new JSONObject();
        request.put("num1", num1);
        request.put("num2", num2);
        request.put("type", opt);
        synchronized (workers) {
            try {
                for (Socket socket : workers) {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(request);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private static void sendRequestToAll(String opt){
        JSONObject request = new JSONObject();
        request.put("type", opt);
        try {
            for (Socket socket : workers) {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(request);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendConsensusToAll(String result) {
        JSONObject response = new JSONObject();
        response.put("type", "consensus");
        response.put("consensus", result);

        try {
            for (Socket socket : workers) {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static String findConsensus(){
        while (results.get(taskId).size() != workerCount);
        List<Integer> votes = results.get(taskId);
        Map<Integer, Integer> counts = new HashMap<>();

        for (Integer vote : votes){
            if (!counts.containsKey(vote)){
                counts.put(vote, 1);
            }
            else {
                counts.put(vote, counts.get(vote) + 1);
            }
        }

        int maxCount = 0;
        List<Integer> maxValues = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : counts.entrySet()){
            int value = entry.getKey();
            int count = entry.getValue();

            if (count > maxCount) {
                maxCount = count;
                maxValues.clear();
                maxValues.add(value);
            } else if (count == maxCount) {
                maxValues.add(value);
            }
        }


        if (maxCount >= (workerCount + 1) / 2){
            if (maxValues.size() > 1) {
                int chosen = ThreadLocalRandom.current().nextInt(maxValues.size());
                int winner = maxValues.get(chosen);

                StringBuilder sb = new StringBuilder();
                sb.append("Tie detected among values:\n");
                for (int v : maxValues) {
                    sb.append(" - " + v + " (" + counts.get(v) + " votes)\n");
                }

                sb.append("Randomly selected winner: " + winner + "\n");
                return sb.toString();
            }
            else {
                return "Consensus reached: " + maxValues.get(0) + " (" + maxCount + "/" + workerCount + " workers agreed)\n";
            }
        }

        else{
            StringBuilder sb = new StringBuilder();
            sb.append("There is no majority, consensus not reached. Here is the spread of votes: ");
            for (Map.Entry<Integer, Integer> entry : counts.entrySet()){
                sb.append(entry.getValue() + " votes for " + entry.getKey() + ".\n");
            }
            return sb.toString();
        }



    }


}

