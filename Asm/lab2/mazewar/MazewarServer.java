import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.jmx.snmp.tasks.ThreadService;

public class MazewarServer {

	private static final int PACKET_QUEUE_SIZE = 1000;
	private static final int MAX_PLAYERS = 4;

	private AtomicBoolean isShutDown;
	
	public ArrayBlockingQueue<MazewarPacket> packetQueue;
	public AtomicInteger clientCount;
	public AtomicInteger sequenceNumber;
	public Socket[] clientSockets;
	public int numPlayers;
	public Set<Integer> activePlayers;
	public String[] playerNames;
	
	private Scanner sc;
	private ServerSocket serverSocket;

	public MazewarServer(int port) {
		packetQueue = new ArrayBlockingQueue<MazewarPacket>(PACKET_QUEUE_SIZE);
		clientCount = new AtomicInteger(0);
		sequenceNumber = new AtomicInteger(0);
		isShutDown = new AtomicBoolean(false);
		numPlayers = 4;
		
		boolean validInput;

		try {
			serverSocket = new ServerSocket(port);
		} 
		catch (IOException e) {
			System.err.println("ERROR: Could not open server socket on port " + port);
			System.exit(-1);
		}


		sc = new Scanner(System.in);
		
		validInput = false;
		while (!validInput) {
			System.out.print("Enter the number of players (4 max):\n");
			try {
				numPlayers = Integer.parseInt(sc.nextLine());
				if (numPlayers > 0 && numPlayers <= MAX_PLAYERS)
					validInput = true;
			}
			catch (NumberFormatException e) {
				// retry
			}
		}

		activePlayers = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
		playerNames = new String[numPlayers];
		clientSockets = new Socket[numPlayers];
		System.out.println("Waiting for players to join");
		
		for (int i = 0; i < numPlayers; i++) {
			try {
				clientSockets[i] = serverSocket.accept();
				activePlayers.add(i);
				System.out.println("Player " + i + " has joined");
			}
			catch (IOException e) {
				System.err.println("ERROR: Could not connect with player " + i);
			}
		}

		
		// game starts
		if (activePlayers.size() == 0) {
			System.err.println("ERROR: No players have connected to server");
			shutDown();
		}
		
		for (int i = 0; i < numPlayers; i++) {
			if (activePlayers.contains(i))
				new MazewarServerEventListenerThread(this, i).start();
		}
		
		while (packetQueue.size() < activePlayers.size()) {
			// wait for all players to check in
		}
		
		for (int i = 0; i < numPlayers; i++) {
			MazewarPacket packet = packetQueue.poll();
			playerNames[packet.playerID] = packet.msg;
		}
		sequenceNumber.set(0); // reset sequence number in listener thread
		
		new Thread(new MazewarServerEventBroadcastThread(this)).start();
//		try {
//			Thread.sleep(1000);
//		}
//		catch (InterruptedException e) {}
//		new Thread(new MazewarServerMissileTickerThread(this)).start();

		validInput = false;
		while (!validInput) {
			System.out.print("Type 'quit' to end the game:\n");
			String s = sc.next();
			s = s.trim();
			if (s.equalsIgnoreCase("quit")) {
				validInput = true;
				shutDown();
			}
		}
	}

	
	public void shutDown() {
		isShutDown.set(true);
		sc.close();
		try {
			System.out.println("Shutting down server");
			Thread.sleep(2000); // wait for everything to shut down
			serverSocket.close();
		}
		catch (IOException e) {
			System.err.println("ERROR: Could not close server socket");
		}
		catch (InterruptedException e) {
			
		}
	}
	
	public boolean isShutDown() {
		return isShutDown.get();
	}

	public static void main(String[] args) throws IOException {

		if (args.length != 1) {
			System.err.println("ERROR: Invalid arguments");
			System.exit(-1);
		}
		
		int port = -1;
		try {
			port = Integer.parseInt(args[0]);
		}
		catch (NumberFormatException e) {
			System.err.println("ERROR: Invalid arguments");
			System.exit(-1);
		}
		
		new MazewarServer(port);
	}
}
