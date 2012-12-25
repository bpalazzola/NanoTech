package me.main__.nanotech.irc;

import me.main__.nanotech.BlockChangeAPI;
import org.bukkit.block.BlockFace;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;

public final class PreviewPrompt implements Prompt {
    public static final PreviewPrompt INSTANCE = new PreviewPrompt();

    private PreviewPrompt() {}

    @Override
    public String getPromptText(final ConversationContext context) {
        String msg = !(Boolean)context.getSessionData("retry") ? "\n\n\n\n\nIs this okay? (yes/no/cancel)" : "Invalid choice.";
        context.setSessionData("retry", false);
        return msg;
    }

    @Override
    public boolean blocksForInput(final ConversationContext context) {
        return true;
    }

    @Override
    public Prompt acceptInput(final ConversationContext context, final String input) {
        BuiltCircuit circuit = (BuiltCircuit)context.getSessionData("bc");
        if (input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("okay") || input.equalsIgnoreCase("ok")) {
            circuit.build((BlockFace) context.getSessionData("orientation"), new BlockChangeAPI.Real());
            ((IRCController)context.getSessionData("controller")).add(circuit);
            context.getForWhom().sendRawMessage("IRC successfully created.");
            return END_OF_CONVERSATION;
        }

        if (input.equalsIgnoreCase("no") || input.equalsIgnoreCase("back")) {
            circuit.destroy((BlockChangeAPI) context.getSessionData("virtualChangeAPI"));
            return OrientationSelectionPrompt.INSTANCE;
        }

        if (input.equalsIgnoreCase("cancel") || input.equalsIgnoreCase("exit")) {
            circuit.destroy((BlockChangeAPI) context.getSessionData("virtualChangeAPI"));
            return END_OF_CONVERSATION;
        }

        context.setSessionData("retry", true);
        return this;
    }
}
