package de.amin.bingo.commands;

import de.amin.bingo.BingoPlugin;
import de.amin.bingo.game.BingoGame;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class inviteCommand implements CommandExecutor {

    private BingoPlugin plugin;
    public inviteCommand(BingoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player))return false;
        Player player = (Player) sender;
        if (args[0] == null) {
            player.sendMessage("No username given.");
            return false;
        }
        Player playerToInvite = Bukkit.getPlayer(args[0]);
        if(playerToInvite == null) {
            player.sendMessage("No online player found with that username.");
            return false;
        }
        BingoGame game = this.plugin.getGamePlayerIsIn(player);
        if (game == null){
            player.sendMessage("You are not in a game");
            return false;
        }
        playerToInvite.sendMessage("you have been added to a game");
        game.addPlayer(playerToInvite);
        game.saveGame();
        return false;
    }
}
