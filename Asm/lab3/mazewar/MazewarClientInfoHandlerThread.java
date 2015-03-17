import java.net.*;
import java.io.*;

public class MazewarClientInfoHandlerThread extends Thread {
	private MazewarClient mazewarClient;

	public MazewarClientInfoHandlerThread(MazewarClient mazewarClient) {
		super("MazewarClientInfoHandlerThread");
		this.mazewarClient = mazewarClient;
	}

	public void run() {
		System.out.println("Started MazewarClientInfoHandlerThread");
		
		Socket socket = null;
		
		try {
			socket = new Socket(mazewarClient.namingServiceHost, mazewarClient.namingServicePort);
			System.out.println("Conntected to naming service");		
		} 
		catch (IOException e) {
			System.err.println("ERROR: Could not connect to naming service");
			System.exit(1);
		}
		
		// send request
		ObjectOutputStream toServer = null;
		try {
			toServer = new ObjectOutputStream(socket.getOutputStream());
			toServer.writeObject(mazewarClient.infoSenderQueue.poll());
		} 
		catch (IOException e) {
			System.err.println("ERROR: Could not send request to naming service");
		}
		
		ObjectInputStream fromServer = null;
		try {
			fromServer = new ObjectInputStream(socket.getInputStream());
			MazewarInfoPacket packetFromServer = (MazewarInfoPacket) fromServer.readObject();
			// ignore the reply for remove_request for now
			if (packetFromServer.packetType == MazewarInfoPacketType.JOIN_REQUEST)
				mazewarClient.infoListenerQueue.add(packetFromServer);
			System.out.println("Received " + packetFromServer.packetType + " packet from naming service");
		} 
		catch (IOException | ClassNotFoundException e) {
			System.err.println("ERROR: Could not read reply from naming service");	
		}

		try {
			if (toServer != null)
				toServer.close();
			if (fromServer != null)
				fromServer.close();
			socket.close();
		}
		catch (IOException e) {
			System.err.println("ERROR: Could not close connection to naming service");
		}

	}
}
