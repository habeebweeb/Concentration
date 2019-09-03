import java.awt.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.text.DefaultCaret;

import javax.swing.*;

public class GameServer extends JFrame implements GameConstants {
	JTextArea logArea;
	private static final String CLIENTS = "-clients";
	private static final String WIDTH = "-width";
	private static final String HEIGHT = "-height";
	private static final String HELP = "-help";

	public static final int MIN_CLIENTS = 2;
	public static final int MIN_GAME_SIZE = 2;
	public static final int MAX_GAME_SIZE = 6;
	private int gameNumber = 0;
	private int numOfClients;

	Socket[] clients;
	/**
	 * Create server and pass command line arguments
	 * Optional arguments can be add in the form of '-arg num'
	 * <ul>
	 * 	<li>{@code -clients n: number of clients}</li>
	 *  <li>{@code -width n: width of the game board}</li>
	 *  <li>{@code -height n: height of the game board}</li>
	 *  <li>{@code -help: show help information}</li>
	 * </ul>
	 * @param args	command line arguments
	 */
	public static void main(String[] args){
		int numOfClients = MIN_CLIENTS;
		int gameWidth = MIN_GAME_SIZE;
		int gameHeight = MIN_GAME_SIZE;

		boolean help = false;

		if(args.length > 0){
			help = true;
			int i = 0;
			try {
				while(i < args.length){
					if(args[i].equals(CLIENTS)){
						i++;
						if(i == args.length || args[i].equals(WIDTH) || args[i].equals(HEIGHT) || args[i].equals(HELP))
							break;
						else {
							numOfClients = Integer.parseInt(args[i]);
							numOfClients = numOfClients < MIN_CLIENTS ? MIN_CLIENTS : numOfClients;
							help = false;
						}

					}
					else if(args[i].equals(WIDTH)){
						i++;
						if(i == args.length || args[i].equals(CLIENTS) || args[i].equals(HEIGHT) || args[i].equals(HELP))
							break;
						else {
							gameWidth = Integer.parseInt(args[i]);
							gameWidth = gameWidth < MIN_GAME_SIZE ? MIN_GAME_SIZE : gameWidth > MAX_GAME_SIZE ? MAX_GAME_SIZE : gameWidth;
							help = false;
						}
					}
					else if(args[i].equals(HEIGHT)){
						i++;
						if(i == args.length || args[i].equals(CLIENTS) || args[i].equals(WIDTH) || args[i].equals(HELP))
							break;
						else {
							gameHeight = Integer.parseInt(args[i]);
							gameHeight = gameHeight < MIN_GAME_SIZE ? MIN_GAME_SIZE : gameHeight > MAX_GAME_SIZE ? MAX_GAME_SIZE : gameHeight;
							help = false;
						}
					}
					else if(args[i].equals(HELP))
						break;
					i++;
				}
			} catch(NumberFormatException e){}
		}

		if(help)
			logHelp();
		else
			new GameServer(numOfClients, gameWidth, gameHeight);
	}

	/**
	 * Starts a server that constantly listens for clients.
	 * @param numOfClients	the number of clients per game
	 * @param gameWidth		the width of the game board
	 * @param gameHeight	the height of the game board
	 */
	public GameServer(int numOfClients, int gameWidth, int gameHeight){
		buildGUI();
		this.numOfClients = numOfClients;

		clients = new Socket[numOfClients];
		log("%nLISTENING FOR CLIENTS%n");

		try(ServerSocket serverSocket = new ServerSocket(PORT)){
			serverStats();
			while(true){
				for(int i = 0; i < this.numOfClients; i++){
					clients[i] = serverSocket.accept();
					clientStats(clients[i]);
					numOfClients--;
				}
				log("GAME %d: STARTING GAME%n", (int) gameNumber + 1);

				log("%nLISTENING FOR CLIENTS%n");

				Runnable service = new GameService(clients, gameNumber, logArea, gameWidth, gameHeight);
				(new Thread(service)).start();

				numOfClients = this.numOfClients;
				gameNumber++;

				clients = new Socket[numOfClients];
			}
		} catch(UnknownHostException e){
			log(e.getMessage());
		} catch(IOException e){
			log(e.getMessage());
		}
	}

	/**
	 * Outputs server statistics and information
	 */
	private void serverStats() throws UnknownHostException{
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date now = new Date();
		log("HOSTNAME: %s", InetAddress.getLocalHost().getHostName());
		log("IP: %s", InetAddress.getLocalHost().getHostAddress());
		log("Server started on %s", sdfDate.format(now));
	}

	/**
	 * Outputs information about the client
	 * @param socket the client's socket
	 */
	private void clientStats(Socket socket){
		InetAddress adr = socket.getInetAddress();
		log("%s - %s connected%n", adr.getHostName(), adr.getHostAddress());
	}

	/**
	 * build gui
	 */
	private void buildGUI(){
		logArea = new JTextArea();
		logArea.setEditable(false);
	    DefaultCaret caret = (DefaultCaret)logArea.getCaret();
	    caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		add(new JScrollPane(logArea), BorderLayout.CENTER);
		setSize(600, 300);
		setTitle("Memory Game Server");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}

	/**
	 * output help to the console
	 */
	public static void logHelp(){
		System.out.println("A memory game server that handles requests from memory game clients over the internet\n");
		System.out.println("java GameServer [" + CLIENTS + " <number of clients>] [" + WIDTH + " <width>] [" + HEIGHT + " <height>] [" + HELP + "]\n");
		System.out.println("\t" + CLIENTS + "\tThe number of clients per game");
		System.out.println("\t" + WIDTH + "\t\tThe width of the game board");
		System.out.println("\t" + HEIGHT + "\t\tThe height of the game board");
		System.out.println("\t" + HELP + "\t\tShows this help information");
		System.out.println("\nthere are a minimum of 2 clients per game");
		System.out.println("width and height are restricted to values of 2 - 6");
		System.out.println("\tIf w * h ends up being odd, both w and h will default to 2");
	}

	/**
	 * output log messages to the server log area
	 * @param msg	the message to output
	 * @param vals	values used in place of tokens specified in msg
	 */
	private void log(String msg, Object... vals){
		logArea.append(String.format(msg + "%n", vals));
	    logArea.setCaretPosition(logArea.getDocument().getLength());
	}
}
