
public interface GameConstants {

	int PORT = 2048;

	/**
	 * INIT p w h
	 * Initialize the client.
	 * p player number
	 * w game width
	 * h game height
	 */
	int INIT = 101;

	/**
	 * SETTURN p
	 * give the player with the given number a turn
	 * p player number
	 */
	int SETTURN	= 0;

	/**
	 * RECEIVE n
	 * receive the card the player selected
	 * n index of the selected card
	 */
	int RECEIVE	= 1;

	/**
	 * REVEAL c n
	 * reveal the card index received with the given value
	 * c the index of the card to reveal
	 * n the value of the card
	 */
	int	REVEAL = 2;

	/**
	 * WAIT a
	 * wait for the specified amount of milliseconds
	 * a the amount of milliseconds to sleep
	 */
	int WAIT = 3;

	/**
	 * MATCH p
	 * check if the player found a match and update points
	 * p the amount of points the person has
	 */
	int MATCH = 4;

	/**
	 * HIDE c1 c2
	 * hide the card (show the back side) at the index specified
	 * c1 the index of the card to hide
	 * c2 the index of the card to hide
	 */
	int HIDE = 5;

	/**
	 * WIN p
	 * end game and specify who won
	 * p the player who won
	 */
	int WIN = 6;

	/**
	 * QUIT p
	 * receive a quit request from player and makes them lose
	 * p the player who quit
	 */
	int QUIT = 7;

	/**
	 * DONE p
	 * Tell the clients who quit and makes them lose
	 * p the player that sent the quit request
	 */
	int DONE = 8;

	/**
	 * return the command given the integer representation
	 * @param command	the command
	 * @return the string of the command
	 */
	default String commandString(int command){
		switch(command){
			case SETTURN:
				return "SETTURN";
			case RECEIVE:
				return "RECEIVE";
			case REVEAL:
				return "REVEAL";
			case WAIT:
				return "WAIT";
			case MATCH:
				return "MATCH";
			case HIDE:
				return "HIDE";
			case WIN:
				return "WIN";
			case QUIT:
				return "QUIT";
			case DONE:
				return "DONE";
			case INIT:
				return "INIT";
			default:
				return "INVALID COMMAND";
		}
	}
}
