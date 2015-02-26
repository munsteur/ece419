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
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The entry point and glue code for the game.  It also contains some helpful
 * global utility methods.
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: Mazewar.java 371 2004-02-10 21:55:32Z geoffw $
 */

public class MazewarClient extends JFrame {

	private static final int QUEUE_SIZE = 1000;
	
	private String host;
	private int port; 
	private boolean isShutDown;
	private int playerID;
	private int numPlayers;
	private Client[] clients;


	public Socket socket;
	public PriorityBlockingQueue<MazewarPacket> listenerQueue;
	public PriorityBlockingQueue<MazewarPacket> senderQueue;
	private AtomicInteger sequenceNumber;
	public String[] playerNames;

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
	private GUIClient guiClient = null;

	/**
	 * The panel that displays the {@link Maze}.
	 */
	private OverheadMazePanel overheadPanel = null;

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
		isShutDown = true;
	}

	public boolean isShutDown() {
		return isShutDown;
	}

	/** 
	 * The place where all the pieces are put together. 
	 */
	public MazewarClient(String host, int port) {
		super("ECE419 Mazewar");
		consolePrintLn("ECE419 Mazewar started!");

		// Create the maze
		maze = new MazeImpl(new Point(mazeWidth, mazeHeight), mazeSeed);
		assert(maze != null);

		// Have the ScoreTableModel listen to the maze to find
		// out how to adjust scores.
		ScoreTableModel scoreModel = new ScoreTableModel();
		assert(scoreModel != null);
		maze.addMazeListener(scoreModel);

		// Throw up a dialog to get the GUIClient name.
		String name = JOptionPane.showInputDialog("Enter your name");
		if((name == null) || (name.length() == 0)) {
			System.exit(0);
		}


		// You may want to put your network initialization code somewhere in
		// here.
		listenerQueue = new PriorityBlockingQueue<MazewarPacket>(QUEUE_SIZE);
		senderQueue = new PriorityBlockingQueue<MazewarPacket>(QUEUE_SIZE);
		isShutDown = false;

		this.host = host;
		this.port = port;
		try {
			socket = new Socket(host, port);
			System.out.println("Waiting for other players to join");		
		} 
		catch (IOException e) {
			System.err.println("ERROR: Could not connect to the server");
			System.exit(1);
		}


		new MazewarClientEventListenerThread(this).start();
		new MazewarClientEventSenderThread(this).start();

		// send player name
		MazewarPacket firstClientPacket = new MazewarPacket(
				MazewarPacketType.PLAYER_NAME, 
				-1,
				-1,
				name);
		senderQueue.add(firstClientPacket);	

		// wait for first packet from server
		// with all players' information
		sequenceNumber = new AtomicInteger(0);
		MazewarPacket firstServerPacket = null;
		while (listenerQueue.isEmpty()) {
			// wait
		}
		firstServerPacket = listenerQueue.poll();
		if (firstServerPacket.packetType != MazewarPacketType.GAME_START || 
				firstServerPacket.sequenceNumber != sequenceNumber.getAndIncrement()) {
			System.err.println("ERROR: Communication error with server");
			System.exit(1);
		}

		//		System.out.println(packet.playerID + " " + packet.packetType + " " + packet.msg);

		playerID = firstServerPacket.playerID;
		// words[0] = numPlayers
		// words[i, i+1] = player id, player name 
		String[] words = firstServerPacket.msg.split(" ");
		numPlayers = Integer.parseInt(words[0]);
		System.out.println("Player " + playerID + " of " + numPlayers + " players");


		playerNames = new String[numPlayers];
		for (int i = 1; i < words.length; i += 2)
			playerNames[Integer.parseInt(words[i])] = words[i+1];
		//		for (int i = 0; i < playerNames.length; i++)
		//			System.out.println(playerNames[i]);



		clients = new Client[numPlayers];

		for (int i = 0; i < numPlayers; i++) {
			if (i == playerID) {
				// Create the GUIClient and connect it to the KeyListener queue
				guiClient = new GUIClient(name, this);
				maze.addClient(guiClient);
				this.addKeyListener(guiClient);
				clients[i] = (Client) guiClient;
			}
			else {
				RemoteClient remoteClient = new RemoteClient(playerNames[i]);
				maze.addClient(remoteClient);
				clients[i] = (Client) remoteClient;
			}
		}




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




		while (!isShutDown) {
			if (!listenerQueue.isEmpty()) {
				MazewarPacket packet = listenerQueue.peek();
				if (packet.sequenceNumber == sequenceNumber.get()) {
					listenerQueue.poll();
					if (packet.packetType == MazewarPacketType.MISSLE_TICK)
						new Thread(maze).start();
					else if (packet.packetType == MazewarPacketType.QUIT)
						shutDown();
					else {
						Client player = clients[packet.playerID];
						switch (packet.packetType) {
						case FIRE:
							player.fire();
							break;
						case GO_BACKWARD:
							player.backup();
							break;
						case GO_FORWARD:
							player.forward();
							break;
						case TURN_LEFT:
							player.turnLeft();
							break;
						case TURN_RIGHT:
							player.turnRight();
							break;
						default:
							break;
						}
					}
					sequenceNumber.incrementAndGet();
				}
			}
		}
		
		System.exit(0);


	}


	/**
	 * Entry point for the game.  
	 * @param args Command-line arguments.
	 */
	public static void main(String args[]) {

		if (args.length != 2) {
			System.err.println("ERROR: Invalid arguments");
			System.exit(-1);
		}

		String host = "";
		int port = -1;
		try {
			host = args[0];
			port = Integer.parseInt(args[1]);
		}
		catch (NumberFormatException e) {
			System.err.println("ERROR: Invalid arguments");
			System.exit(-1);
		}

		new MazewarClient(host, port);
	}
}
