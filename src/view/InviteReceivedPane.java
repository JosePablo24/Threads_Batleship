package view;

import model.MatchRoom;

import javax.swing.*;
import java.awt.*;

/**
 * An option pane asking the user if they would like to play a game against a
 * player who has invited them.
 */
public class InviteReceivedPane extends JOptionPane {

    private JDialog dialog;
    private MatchRoom matchRoom;
    private String key;

    /**
     * Constructs InviteReceivedPane with the key of the player inviting them,
     * the name of the player inviting them, and a reference to the
     * {@link model.MatchRoom} object to send the response back to.
     *
     * @param key key of player inviting
     * @param name name of player inviting
     * @param matchRoom MatchRoom object to send response too
     */
    public InviteReceivedPane(String key, String name, MatchRoom matchRoom) {
        super();
        this.setMessage(name + " quiere jugar contigo.");
        this.setMessageType(QUESTION_MESSAGE);
        this.setOptionType(YES_NO_OPTION);
        String[] options = {"Aceptar", "Rechazar"};
        this.setOptions(options);
        this.key = key;
        this.matchRoom = matchRoom;
    }

    /**
     * Shows InviteReceivedPane, and informs {@link model.MatchRoom} of the
     * user's response.
     *
     * @param parent the frame to display the dialog in
     */
    public void showOptionPane(Component parent) {
        dialog = this.createDialog(parent, "Invitar");
        dialog.setVisible(true);
        dialog.dispose();
        if (getValue() == "Aceptar") {
            matchRoom.sendStringArray(new String[]{"join", "accept", key});
        } else if (getValue() == "Rechazar") {
            matchRoom.sendStringArray(new String[]{"join", "reject", key});
        }
    }

    /**
     * Disposes of the frame, cancelling the user's chance to respond.
     */
    public void dispose() {
        dialog.dispose();
    }
}
