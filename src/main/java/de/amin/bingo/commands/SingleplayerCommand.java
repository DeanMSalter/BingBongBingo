package de.amin.bingo.commands;

import de.amin.bingo.BingoPlugin;
import de.amin.bingo.game.BingoGame;
import de.amin.bingo.game.board.map.BoardRenderer;
import de.amin.bingo.gamestates.GameState;
import de.amin.bingo.gamestates.GameStateManager;
import de.amin.bingo.gamestates.impl.MainState;
import de.amin.bingo.gamestates.impl.PreState;
import de.amin.bingo.utils.Config;
import de.amin.bingo.utils.Localization;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SingleplayerCommand implements CommandExecutor {

    private BingoPlugin plugin;
    private BingoGame game;
    private BoardRenderer renderer;
    private GameStateManager gameStateManager;

    public SingleplayerCommand(BingoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player))return false;
        Player player = (Player) sender;
        List<UUID> players = new ArrayList<UUID>();
        players.add(player.getUniqueId());
        this.game = new BingoGame(players, plugin);
        this.renderer = new BoardRenderer(plugin, game);
        this.gameStateManager = new GameStateManager(plugin, game, renderer);

        gameStateManager.setGameState(GameState.MAIN_STATE);
        ((MainState) gameStateManager.getCurrentGameState()).setTime(Config.GAME_DURATION);

//        game.createBoards(players);
        renderer.updateImages();
        return false;
    }
}
