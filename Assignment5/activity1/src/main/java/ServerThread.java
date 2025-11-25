import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class ServerThread extends Thread {
	private ServerSocket serverSocket;
	private Set<Socket> listeningSockets = new HashSet<>();
	private Set<Integer> knownPeerPorts = new HashSet<>();

	public ServerThread(String portNum) throws IOException {
		serverSocket = new ServerSocket(Integer.parseInt(portNum));
		knownPeerPorts.add(serverSocket.getLocalPort()); // know myself
	}

	public int getLocalPort() {
		return serverSocket.getLocalPort();
	}

	public void run() {
		try {
			while (true) {
				Socket sock = serverSocket.accept();
				System.out.println("Someone Connected");
				listeningSockets.add(sock);
				ClientThread clientThread = new ClientThread(sock, this);
				clientThread.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized void connectToPeer(int peerPort) {
		int myPort = serverSocket.getLocalPort();
		if (peerPort == myPort) return;
		if (knownPeerPorts.contains(peerPort)) return;

		try {
			Socket socket = new Socket("localhost", peerPort);
			listeningSockets.add(socket);
			knownPeerPorts.add(peerPort);

			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			out.println("HELLO " + myPort);
			ClientThread clientThread = new ClientThread(socket, this);
			clientThread.start();

			System.out.println("Connected to peer on port " + peerPort);
		} catch (IOException e) {
			System.out.println("Could not connect to peer on port " + peerPort);
		}
	}

	public synchronized void handleHello(int remotePort) {
		if (!knownPeerPorts.contains(remotePort)) {
			knownPeerPorts.add(remotePort);
		}

	}

	public synchronized String buildPeersMessage() {
		StringBuilder sb = new StringBuilder();
		sb.append("PEERS ");
		boolean first = true;
		for (int p : knownPeerPorts) {
			if (!first) sb.append(",");
			sb.append(p);
			first = false;
		}
		return sb.toString();
	}

	public synchronized void handlePeersList(Set<Integer> ports) {
		for (int p : ports) {
			connectToPeer(p);
		}
	}

	public synchronized void addSocket(Socket socket) {
		listeningSockets.add(socket);
	}

	public synchronized void sendMessage(String message) {
		try {
			for (Socket s : listeningSockets) {
				PrintWriter out = new PrintWriter(s.getOutputStream(), true);
				out.println(message);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
