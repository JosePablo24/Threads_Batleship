package model;

import server.Game;
import server.messages.ChatMessage;
import server.messages.MoveMessage;
import server.messages.MoveResponseMessage;
import server.messages.NotificationMessage;
import view.ClientView;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * A Client used for communicating with the server. Contains both player's 
 * {@link Board}s, an {@link ObjectOutputStream} and an 
 * {@link ObjectInputStream}.
 */
public class Client extends Thread {

    private Board ownBoard;
    private Board opponentBoard;
    private ClientView view;

    private ObjectOutputStream out;
    private ObjectInputStream in;

    private String opponentName = "Jugador";

    /**
     * Constructs a Client with the players' {@link Board}s, {@link ClientView} 
     * and streams.
     * @param clientView
     *          The {@link ClientView} used for the GUI
     * @param ownBoard
     *          The {@link Board} belonging to the player
     * @param opponentBoard
     *          The {@link Board} belonging to the player's opponent
     * @param out
     *          The {@link ObjectOutputStream} for sending data
     * @param in 
     *          The {@link ObjectOutputStream} for receiving data
     */
    public Client(ClientView clientView, Board ownBoard, Board opponentBoard,
            ObjectOutputStream out, ObjectInputStream in) {
        this.ownBoard = ownBoard;
        this.opponentBoard = opponentBoard;
        this.view = clientView;

        ownBoard.setClient(this);
        opponentBoard.setClient(this);

        this.out = out;
        this.in = in;
    }

    /**
     * Runs this {@link Thread}. Waits to receive input from the server, parses 
     * the input and executes instructions based on the input.
     */
    @Override
    public void run() {
        super.run();
        Object input;
        try {
            while ((input = in.readObject()) != null) {
                parseInput(input);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    /**
     * Determines the type of message of the input and then responds 
     * accordingly.
     * @param input 
     *          The message from the server allowing the Client to determine 
     *          what course of action to take next.
     */
    public void parseInput(Object input) {
        if (input instanceof NotificationMessage) {
            NotificationMessage n = (NotificationMessage) input;
            switch (n.getCode()) {
            case NotificationMessage.OPPONENTS_NAME:
                // TODO: handle receiving opponents name
                //view.addChatMessage("Received opponent's name.");
                if (n.getText().length == 1) {
                    opponentName = n.getText()[0];
                    view.setTitle("Playing Battleships against " +
                            opponentName);
                }
                break;
            case NotificationMessage.BOARD_ACCEPTED:
                view.setMessage("Tablero Aceptado. Esperando oponente.");
                view.stopTimer();
                ownBoard.setBoatPositionLocked(true);
                break;
            case NotificationMessage.GAME_TOKEN:
                // TODO: handle receiving game token to share with friend
                view.addChatMessage("Received game token.");
                break;
            case NotificationMessage.GAME_NOT_FOUND:
                // TODO: handle joining a game that doesn't exist
                view.addChatMessage("Juego no encontrado.");
                break;
            case NotificationMessage.PLACE_SHIPS:
                // TODO: allow player to start positioning ships
                //view.addChatMessage("Can place ships now.");
                ownBoard.setBoatPositionLocked(false);
                break;
            case NotificationMessage.YOUR_TURN:
                view.stopTimer();
                view.setTimer(Game.TURN_TIMEOUT / 1000);
                view.setMessage("Es tu turno.");
                break;
            case NotificationMessage.OPPONENTS_TURN:
                view.stopTimer();
                view.setTimer(Game.TURN_TIMEOUT / 1000);
                view.addChatMessage("Es turno del oponente.");
                view.setMessage("Es turno del oponente.");
                break;
            case NotificationMessage.GAME_WIN:
                // TODO: inform player they have won the game
                view.setMessage("¡¡¡¡Ganaste!!!!.");
                view.stopTimer();
                view.gameOverAction("Ganaste");
                break;
            case NotificationMessage.GAME_LOSE:
                // TODO: inform player they have lost the game
                view.setMessage("Perdiste.");
                view.stopTimer();
                view.gameOverAction("Perdiste!");
                break;
            case NotificationMessage.TIMEOUT_WIN:
                // TODO: inform of win due to opponent taking too long
                view.addChatMessage("Tu oponente se tardo demasiado, Ganaste!");
                view.gameOverAction("Tu oponente se tardo demasiado, Ganaste!");
                break;
            case NotificationMessage.TIMEOUT_LOSE:
                // TODO: inform of loss due to taking too long
                view.addChatMessage("Te tomaste mucho tiempo perdiste!");
                view.gameOverAction("Te tomaste mucho tiempo perdiste!");
                break;
            case NotificationMessage.TIMEOUT_DRAW:
                // TODO: inform that both took too long to place ships
                view.addChatMessage("El juego termino en un empate.");
                view.gameOverAction("El juego termino en un empate.");
                break;
            case NotificationMessage.NOT_YOUR_TURN:
                view.addChatMessage("No es tu turno!");
                break;
            case NotificationMessage.INVALID_BOARD:
                view.addChatMessage("Tablero invalido.");
                break;
            case NotificationMessage.NOT_IN_GAME:
                view.addChatMessage("No estas en el juego.");
                break;
            case NotificationMessage.INVALID_MOVE:
                view.addChatMessage("Movimiento invalido.");
                break;
            case NotificationMessage.REPEATED_MOVE:
                view.addChatMessage("No se puede repetir movimiento.");
                break;
            case NotificationMessage.OPPONENT_DISCONNECTED:
                view.addChatMessage("Oponente desconectado.");
            }
        } else if (input instanceof MoveResponseMessage) {
            MoveResponseMessage move = (MoveResponseMessage) input;
            if (move.isOwnBoard()) {
                ownBoard.applyMove(move);
            } else {
                opponentBoard.applyMove(move);
            }
        } else if (input instanceof ChatMessage) {
            ChatMessage chatMessage = (ChatMessage) input;
            view.addChatMessage("<b>" + opponentName + ":</b> " + chatMessage.getMessage());
        }
    }

    /**
     * Sends the {@link Board} over the {@link ObjectOutputStream}.
     * @param board
     *          The {@link Board} to send to the server
     * @throws IOException 
     */
    public void sendBoard(Board board) throws IOException {
        out.reset();
        out.writeObject(board);
        out.flush();
    }

    /**
     * Gets the {@link ClientView}.
     * @return 
     *          the {@link ClientView} belonging to the Client
     */
    public ClientView getView() {
        return view;
    }

    /**
     * Sends a message to be displayed in the opponents chat window.
     * @param message
     *          The text of the message to be sent
     * @throws IOException 
     */
    public void sendChatMessage(String message) throws IOException {
        System.out.println(message);
        out.writeObject(new ChatMessage(message));
        out.flush();
    }

    /**
     * Sends a move to be executed on the opponent's {@link Board}.
     * @param x
     *          The index of the {@link Square} on the X-axis to be hit
     * @param y
     *          The index of the {@link Square} on the Y-axis to be hit
     * @throws IOException 
     */
    public void sendMove(int x, int y) throws IOException {
        out.writeObject(new MoveMessage(x, y));
        out.flush();
    }

    /**
     * Gets the opponent's name.
     * @return 
     *          the opponent's name
     */
    public String getOpponentName() {
        return opponentName;
    }

}
