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

public class createGameCommand implements CommandExecutor {

    private BingoPlugin plugin;
    public createGameCommand(BingoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player))return false;
        Player player = (Player) sender;
        List<UUID> players = new ArrayList<UUID>();
        players.add(player.getUniqueId());
        BingoGame game = new BingoGame(plugin, players);
        plugin.addGame(game.getGameID(), game);
        game.saveGame();
        player.sendMessage("game created with ID" + game.getGameID());
        return false;
    }
}
