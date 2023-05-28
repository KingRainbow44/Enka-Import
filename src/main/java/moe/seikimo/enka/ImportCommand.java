package moe.seikimo.enka;

import emu.grasscutter.command.Command;
import emu.grasscutter.command.CommandHandler;
import emu.grasscutter.game.player.Player;

import java.util.List;

@Command(label = "import", aliases = {"enka"}, permission = "enka.import",
        targetRequirement = Command.TargetRequirement.NONE)
public final class ImportCommand implements CommandHandler {
    @Override
    public void execute(Player sender, Player targetPlayer, List<String> args) {
        // Check if the command was executed by a player.
        if (sender == null) {
            CommandHandler.sendMessage(null, "This command can only be executed by a player.");
            return;
        }

        // Check arguments.
        if (args.size() < 1) {
            CommandHandler.sendMessage(sender, "Usage: /import <official UID>");
            return;
        }

        // Get the player's official UID.
        var uid = args.get(0);
        CommandHandler.sendMessage(sender, "Attempting to query data for " + uid + ".");

        // Query the player's data.
        var data = Importer.queryUser(uid);
        if (data == null) {
            CommandHandler.sendMessage(sender, "Failed to query user data.");
            return;
        }

        // Import the data for the player.
        CommandHandler.sendMessage(sender, "Data found! Import will begin now.");
        Importer.importToPlayer(sender, data);

        // Notify the player.
        CommandHandler.sendMessage(sender, "Import completed!");
        CommandHandler.sendMessage(sender, "You will need to log in again.");

        new Thread(() -> {
            try {
                Thread.sleep(5000L);
                sender.kick();
            } catch (Exception ignored) { }
        }).start();
    }
}
