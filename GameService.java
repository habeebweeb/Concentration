import java.io.*;
import java.net.*;

import javax.swing.JTextArea;

public class GameService implements Runnable, GameConstants{
	/**
	 * Default amount of ms to sleep for
	 */
	public static final int DEFAULT_SLEEP_TIME = 2000;

	/**
	 * The amount of points to give for a matching pair
	 */
	public static final int POINTS_TO_GIVE = 1;

	/**
	 * The minimum size for the game board
	 */
	public static final int MIN_GAME_SIZE = 2;

	private Socket[] socket;
	private DataInputStream[] fromClient;
	private DataOutputStream[] toClient;
	private JTextArea logArea;

	private int gameNumber;
	private int gameWidth;
	private int gameHeight;

	private int currentPlayer = 0;
	private int[] cards;
	private int currentPick = 0;
	private int[] chosenCards = new int[2];
	private int pairsLeft;

	private int[] points;
	private boolean done = false;

	/**
	 * Construct a game service for the clients
	 * @param clients		The clients to handle
	 * @param gameNumber	The number of this game
	 * @param tA			The servers text area
	 * @param gameWidth		The width of the game
	 * @param gameHeight	The height of the game
	 */
	public GameService(Socket[] clients,int gameNumber, JTextArea tA, int gameWidth, int gameHeight){
		socket = clients;
		this.gameNumber = gameNumber;
		logArea = tA;
		fromClient = new DataInputStream[socket.length];
		toClient = new DataOutputStream[socket.length];
		this.gameWidth = gameWidth;
		this.gameHeight = gameHeight;


		boolean winnableGame = (gameWidth * gameHeight) % 2 == 0;
		if(!winnableGame){
			this.gameWidth = MIN_GAME_SIZE;
			this.gameHeight = MIN_GAME_SIZE;
		}

		points = new int[socket.length];
		shuffleCards();
		initializeClients();
	}

	public void run(){
		int i = 0;
		try {
			try {
				while(!done){
					fromClient[i] = new DataInputStream(socket[i].getInputStream());
					processCommands(i);
					i = ++i % socket.length;
				}
			} finally {
				for(i = 0; i < socket.length; i++){
					socket[i].close();
					socket[i] = null;
				}
			}
		} catch(IOException e){
			log(e.getMessage());
		} finally {
			log("ENDING GAME%n");
		}
	}

	/**
	 * listen for commands from the client
	 * @param client	the current client to listen from
	 * @throws IOException
	 */
	private void processCommands(int client) throws IOException {
		int cmd;
		log("waiting for commands from player %d", (int) client + 1);
		boolean done = false;
		do {
			cmd = fromClient[client].readInt();
			switch(cmd){
			case RECEIVE:
				int chosenCard = fromClient[currentPlayer].readInt();
				log("%s: received %d from current player", commandString(cmd), chosenCard);
				revealCard(chosenCard);
				if(currentPick == 0)
					done = true;
				break;
			case QUIT:
				int player = fromClient[client].readInt();
				log("%s: player %d quit the game", commandString(cmd), (int) player + 1);
				determineWinner(player);
				done = true;
				break;
			}
		} while(!done);
	}

	/**
	 * Initialize the clients
	 */
	private void initializeClients(){
		log("%s: Initializing clients. gameWidth: %d, gameHeight: %d", commandString(INIT), gameWidth, gameHeight);
		try{
			for(int i = 0; i < socket.length; i++){
				toClient[i] = new DataOutputStream(socket[i].getOutputStream());
				toClient[i].writeInt(INIT);
				toClient[i].writeInt(i);
				toClient[i].writeInt(gameWidth);
				toClient[i].writeInt(gameHeight);
				toClient[i].flush();
			}
			setTurn(true);
		} catch(IOException e){
			log(e.getMessage());
		}
	}

	/**
	 * Gives a player a turn
	 * @param initial	ensures the first turn is given to player 0
	 * @throws IOException
	 */
	private void setTurn(boolean initial) throws IOException{
		if(!initial)
			currentPlayer = ++currentPlayer % socket.length;

		log("%s: Giving player %d a turn", commandString(SETTURN), (int) currentPlayer + 1);

		for(int i = 0; i < socket.length; i++){
			toClient[i] = new DataOutputStream(socket[i].getOutputStream());
			toClient[i].writeInt(SETTURN);
			toClient[i].writeInt(currentPlayer);
			toClient[i].flush();
		}
	}

	/**
	 * Reveals the card the client picked.
	 * If the client has picked 2 cards, the server will determine if there is a match.
	 * If the last 2 cards are uncovered, the server will determine a winner
	 * @param cardToReveal	the card to reveal
	 * @throws IOException
	 */
	private void revealCard(int cardToReveal) throws IOException{
		log("%s: Revealing card %d with a value of %d", commandString(REVEAL), cardToReveal, cards[cardToReveal]);
		chosenCards[currentPick] = cardToReveal;
		for(int i = 0; i < socket.length; i++){
			toClient[i] = new DataOutputStream(socket[i].getOutputStream());
			toClient[i].writeInt(REVEAL);
			toClient[i].writeInt(cardToReveal);
			toClient[i].writeInt(cards[cardToReveal]);
			toClient[i].flush();
		}
		currentPick = ++currentPick % 2;
		if(currentPick == 0){
			if(cards[chosenCards[0]] == cards[chosenCards[1]]){
				log("%s: Match found at cards %d and %d, adding %d point(s)", commandString(MATCH), chosenCards[0], chosenCards[1], 1);
				points[currentPlayer] += POINTS_TO_GIVE;
				toClient[currentPlayer].writeInt(MATCH);
				toClient[currentPlayer].writeInt(points[currentPlayer]);
				toClient[currentPlayer].flush();
				pairsLeft--;
				if(pairsLeft == 0){
					determineWinner();
					return;
				}
			} else {
				clientSleep();
				hideCards();
			}
			setTurn(false);
		}
	}

	/**
	 * Make a client sleep
	 * @throws IOException
	 */
	private void clientSleep() throws IOException{
		log("%s: Making clients sleep for %dms", commandString(WAIT), DEFAULT_SLEEP_TIME);
		for(int i = 0; i < socket.length; i ++){
			toClient[i] = new DataOutputStream(socket[i].getOutputStream());
			toClient[i].writeInt(WAIT);
			toClient[i].writeInt(DEFAULT_SLEEP_TIME);
			toClient[i].flush();
		}
	}

	/**
	 * Hides the cards a player has uncovered
	 * @throws IOException
	 */
	private void hideCards() throws IOException {
		log("%s: Hiding cards %d and %d", commandString(HIDE), chosenCards[0], chosenCards[1]);
		for(int i = 0; i < socket.length; i++){
			toClient[i] = new DataOutputStream(socket[i].getOutputStream());
			toClient[i].writeInt(HIDE);
			toClient[i].writeInt(chosenCards[0]);
			toClient[i].writeInt(chosenCards[1]);
			toClient[i].flush();
		}
	}

	/**
	 * Determines who wins the game
	 * @throws IOException
	 */
	private void determineWinner() throws IOException{
		determineWinner(-1);
	}

	/**
	 * Determines who wins the game.
	 * The player who quit will automatically lose the game
	 * @param quitter	The player that quit the game
	 * @throws IOException
	 */
	private void determineWinner(int quitter) throws IOException{
		int mostPoints = -1;
		int player = 0;
		
		for(int i = 0; i < points.length; i++){
			if(points[i] > mostPoints && i != quitter){
				mostPoints = points[i];
				player = i;
			}
		}
		
		if(points[currentPlayer] == mostPoints && currentPlayer != quitter)
			player = currentPlayer;
		
		for(int i = 0; i < socket.length; i++){
			toClient[i] = new DataOutputStream(socket[i].getOutputStream());
			toClient[i].writeInt(WIN);
			toClient[i].writeInt(player);
			toClient[i].flush();
		}
		log("%s: Player %d won", commandString(WIN), (int) player + 1);
		quitGame();
	}

	/**
	 * quit the game
	 * @throws IOException
	 */
	private void quitGame() throws IOException{
		for(int i = 0; i < socket.length; i++){
			toClient[i] = new DataOutputStream(socket[i].getOutputStream());
			toClient[i].writeInt(DONE);
			toClient[i].flush();
		}
		this.done = true;
	}

	/**
	 * creates the game board and shuffles cards
	 * not the best algorithm but it kind of works
	 */
	private void shuffleCards(){
		log("SHUFFLING CARDS");
		int cardsSize = gameWidth * gameHeight;
		cards = new int[cardsSize];
		for(int i = 0; i < cards.length; i++)
			cards[i] = -1;

		int pairs = cardsSize / 2;
		pairsLeft = pairs;

		int i = 0;
		int currentPair = 0;
		int add = 0;

		java.util.Random rand = new java.util.Random(System.currentTimeMillis());

		while(currentPair < pairs){
			i = rand.nextInt(cardsSize);
			if(cards[i] == -1){
				if(rand.nextInt(20) < 2){
					cards[i] = currentPair;
					add = ++add % 2;
					if(add == 0)
						currentPair++;
				}
			}
		}
	}

	/**
	 * output log messages to the server log area
	 * @param msg	the message to output
	 * @param vals	values used in place of tokens specified in msg
	 */
	private void log(String msg, Object... vals){
		String message = String.format("GAME %d: %s%n", (int) gameNumber + 1, msg);
		logArea.append(String.format(message, vals));
	    logArea.setCaretPosition(logArea.getDocument().getLength());
	}
}
