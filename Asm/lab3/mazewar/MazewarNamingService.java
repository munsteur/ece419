import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;



public class MazewarNamingService {
	
	// <id, player>	
	public ConcurrentHashMap<Integer, Player> playersLookup;
	// id's available to be assigned
//	public ConcurrentLinkedQueue<Integer> availableIDs;
	
	public final int maxPlayers;
	public AtomicBoolean isShutDown;
	private ServerSocket serverSocket;
	public AtomicInteger id;
	
	public MazewarNamingService(int port, int maxPlayers) {
		playersLookup = new ConcurrentHashMap<Integer, Player>();
//		availableIDs = new ConcurrentLinkedQueue<>();
//		for (int i = 0; i < maxPlayers; i++)
//			availableIDs.add(i);
		this.maxPlayers = maxPlayers;
		isShutDown = new AtomicBoolean(false);
		id = new AtomicInteger(0);
		
		serverSocket = null;
		try {
			serverSocket = new ServerSocket(port);
			System.out.println("Started naming service");
		} 
		catch (IOException e) {
			System.err.println("ERROR: Could not open server socket on port " + port);
			System.exit(-1);
		}
		
		(new MazewarNamingServiceControlThread(this)).start();

		
		while (!isShutDown.get()) {
			Socket clientSocket = null;
			try {
				clientSocket = serverSocket.accept();
				(new MazewarNamingServiceHandlerThread(this, clientSocket)).start();
			}
			catch (SocketException e1) {
				// socket is closed while accepting
			}
			catch (IOException e2) {
				System.err.println("ERROR: Could not accept incoming connection");
				continue;
			}
		}
	}
	
	public void shutDown() {
		isShutDown.set(true);
		try {
			serverSocket.close();
			System.out.println("Shut down naming service");
		} catch (IOException e) {
			System.err.println("ERROR: Could not close server socket");
		}
	}
	
	
	public static void main(String[] args) throws IOException {

		if (args.length != 2) {
			System.err.println("ERROR: Invalid arguments");
			System.exit(-1);
		}
		
		int port = -1;
		int maxPlayers = -1;
		try {
			port = Integer.parseInt(args[0]);
			maxPlayers = Integer.parseInt(args[1]);
		}
		catch (NumberFormatException e) {
			System.err.println("ERROR: Invalid arguments");
			System.exit(-1);
		}
		
		new MazewarNamingService(port, maxPlayers);
	}
	
}
