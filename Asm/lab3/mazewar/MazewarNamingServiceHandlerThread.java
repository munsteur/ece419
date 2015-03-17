import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


public class MazewarNamingServiceHandlerThread extends Thread {

	MazewarNamingService namingService;
	Socket clientSocket;

	public MazewarNamingServiceHandlerThread(MazewarNamingService namingService, Socket clientSocket) {
		super("MazewarNamingServiceHandlerThread");
		this.namingService = namingService;
		this.clientSocket = clientSocket;
	}

	public void run() {
		System.out.println("Started MazewarNamingServiceHandlerThread");
		
		// read request
		ObjectInputStream fromClient = null;
		MazewarInfoPacket rxPacket = null;
		try {
			fromClient = new ObjectInputStream(clientSocket.getInputStream());
			rxPacket = (MazewarInfoPacket) fromClient.readObject();
		}
		catch (IOException | ClassNotFoundException e) {
			System.err.println("ERROR: Could not read request from player");
		}

		// process request
		MazewarInfoPacket txPacket = null;
		if (rxPacket != null) {
			switch (rxPacket.packetType) {
			case JOIN_REQUEST:
				txPacket = new MazewarInfoPacket(MazewarInfoPacketType.JOIN_REQUEST, -1, false, "");
				if (namingService.playersLookup.size() < namingService.maxPlayers) {
					int id = namingService.availableIDs.poll();
					namingService.playersLookup.put(
							id,
							new Player(
									clientSocket.getInetAddress().getHostAddress(),
									Integer.parseInt(((String)rxPacket.extraInfo).split(" ")[1]),
									//clientSocket.getPort(),
									id,
									((String)rxPacket.extraInfo).split(" ")[0] // player name
									)
							);
					// send back OK status
					txPacket.ok = true;
					// send back assigned player ID
					txPacket.playerID = id;
					// send player list
					txPacket.extraInfo = namingService.playersLookup;
				
				}
				break;
			case REMOVE_REQUEST:
				int id = rxPacket.playerID; 
				txPacket = new MazewarInfoPacket(MazewarInfoPacketType.REMOVE_REQUEST, -1, false, "");
				if (namingService.playersLookup.size() > 0) {
					if (namingService.playersLookup.containsKey(id)) {
						namingService.availableIDs.add(id);
						namingService.playersLookup.remove(id);
						txPacket.playerID = id;
						txPacket.ok = true;
					}
				}
				break;
			default:
				break;
			}
		}

		// reply to request
		ObjectOutputStream toClient = null;
		if (txPacket != null) {
			try {
				toClient = new ObjectOutputStream(clientSocket.getOutputStream());
				toClient.writeObject(txPacket);
			} 
			catch (IOException e) {
				System.err.println("ERROR: Could not send reply to player");
			}
		}

		// clean up
		try {
			if (fromClient != null)
				fromClient.close();
			if (toClient != null)
				toClient.close();
			if (clientSocket != null)
				clientSocket.close();
		}
		catch (IOException e) {
			System.err.println("ERROR: Could not close connection to player");
		}
	}
}
