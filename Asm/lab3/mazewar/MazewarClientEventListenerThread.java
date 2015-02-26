import java.net.*;
import java.io.*;

public class MazewarClientEventListenerThread extends Thread {
	private Socket socket = null;
	private MazewarClient mazewarClient;

	public MazewarClientEventListenerThread(MazewarClient mazewarClient) {
		super("MazewarClientEventListenerThread");
		this.mazewarClient = mazewarClient;
		this.socket = mazewarClient.socket;
		System.out.println("Created MazewarClientEventListenerThread");
	}

	public void run() {

		ObjectInputStream fromServer = null;
		try {
			fromServer = new ObjectInputStream(socket.getInputStream());
		} 
		catch (IOException e) {
			System.err.println("ERROR: Could not connect to server");
			mazewarClient.shutDown();
		}

		while (!mazewarClient.isShutDown()) {
			try {
				MazewarPacket packetFromServer = (MazewarPacket) fromServer.readObject();
				mazewarClient.listenerQueue.add(packetFromServer);
				System.out.println("Received player " + packetFromServer.playerID + " " + packetFromServer.packetType + " event from server");
			}
			catch (IOException e) {
				System.err.println("ERROR: Could not read packets from server");
				mazewarClient.shutDown();
			}
			catch (ClassNotFoundException e) {

			}
		}
		
		try {
			if (fromServer != null)
				fromServer.close();
			socket.close();
		}
		catch (IOException e) {
			System.err.println("ERROR: Could not close connection to server");
		}
		
		System.out.println("MazewarClientEventListenerThread finished");

	}
}
