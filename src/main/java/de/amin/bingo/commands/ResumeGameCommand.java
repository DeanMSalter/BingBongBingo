package de.amin.bingo.commands;

import de.amin.bingo.BingoPlugin;
import de.amin.bingo.game.BingoGame;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ResumeGameCommand implements CommandExecutor {

    private BingoPlugin plugin;
    public ResumeGameCommand(BingoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player))return false;
        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage("Please provide the ID of the game you want to resume.");
            return false;
        }
        BingoGame game = plugin.getGame(Integer.parseInt(args[0]));
        if (game == null) {
            player.sendMessage("Could not find the game specified, please check you are using the right ID");
            return false;
        }
        game.startGame(player);
        return false;
    }
}
