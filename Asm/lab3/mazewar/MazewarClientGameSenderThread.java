import java.net.*;
import java.io.*;

public class MazewarClientGameSenderThread extends Thread {
	private Socket socket = null;
	private MazewarClient mazewarClient;
	private int playerID;

	public MazewarClientGameSenderThread(MazewarClient mazewarClient, int playerID, Socket socket) {
		super("MazewarClientGameSenderThread");
		this.mazewarClient = mazewarClient;
		this.playerID = playerID;
		this.socket = socket;
		
	}

	public void run() {
		System.out.println("Started MazewarClientGameSenderThread");

		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(socket.getOutputStream());
		} 
		catch (IOException e) {
			System.err.println("ERROR: Could not connect to player " + playerID);
		}

		while (!mazewarClient.playerShutdown.containsKey(playerID)) {
			if (!mazewarClient.gameSenderQueues.get(playerID).isEmpty()) {
				try {
					oos.writeObject(mazewarClient.gameSenderQueues.get(playerID).poll());
				}
				catch (IOException e) {
					System.err.println("ERROR: Could not send packets to player " + playerID);
				}
			}
		}

		try {
			if (oos != null)
				oos.close();
			socket.close();
		}
		catch (IOException e) {
			System.err.println("ERROR: Could not close connection to player " + playerID);
		}
		
		System.out.println("Exitted MazewarClientGameSenderThread");

	}
}
