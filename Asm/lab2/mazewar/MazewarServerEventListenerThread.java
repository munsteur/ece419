import java.net.*;
import java.io.*;

public class MazewarServerEventListenerThread extends Thread {
	private Socket socket = null;
	private MazewarServer mazewarServer;
	private int playerID;

	public MazewarServerEventListenerThread(MazewarServer mazewarServer, int playerID) {
		super("MazewarServerEventListenerThread");
		this.mazewarServer = mazewarServer;
		this.playerID = playerID;
		this.socket = mazewarServer.clientSockets[playerID];
		System.out.println("Created MazewarServerEventListenerThread for player " + playerID);
	}

	public void run() {

		ObjectInputStream fromClient = null;
		try {
			fromClient = new ObjectInputStream(socket.getInputStream());
		} 
		catch (IOException e) {
			System.err.println("ERROR: Could not connect with player " + playerID);
			mazewarServer.activePlayers.remove(playerID);
		}

		while (!mazewarServer.isShutDown() && mazewarServer.activePlayers.contains(playerID)) {
			try {
				MazewarPacket packetFromClient = (MazewarPacket) fromClient.readObject();
				packetFromClient.playerID = playerID;
				packetFromClient.sequenceNumber = mazewarServer.sequenceNumber.getAndIncrement();
				mazewarServer.packetQueue.add(packetFromClient);
				System.out.println("Received " + packetFromClient.packetType + " packet from player " + playerID);
			}
			catch (IOException e) {
				System.err.println("ERROR: Could not read packets from player " + playerID);
				mazewarServer.activePlayers.remove(playerID);
			}
			catch (ClassNotFoundException e) {

			}
		}
		
		try {
			if (fromClient != null)
				fromClient.close();
			socket.close();
		}
		catch (IOException e) {
			System.err.println("ERROR: Could not close connections to player " + playerID);
		}

	}
}
