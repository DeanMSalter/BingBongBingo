package de.amin.bingo.commands;

import de.amin.bingo.BingoPlugin;
import de.amin.bingo.game.BingoGame;
import de.amin.bingo.game.board.map.BoardRenderer;
import de.amin.bingo.gamestates.GameState;
import de.amin.bingo.gamestates.GameStateManager;
import de.amin.bingo.gamestates.impl.MainState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PauseCommand implements CommandExecutor {

    private BingoPlugin plugin;
    public PauseCommand(BingoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player))return false;
        Player player = (Player) sender;
        BingoGame game = this.plugin.getGamePlayerIsIn(player);

        if (game == null){
            player.sendMessage("You are not in a game");
            return false;
        }
        game.saveGame();
        game.pauseGame();
        return false;
    }
}
