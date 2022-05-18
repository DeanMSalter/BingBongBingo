package de.amin.bingo.gamestates.impl;

import de.amin.bingo.BingoPlugin;
import de.amin.bingo.game.BingoGame;
import de.amin.bingo.game.board.map.BoardRenderer;
import de.amin.bingo.gamestates.GameState;
import de.amin.bingo.gamestates.GameStateManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PauseState extends GameState {

    private BingoPlugin plugin;
    private final GameStateManager gameStateManager;
    private final BingoGame game;
    private final BoardRenderer renderer;
    public PauseState(BingoPlugin plugin, GameStateManager gameStateManager, BingoGame game, BoardRenderer renderer) {
        this.plugin = plugin;
        this.gameStateManager = gameStateManager;
        this.game = game;
        this.renderer = renderer;
    }

    @Override
    public void start() {

        for(UUID playerID : this.game.getPlayers()) {
            Player player = Bukkit.getPlayer(playerID);
            player.teleport(this.game.getPreviousPositions().get(playerID));
            if (player != null) {
                player.sendMessage("Game is pausing.");
                player.getInventory().removeItem(game.getBoardItem());
            }
        }
    }

    @Override
    public void end() {

    }
}
