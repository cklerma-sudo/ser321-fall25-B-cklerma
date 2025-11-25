import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Worker {
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static Scanner scanner = new Scanner(System.in);
    private static int port;
    private static String IP;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Expected: <ip> <port>, missing arguments so setting default values: localhost 9000");
            port = 9000;
            IP = "localhost";
        }
        else {
            IP = args[0];
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Expected an integer, got " + args[1]);
                System.exit(1);
            }
        }

        try {
            socket = new Socket(IP, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String welcomeMessage = in.readLine();
            JSONObject welcomeMessageJSON = new JSONObject(welcomeMessage);
            System.out.println(welcomeMessageJSON.getString("message"));
            while (true) {
                String serverMessage = in.readLine();
                JSONObject request = new JSONObject(serverMessage);
                String type = request.getString("type");
                JSONObject response = new JSONObject();
                int result;
                switch (type) {
                    case "add":
                        System.out.println("Add " + request.getInt("num1") + " and " + request.getInt("num2"));
                        result = scanner.nextInt();
                        response.put("type", "add");
                        response.put("result", result);
                        break;
                    case "sub":
                        System.out.println("Subtract these " + request.getInt("num1") + " - " + request.getInt("num2"));
                        result = scanner.nextInt();
                        response.put("type", "sub");
                        response.put("result", result);
                        break;
                    case "mul":
                        System.out.println("Multiply " + request.getInt("num1") + " by " + request.getInt("num2"));
                        result = scanner.nextInt();
                        response.put("type", "mul");
                        response.put("result", result);
                        break;
                    case "div":
                        System.out.println("Divide these " + request.getInt("num1") + " / " + request.getInt("num2"));
                        result = scanner.nextInt();
                        response.put("type", "div");
                        response.put("result", result);
                        break;
                    case "quit":
                        System.out.println("Leader is Shutting Down");
                        return;
                }
                out.println(response);
                System.out.println("Sent to leader, awaiting consensus");
                String consensusMessage = in.readLine();
                JSONObject consensus = new JSONObject(consensusMessage);
                System.out.println(consensus.getString("consensus"));
                System.out.println("Now awaiting the next one");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            JSONObject quitMessage = new JSONObject();
            quitMessage.put("type", "quit");
            if (socket.isConnected()) out.println(quitMessage);
        }




    }
}
