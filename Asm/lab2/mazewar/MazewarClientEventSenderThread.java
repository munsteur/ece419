import java.net.*;
import java.io.*;

public class MazewarClientEventSenderThread extends Thread {
	private Socket socket = null;
	private MazewarClient mazewarClient;

	public MazewarClientEventSenderThread(MazewarClient mazewarClient) {
		super("MazewarClientEventSenderThread");
		this.mazewarClient = mazewarClient;
		this.socket = mazewarClient.socket;
		System.out.println("Created MazewarClientEventSenderThread");
	}

	public void run() {

		ObjectOutputStream toServer = null;
		try {
			toServer = new ObjectOutputStream(socket.getOutputStream());
		} 
		catch (IOException e) {
			System.err.println("ERROR: Could not connect to server");
			mazewarClient.shutDown();
		}

		while (!mazewarClient.isShutDown()) {
			if (!mazewarClient.senderQueue.isEmpty()) {
				try {
					toServer.writeObject(mazewarClient.senderQueue.poll());
				}
				catch (IOException e) {
					System.err.println("ERROR: Could not send packets to server");
					mazewarClient.shutDown();
				}
			}
		}

		try {
			if (toServer != null)
				toServer.close();
			socket.close();
		}
		catch (IOException e) {
			System.err.println("ERROR: Could not close connection to server");
		}

	}
}
