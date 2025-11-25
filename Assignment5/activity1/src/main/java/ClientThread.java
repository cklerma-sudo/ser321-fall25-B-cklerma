import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;

public class ClientThread extends Thread {
	private final Socket socket;
	private final BufferedReader bufferedReader;
	private final ServerThread serverThread;

	public ClientThread(Socket socket, ServerThread serverThread) throws IOException {
		this.socket = socket;
		this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.serverThread = serverThread;
	}

	public void run() {
		try {
			String line;
			while ((line = bufferedReader.readLine()) != null) {

				if (line.startsWith("HELLO ")) {
					int remotePort = Integer.parseInt(line.substring("HELLO ".length()).trim());
					serverThread.handleHello(remotePort);
					PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
					out.println(serverThread.buildPeersMessage());

				} else if (line.startsWith("PEERS ")) {
					String portsPart = line.substring("PEERS ".length()).trim();
					String[] parts = portsPart.split(",");
					Set<Integer> ports = new HashSet<>();
					for (String p : parts) {
						if (!p.isEmpty()) {
							ports.add(Integer.parseInt(p.trim()));
						}
					}
					serverThread.handlePeersList(ports);

				} else {
					JSONObject json = new JSONObject(line);
					System.out.println("[" + json.getString("username") + "]: " + json.getString("message"));
				}
			}
		} catch (Exception e) {
			interrupt();
		}
	}
}
