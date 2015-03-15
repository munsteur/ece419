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
		System.out.println("Starting MazewarNamingServiceHandlerThread");
	}

	public void run() {
		// read request
		ObjectInputStream fromClient = null;
		MazewarAdminPacket rxPacket = null;
		try {
			fromClient = new ObjectInputStream(clientSocket.getInputStream());
			rxPacket = (MazewarAdminPacket) fromClient.readObject();
		}
		catch (IOException | ClassNotFoundException e) {
			System.err.println("ERROR: Could not read request from player");
			return;
		}

		// process request
		MazewarAdminPacket txPacket = null;
		if (rxPacket != null) {
			switch (rxPacket.packetType) {
			case JOIN_REQUEST:
				txPacket = new MazewarAdminPacket(MazewarAdminPacketType.JOIN_REQUEST, -1, false, "");
				if (namingService.playersLookup.size() < namingService.maxPlayers) {
					int id = namingService.availableIDs.poll();
					namingService.playersLookup.put( 
							clientSocket.getRemoteSocketAddress().toString() + ":" + clientSocket.getPort(),
							id);
					// send back OK status
					txPacket.ok = true;
					// send back assigned player ID
					txPacket.playerID = id;
					// send player list
					for (String addr : namingService.playersLookup.keySet())
						txPacket.msg += addr + " " + namingService.playersLookup.get(addr); 
				}
				break;
			case REMOVE_REQUEST:
				String addr = rxPacket.msg; 
				txPacket = new MazewarAdminPacket(MazewarAdminPacketType.REMOVE_REQUEST, -1, false, "");
				if (namingService.playersLookup.size() > 0) {
					if (namingService.playersLookup.containsKey(addr)) {
						namingService.playersLookup.remove(addr);
						namingService.availableIDs.add(rxPacket.playerID);
						
						txPacket.playerID = rxPacket.playerID;
						txPacket.ok = true;
					}
				}
				break;
			default:
				break;
			}
		}

		// reply to request
		if (txPacket != null) {
			ObjectOutputStream toClient = null;
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
			if (clientSocket != null)
				clientSocket.close();
		}
		catch (IOException e) {
			System.err.println("ERROR: Could not close connection to player");
		}
	}
}
