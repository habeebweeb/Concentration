import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 *	Memory Game client 0
 */
public class Player extends JFrame implements GameConstants, Runnable{
	private static final String SERVER = "-server";
	private static final String IMG = "-img";
	private static final String HELP = "-help";

	private Socket socket;
	private DataOutputStream toServer;
	private DataInputStream fromServer;

	private JLabel gameStatusLabel;
	private JLabel pairsLabel;
	private JLabel messageLabel;

	private JButton quitButton;
	//private JButton restartButton;

	private int gameWidth;
	private int gameHeight;

	private Card[] cards;

	private int player = 0;
	private boolean myTurn = false;
	private boolean win = false;
	private boolean gameOver = false;
	private int picks = 0;
	private int points = 0;

	private String imagePath;

	/**
	 * Create player
	 * Optional arguments can be add in the form of '-arg (value)'
	 * <ul>
	 * 	<li>{@code -server host: The server address}</li>
	 *  <li>{@code -img dir: image directory.}</li>
	 *  <li>{@code -help: show program usage}</li>
	 * </ul>
	 * @param args	command line arguments
	 */
	public static void main(String[] args){
		String serverHost = "";
		String imagePath = "";

		//true from the start to ensure a -server argument is passed
		boolean help = true;

		if(args.length > 0){
			int i = 0;
			while(i < args.length){
				if(args[i].equals(SERVER)){
					i++;
					if(i == args.length || args[i].equals(HELP) || args[i].equals(IMG))
						break;
					else{
						serverHost = args[i];
						log("Server host address: %s", serverHost);
						help = false;
					}

				}
				else if(args[i].equals(IMG)){
					i++;
					if(i == args.length || args[i].equals(HELP) || args[i].equals(SERVER))
						break;
					else {
						imagePath = imagePath + args[i];
						log("Image directory: %s", imagePath);
						help = false;
					}
				}
				else if(args[i].equals(HELP))
					break;

				i++;
			}
		}

		if(help)
			logHelp();
		else
			new Player(serverHost, imagePath);
	}

	/**
	 *	construct the game client
	 *	@param serverHost the server to interact with
	 *	@param imageDirectory define images directory and use images if defined
	 */
	public Player(String serverHost, String imagePath){
		this.imagePath = imagePath;

		openConnection(serverHost);
		createUI();

		if(socket != null && !socket.isClosed()){
			(new Thread(this)).start();
			log("Waiting for other players");
		} else {
			log("COULD NOT CONNECT");
		}

	}

	/**
	 * open connection with host
	 * @param serverHost the host to connect to
	 */
	private void openConnection(String serverHost){
		try {
			this.socket = new Socket(serverHost, PORT);
			this.fromServer = new DataInputStream(socket.getInputStream());
			this.toServer = new DataOutputStream(socket.getOutputStream());
		} catch(SecurityException e){
			log(e.getMessage());
		} catch(UnknownHostException e){
			log(e.getMessage());
		} catch(IOException e){
			log(e.getMessage());
		}
	}

	/**
	 * close socket
	 */
	private void closeConnection(){
		try {
			if(socket != null && !socket.isClosed()){
				socket.close();
			}
		} catch (IOException e){
			log(e.getCause().getMessage());
		}
		socket = null;
		updateGUI("CONNECTION CLOSED");
		//Another client may have disconnected so we need to make sure no more inputs can be detected
		gameOver = true;
		log("CONNECTION CLOSED");
	}

	public void run(){
		boolean done = false;

		try {
			while(!done){
				int msg = fromServer.readInt();
				switch(msg){
				case INIT:
					this.player = fromServer.readInt();
					gameWidth = fromServer.readInt();
					gameHeight = fromServer.readInt();
					cards = new Card[gameWidth * gameHeight];
					createPanel();
					log("%s: Game initialized", commandString(msg));
					log("YOU ARE PLAYER %d", (int) player + 1);
					break;
				case SETTURN:
					int turn = fromServer.readInt();
					log("%s: It's player %d's turn", commandString(SETTURN), (int) turn + 1);
					setTurn(turn);
					break;
				case REVEAL:
					int cardToReveal = fromServer.readInt();
					int cardValue = fromServer.readInt();
					revealCard(cardToReveal, cardValue);
					log("%s: Card %d revealed", commandString(msg), cardToReveal, cardValue);
					break;
				case MATCH:
					int pointsToAdd = fromServer.readInt();
					addPoints(pointsToAdd);
					log("%s: Match found! %d points added", commandString(msg), pointsToAdd);
					break;
				case WAIT:
					int timeToSleep = fromServer.readInt();
					log("%s: Waiting for %dms", commandString(msg), timeToSleep);
					sleep(timeToSleep);
					break;
				case HIDE:
					int cardToHide1 = fromServer.readInt();
					int cardToHide2 = fromServer.readInt();
					log("%s: Hiding cards %d and %d", commandString(msg), cardToHide1, cardToHide2);
					hideCards(cardToHide1, cardToHide2);
					break;
				case WIN:
					int winner = fromServer.readInt();
					log("%s: Player %d won", commandString(msg), (int) winner + 1);
					gameOver(winner);
					break;
				case DONE:
					log("%s: done", commandString(msg));
					done = true;
					break;
				}
			}
		} catch(IOException e){
	         //do nothing
		} finally {
			closeConnection();
		}

	}

	/**
	 *	give a player a turn
	 *	@param player the player that gets a turn
	 */
	public void setTurn(int player){
		if(this.player == player){
			myTurn = true;
			picks = 0;
		} else {
			myTurn = false;
		}
		updateGUI();
	}

	/**
	 *	send the choice of card to the server
	 *	@param index the card index to send to the server
	 */
	public void pickCard(int index){
		if(gameOver)
			return;
		if(myTurn){
			if(picks < 2){
				picks++;
				updateGUI();
				log("%s: Sending card %d to server", commandString(RECEIVE), index);
				try {
					toServer.writeInt(RECEIVE);
					toServer.writeInt(index);
					toServer.flush();
				}
				catch(IOException e){
					log(e.getMessage());
				}
			} else {
				updateGUI("You can't pick anymore cards");
			}
		} else {
			updateGUI("It's not your turn");
		}
	}

	/**
	 *	reveal the card
	 *	@param card	the index of the card to reveal
	 *	@param cardValue the value of the card revealed
	 */
	public void revealCard(int card, int cardValue){
		cards[card].revealCard(cardValue);
	}

	/**
	 *	hide the card
	 *	@param card	the card to hide
	 */
	public void hideCards(int card1, int card2){
		cards[card1].hideCard();
		cards[card2].hideCard();
	}

	/**
	 *	update the player points
	 *	@param points the player has now
	 */
	public void addPoints(int points){
		this.points = points;
		updateGUI();
	}

	/**
	 *	ends the game and tells the player whether or not they won
	 *	@param player	the player that won
	 */
	public void gameOver(int player){
		log("GAME OVER");
		win = this.player == player;
		gameOver = true;
		myTurn = false;
		updateGUI();
	}

	/**
	 *	ends the game and forces the player that quit to lose
	 */
	public void quitGameAction(){
		if(gameOver)
			return;
		else if(myTurn){
			try{
				toServer.writeInt(QUIT);
				toServer.writeInt(player);
			} catch(IOException e){
				log(e.getMessage());
			}
		}
		else
			updateGUI("It's not your turn");
	}

	/**
	 *	update the gui
	 *	@param msg a message to show the client
	 */
	private void updateGUI(String msg){
		pairsLabel.setText(this.points + " points");
		gameStatusLabel.setForeground(Color.BLACK);
		if(myTurn)
			gameStatusLabel.setText("Your Turn");
		else
			gameStatusLabel.setText("Wait");
		if(gameOver)
			if(win){
				gameStatusLabel.setText("YOU WON!");
				gameStatusLabel.setForeground(Color.GREEN);
			} else {
				gameStatusLabel.setText("You lost");
				gameStatusLabel.setForeground(Color.BLUE);
			}

		messageLabel.setText(msg);
	}
	/**
	 *	update the gui
	 */
	private void updateGUI(){
		updateGUI("");
	}

	/**
	 * sleep for the specified amount of milliseconds
	 * @param ms	milliseconds to sleep
	 */
	public void sleep(int ms){
		try{
			Thread.sleep(ms);
		} catch(InterruptedException e){
			e.printStackTrace();
		}
	}

	/**
	 * output help to the console
	 */
	public static void logHelp(){
		System.out.println("A memory game client that can connect to the internet and play with others\n");
		System.out.println("java Player -server <server address> [-help] [-img <image directory>]\n");
		System.out.println("\t-server\t\tThe server address to connect to");
		System.out.println("\t-help\t\tShows this help information");
		System.out.println("\t-img\t\tSpecifies a directory to use to find images for the cards in the game");
		System.out.println("\t\t\tThere are a total of 20 images in the jpg format; 1 back image and 19 front images");
		System.out.println("\t\t\tThe back image must be a in the form 'back.jpg'");
		System.out.println("\t\t\tThe front images must be in the form 'n.jpg' where n is a number from 0 - 18");
	}

	/**
	 *	creates the game GUI
	 */
	private void createPanel(){
		JPanel statusPanel = new JPanel(new GridLayout(0,3));
		statusPanel.add(gameStatusLabel);
		statusPanel.add(new JLabel(" "));
		statusPanel.add(pairsLabel);

		JPanel cardPanel = new JPanel(new GridLayout(gameHeight, gameWidth));

		boolean hasImages = hasImages();
		if(hasImages)
			log("All images found");
		else
			log("Images will not be used");

		for(int i = 0; i < gameWidth * gameHeight; i++){
			cards[i] = new Card(i, imagePath, hasImages);
			cards[i].addActionListener(new clickListener(cards[i], this));
			cardPanel.add(cards[i]);
		}

		JPanel controlPanel = new JPanel(new GridLayout(0,3));
		controlPanel.add(quitButton);
		controlPanel.add(messageLabel);
		//controlPanel.add(restartButton);

		JPanel gamePanel = new JPanel(new BorderLayout());
		gamePanel.add(BorderLayout.NORTH, statusPanel);
		gamePanel.add(cardPanel);
		gamePanel.add(BorderLayout.SOUTH, controlPanel);

		add(gamePanel);

		setSize(550, 500);
		setTitle("Memory Game - Player " + ( (int) player + 1));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}

	/**
	 * @return true if the required images exist in the directory specified otherwise false
	 */
	private boolean hasImages(){
		boolean hasBackImage = Files.exists(Paths.get(imagePath + "/back.jpg"));
		for(int i = 0; i < Card.NUM_OF_FRONT_IMAGES; i++){
			if(!Files.exists(Paths.get(imagePath + "/" + i + ".jpg")))
				return false;
		}
		return hasBackImage;
	}

	/**
	 *	initializes components of the gui including the cards
	 */
	private void createUI(){
		gameStatusLabel = new JLabel("Wait");
		pairsLabel = new JLabel("0 pairs", JLabel.RIGHT);
		messageLabel = new JLabel("", JLabel.CENTER);
		messageLabel.setForeground(Color.RED);

		quitButton = new JButton("Quit Game");
		quitButton.addActionListener(e -> quitGameAction());
	}

	/**
	 * output messages to command line
	 * @param msg the message to output
	 * @param args items to replace tokens with in msg
	 */
	public static void log(String msg, Object... args){
		System.out.printf(msg + "%n", args);
	}

	/**
	 *	The listener for the cards
	 */
	private class clickListener implements ActionListener {
		private Card card;
		private Player game;

		/**
		 *	gives the button a reference to its card and the game client
		 */
		public clickListener(Card card, Player game){
			this.card = card;
			this.game = game;
		}

		public void actionPerformed(ActionEvent e){
			game.pickCard(card.getIndex());
		}
	}
}
