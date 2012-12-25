package me.main__.nanotech.irc;

import me.main__.nanotech.BlockChangeAPI;
import org.bukkit.block.BlockFace;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;

import java.util.Collection;

public final class OrientationSelectionPrompt implements Prompt {
    public static final OrientationSelectionPrompt INSTANCE = new OrientationSelectionPrompt();

    private OrientationSelectionPrompt() {}

    @Override
    public String getPromptText(final ConversationContext context) {
        String msg;
        if (!(Boolean)context.getSessionData("init")) {
            context.setSessionData("virtualChangeAPI", new BlockChangeAPI.Virtual((Player) context.getForWhom()));

            Collection<BlockFace> cf = ((BuiltCircuit)context.getSessionData("bc")).getPossibleOrientations();
            BlockFace[] orientations = cf.toArray(new BlockFace[cf.size()]);
            context.setSessionData("faces", orientations);

            StringBuilder message = new StringBuilder("\n\n\n\n\nPlease select an orientation for this IRC:\n");
            message.append("-----\n");
            message.append(" [0] Cancel\n");
            for (int i = 0; i < orientations.length; i++)
                message.append(" [").append(i + 1).append("] ").append(orientationToString(orientations[i])).append('\n');

            context.setSessionData("list", msg = message.toString());

            context.setSessionData("init", true);
        }
        else msg = !(Boolean)context.getSessionData("retry") ? (String)context.getSessionData("list") : "Invalid choice.";
        context.setSessionData("retry", false);
        return msg;
    }

    public static String orientationToString(BlockFace orientation) {
        switch (orientation) {
            case NORTH_NORTH_EAST:
                return "Inputs: South, Outputs: North, Alignment: East";
            case NORTH_NORTH_WEST:
                return "Inputs: South, Outputs: North, Alignment: West";
            case SOUTH_SOUTH_EAST:
                return "Inputs: North, Outputs: South, Alignment: East";
            case SOUTH_SOUTH_WEST:
                return "Inputs: North, Outputs: South, Alignment: West";
            case EAST_NORTH_EAST:
                return "Inputs: West, Outputs: East, Alignment: North";
            case EAST_SOUTH_EAST:
                return "Inputs: West, Outputs: East, Alignment: South";
            case WEST_NORTH_WEST:
                return "Inputs: East, Outputs: West, Alignment: North";
            case WEST_SOUTH_WEST:
                return "Inputs: East, Outputs: West, Alignment: South";
            default:
                return "<invalid (bug)>";
        }
    }

    @Override
    public boolean blocksForInput(final ConversationContext context) {
        return true;
    }

    @Override
    public Prompt acceptInput(final ConversationContext context, final String input) {
        BlockFace[] orientations = (BlockFace[])context.getSessionData("faces");
        BuiltCircuit circuit = (BuiltCircuit)context.getSessionData("bc");
        BlockFace orientation;
        try {
            int i = Integer.parseInt(input);
            if (i == 0) {
                context.getForWhom().sendRawMessage("Cancelled.");
                return END_OF_CONVERSATION;
            }
            orientation = orientations[i - 1]; // we're using user-friendly indices (starting at 1) and 0 is cancel
        } catch (Exception e) {
            context.setSessionData("retry", true);
            return this;
        }
        circuit.build(orientation, (BlockChangeAPI) context.getSessionData("virtualChangeAPI"));
        context.setSessionData("orientation", orientation);
        return PreviewPrompt.INSTANCE;
    }
}
