/*
Copyright (C) 2004 Geoffrey Alan Washburn

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
USA.
 */

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JOptionPane;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

import javax.swing.BorderFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.Comparator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The entry point and glue code for the game.  It also contains some helpful
 * global utility methods.
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: Mazewar.java 371 2004-02-10 21:55:32Z geoffw $
 */

public class MazewarClient extends JFrame {

	public boolean isShutDown;
	public boolean isTicker;
	public boolean isPaused;

	public int port;
	public String namingServiceHost;
	public int namingServicePort;

	private static final int QUEUE_SIZE = 10000;
	public PriorityBlockingQueue<MazewarGamePacket> gameListenerQueue;
	public ConcurrentHashMap<Integer, PriorityBlockingQueue<MazewarGamePacket>> gameSenderQueues;
	public ArrayBlockingQueue<MazewarInfoPacket> infoListenerQueue;
	public ArrayBlockingQueue<MazewarInfoPacket> infoSenderQueue;
	public ConcurrentHashMap<Double, ConcurrentHashMap<Integer, MazewarGamePacket>> acks; // <lamport, ack count>

	public ConcurrentHashMap<Integer, Player> players; // <player id, player object>
	private ConcurrentHashMap<Integer, Client> clients; // <client id, client object>
	public ConcurrentHashMap<Integer, Boolean> playerShutdown;
	public int playerID;

	AtomicLong lamport; 


	/**
	 * The default width of the {@link Maze}.
	 */
	private final int mazeWidth = 20;

	/**
	 * The default height of the {@link Maze}.
	 */
	private final int mazeHeight = 10;

	/**
	 * The default random seed for the {@link Maze}.
	 * All implementations of the same protocol must use 
	 * the same seed value, or your mazes will be different.
	 */
	private final int mazeSeed = 42;

	/**
	 * The {@link Maze} that the game uses.
	 */
	private MazeImpl maze = null;

	/**
	 * The {@link GUIClient} for the game.
	 */
	public GUIClient guiClient = null;

	/**
	 * The panel that displays the {@link Maze}.
	 */
	private OverheadMazePanel overheadPanel = null;

	private ScoreTableModel scoreModel;

	/**
	 * The table the displays the scores.
	 */
	private JTable scoreTable = null;

	/** 
	 * Create the textpane statically so that we can 
	 * write to it globally using
	 * the static consolePrint methods  
	 */
	private static final JTextPane console = new JTextPane();

	/** 
	 * Write a message to the console followed by a newline.
	 * @param msg The {@link String} to print.
	 */ 
	public static synchronized void consolePrintLn(String msg) {
		console.setText(console.getText()+msg+"\n");
	}

	/** 
	 * Write a message to the console.
	 * @param msg The {@link String} to print.
	 */ 
	public static synchronized void consolePrint(String msg) {
		console.setText(console.getText()+msg);
	}

	/** 
	 * Clear the console. 
	 */
	public static synchronized void clearConsole() {
		console.setText("");
	}

	/**
	 * Method for performing cleanup before exiting the game.
	 */
	public void shutDown() {
		// send remove request to naming service
		MazewarInfoPacket removeRequest = new MazewarInfoPacket(
				MazewarInfoPacketType.REMOVE_REQUEST, 
				playerID, true, "");
		infoSenderQueue.add(removeRequest);
		new MazewarClientInfoHandlerThread(this).start();

		// send broadcast shutdown
		MazewarGamePacket leavePacket = buildPacket(MazewarGamePacketType.LEAVE, -1.0, null);
		broadcastPacket(leavePacket);
		// clean up resources
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {}

		isShutDown = true;
	}

	public MazewarGamePacket buildPacket(MazewarGamePacketType type, double ackLamport, Object extraInfo) {
		return new MazewarGamePacket(type, playerID, lamport.incrementAndGet(), ackLamport, extraInfo);
	}

	public void broadcastPacket(MazewarGamePacket packet) {
		//gameListenerQueue.add(packet);
		queueMsg(packet);

		MazewarGamePacket first = gameListenerQueue.peek();
		System.out.println("Added: " + packet.extendLamport + " Head: " + first.extendLamport);
		for (MazewarGamePacket p : gameListenerQueue) {
			System.out.println(p.extendLamport + " ");
		}
		System.out.println();
		
		for (PriorityBlockingQueue<MazewarGamePacket> queue : gameSenderQueues.values()) {
			queue.add(packet);
		}
	}

	private void addRemoteClient(int id, String[] info) {
		int score = Integer.parseInt(info[0]);
		int x = Integer.parseInt(info[1]);
		int y = Integer.parseInt(info[2]);
		int directionVal = Integer.parseInt(info[3]);
		String name = info[4];

		RemoteClient remoteClient = new RemoteClient(name, score, new DirectedPoint(new Point(x, y), new Direction(directionVal)));
		clients.put(id, (Client) remoteClient);
		maze.addClient(remoteClient);


	}

	public void queueMsg(MazewarGamePacket packet) {
		gameListenerQueue.add(packet);
	}

	/** 
	 * The place where all the pieces are put together. 
	 */
	public MazewarClient(int port, String namingServiceHost, int namingServicePort) {
		super("ECE419 Mazewar");
		consolePrintLn("ECE419 Mazewar started!");



		// Create the maze
		maze = new MazeImpl(new Point(mazeWidth, mazeHeight), mazeSeed);
		assert(maze != null);

		// Have the ScoreTableModel listen to the maze to find
		// out how to adjust scores.
		scoreModel = new ScoreTableModel();
		assert(scoreModel != null);
		maze.addMazeListener(scoreModel);

		// Throw up a dialog to get the GUIClient name.
		String name = JOptionPane.showInputDialog("Enter your name");
		if((name == null) || (name.length() == 0)) {
			System.exit(0);
		}



		// You may want to put your network initialization code somewhere in
		// here.

		// one rx queue for all players
		gameListenerQueue	= new PriorityBlockingQueue<MazewarGamePacket>(100000000);
		// one tx queue for each player
		gameSenderQueues	= new ConcurrentHashMap<Integer, PriorityBlockingQueue<MazewarGamePacket>>();
		infoListenerQueue	= new ArrayBlockingQueue<MazewarInfoPacket>(QUEUE_SIZE);
		infoSenderQueue		= new ArrayBlockingQueue<MazewarInfoPacket>(QUEUE_SIZE);
		acks				= new ConcurrentHashMap<Double, ConcurrentHashMap<Integer, MazewarGamePacket>>();

		isShutDown = false;
		playerShutdown		= new ConcurrentHashMap<Integer, Boolean>();
		isTicker = false;
		isPaused = false;

		this.namingServiceHost = namingServiceHost;
		this.namingServicePort = namingServicePort;
		this.port = port;





		// send join request to naming service
		MazewarInfoPacket joinRequest = new MazewarInfoPacket(
				MazewarInfoPacketType.JOIN_REQUEST, 
				-1, true, name + " " + port);
		infoSenderQueue.add(joinRequest);
		new MazewarClientInfoHandlerThread(this).start();

		// wait for reply
		while (infoListenerQueue.isEmpty()) {
			// wait
		}
		MazewarInfoPacket joinReply = (MazewarInfoPacket) infoListenerQueue.poll();
		if (joinReply == null || !joinReply.ok) {
			System.err.println("ERROR: Unable to join game");
			System.exit(1);
		}

		playerID = joinReply.playerID;
		players = (ConcurrentHashMap<Integer, Player>)joinReply.extraInfo;
		System.out.println("Player " + playerID);

		//		for (Player player : players.values())
		//			System.out.println(player.name + " " + player.host + " " + player.port);

		(new MazewarClientConnectionHandlerThread(this)).start();





		lamport = new AtomicLong(0);
		clients = new ConcurrentHashMap<Integer, Client>();


		// Create the GUIClient and connect it to the KeyListener queue
		guiClient = new GUIClient(name, this);
		maze.addClient(guiClient);
		this.addKeyListener(guiClient);
		clients.put(playerID, (Client) guiClient);



		for (Player player : players.values()) {
			if (playerID != player.id) {
				gameSenderQueues.put(player.id, new PriorityBlockingQueue<MazewarGamePacket>());
			}
		}

		MazewarGamePacket joinPacket = buildPacket(MazewarGamePacketType.JOIN, -1.0, 
				new String[] {
				""+guiClient.getScore(),
				""+guiClient.getPoint().getX(),
				""+guiClient.getPoint().getY(),
				""+guiClient.getOrientation().toVal(),
				guiClient.getName()
		});
		broadcastPacket(joinPacket);

		for (Player player : players.values()) {
			if (playerID != player.id) {
				Socket socket = null;
				try {
					socket = new Socket(player.host, player.port);
					System.out.println("Conntected to player " + player.id);		
				} 
				catch (IOException e) {
					System.err.println("ERROR: Could not connect to player " + player.id);
					System.exit(1);
				}
				
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {}
				
				(new MazewarClientGameListenerThread(this, player.id, socket)).start();
				(new MazewarClientGameSenderThread(this, player.id, socket)).start();
			}
		}



		/*// wait for acks if there is other players
		if (players.size() > 1) {
			boolean ready = false;
			while (!ready) {
				if (!gameListenerQueue.isEmpty()) {
					MazewarGamePacket packet = gameListenerQueue.peek();
					if ( acks.containsKey(packet.extendLamport) && acks.get(packet.extendLamport).size() >= players.size()-1 ) {
						ready = true;
					}
				}
			}
		}*/

		if (players.size() == 1) {
			isTicker = true;
			(new MazewarClientMissileTickerThread(this)).start();
		}










		// process JOIN and start game
		//acks.remove(gameListenerQueue.poll().extendLamport);


		ConcurrentHashMap<Double, Boolean> seen = new ConcurrentHashMap<Double, Boolean>();

		while (!isShutDown) {

			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {}

			if (!gameListenerQueue.isEmpty()) {
				MazewarGamePacket packet = gameListenerQueue.peek();

				// send ACK to all other clients
				double msgLamport = packet.extendLamport;
				System.out.println("Head of queue: " + msgLamport);
				for (MazewarGamePacket p : gameListenerQueue) {
					System.out.println(p.extendLamport + " ");
				}
				System.out.println();

				if (!seen.containsKey(packet.extendLamport)) {
					for (PriorityBlockingQueue<MazewarGamePacket> queue : gameSenderQueues.values()) {
						if (packet.packetType == MazewarGamePacketType.JOIN) {
							queue.add(buildPacket(MazewarGamePacketType.ACK, msgLamport, 
									new String[] {
									""+guiClient.getScore(),
									""+guiClient.getPoint().getX(),
									""+guiClient.getPoint().getY(),
									""+guiClient.getOrientation().toVal(),
									guiClient.getName()
							}
									));
						}
						else {
							queue.add(buildPacket(MazewarGamePacketType.ACK, msgLamport, null));
						}
					}
					seen.put(packet.extendLamport, true);
				}
				
				
				if ( players.size() == 1 ||
						(acks.containsKey(msgLamport) && acks.get(msgLamport).size() >= players.size()-1) ) {

					MazewarGamePacket polledPacket = gameListenerQueue.poll();
					if (polledPacket.lamport != packet.lamport ||
							polledPacket.playerID != packet.playerID) {
						//gameListenerQueue.add(polledPacket);
						queueMsg(polledPacket);
						System.err.println("Poll != Peek alert");
					}
					else {
						int id = packet.playerID;
						//double msgLamport = packet.extendLamport;
						switch (packet.packetType) {
						case JOIN:
							if (playerID == id) {
								for (int otherPlayerID : players.keySet()) {
									if (otherPlayerID != playerID) {
										addRemoteClient(otherPlayerID, (String[])acks.get(msgLamport).get(otherPlayerID).extraInfo);
									}
								}
								initGame();
							}else {
								addRemoteClient(id, (String[])packet.extraInfo);
								isPaused = false;
							}
							break;
						case LEAVE:
							maze.removeClient(clients.get(id));
							playerShutdown.put(id, true);
							players.remove(id);
							gameSenderQueues.remove(id);
							break;
						case FIRE:
							clients.get(id).fire();
							break;
						case GO_BACKWARD:
							clients.get(id).backup();
							break;
						case GO_FORWARD:
							clients.get(id).forward();
							break;
						case TURN_LEFT:
							clients.get(id).turnLeft();
							break;
						case TURN_RIGHT:
							clients.get(id).turnRight();
							break;
						case MISSLE_TICK:
							maze.missileTick();
							break;
						default:
							break;

						}

						
						if (players.size() > 1)
							acks.remove(msgLamport);
						System.out.println("Finished processing player " + id + " " + packet.packetType + " event " + msgLamport);
					}
				}
			}
		}


		System.exit(0);


	}

	public void initGame() {
		// Use braces to force constructors not to be called at the beginning of the
		// constructor.
		{
			//			maze.addClient(new RobotClient("Norby"));
			//			maze.addClient(new RobotClient("Robbie"));
			//			maze.addClient(new RobotClient("Clango"));
			//			maze.addClient(new RobotClient("Marvin"));
		}


		// Create the panel that will display the maze.
		overheadPanel = new OverheadMazePanel(maze, guiClient);
		assert(overheadPanel != null);
		maze.addMazeListener(overheadPanel);

		// Don't allow editing the console from the GUI
		console.setEditable(false);
		console.setFocusable(false);
		console.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));

		// Allow the console to scroll by putting it in a scrollpane
		JScrollPane consoleScrollPane = new JScrollPane(console);
		assert(consoleScrollPane != null);
		consoleScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Console"));

		// Create the score table
		scoreTable = new JTable(scoreModel);
		assert(scoreTable != null);
		scoreTable.setFocusable(false);
		scoreTable.setRowSelectionAllowed(false);

		// Allow the score table to scroll too.
		JScrollPane scoreScrollPane = new JScrollPane(scoreTable);
		assert(scoreScrollPane != null);
		scoreScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Scores"));

		// Create the layout manager
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		getContentPane().setLayout(layout);

		// Define the constraints on the components.
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 3.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		layout.setConstraints(overheadPanel, c);
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.weightx = 2.0;
		c.weighty = 1.0;
		layout.setConstraints(consoleScrollPane, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;
		layout.setConstraints(scoreScrollPane, c);

		// Add the components
		getContentPane().add(overheadPanel);
		getContentPane().add(consoleScrollPane);
		getContentPane().add(scoreScrollPane);

		// Pack everything neatly.
		pack();

		// Let the magic begin.
		setVisible(true);
		overheadPanel.repaint();
		this.requestFocusInWindow();

	}


	/**
	 * Entry point for the game.  
	 * @param args Command-line arguments.
	 */
	public static void main(String args[]) {

		if (args.length != 3) {
			System.err.println("ERROR: Invalid arguments. Usage: local_port naming_service_ip naming_service_port");
			System.exit(-1);
		}

		int port = -1;
		String namingServiceHost = "";
		int namingServicePort = -1;
		try {
			port = Integer.parseInt(args[0]);
			namingServiceHost = args[1];
			namingServicePort = Integer.parseInt(args[2]);
		}
		catch (NumberFormatException e) {
			System.err.println("ERROR: Invalid arguments. Usage: local_port naming_service_ip naming_service_port");
			System.exit(-1);
		}

		new MazewarClient(port, namingServiceHost, namingServicePort);
	}
}
