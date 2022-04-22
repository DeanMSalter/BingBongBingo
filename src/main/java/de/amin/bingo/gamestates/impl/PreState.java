package de.amin.bingo.gamestates.impl;

import de.amin.bingo.BingoPlugin;
import de.amin.bingo.game.BingoGame;
import de.amin.bingo.game.board.map.BoardRenderer;
import de.amin.bingo.gamestates.GameState;
import de.amin.bingo.gamestates.GameStateManager;
import de.amin.bingo.utils.Config;
import de.amin.bingo.utils.ItemBuilder;
import de.amin.bingo.utils.Localization;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class PreState extends GameState {

    private final BingoPlugin plugin;
    private final GameStateManager gameStateManager;
    private int time = Config.PRESTATE_TIME;
    private BukkitTask timerTask;
    private final BingoGame game;
    private final BoardRenderer renderer;
    public PreState(BingoPlugin plugin, GameStateManager gameStateManager, BingoGame game, BoardRenderer renderer) {
        this.plugin = plugin;
        this.gameStateManager = gameStateManager;
        this.game = game;
        this.renderer = renderer;
    }

    @Override
    public void start() {
        startTimer();
    }

    @Override
    public void end() {
        timerTask.cancel();
    }

    private void startTimer() {
        Server server = plugin.getServer();

        timerTask = server.getScheduler().runTaskTimer(plugin, () -> {
            if (time > 0) {
                this.game.getPlayers().forEach(playerID -> {
                    Player player = Bukkit.getPlayer(playerID);
                    switch (time) {
                        case 60:
                        case 30:
                        case 15:
                        case 10:
                        case 5:
                        case 3:
                        case 2:
                        case 1: {
                            player.sendMessage(Localization.get(player, "game.prestate.timer", String.valueOf(time)));
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                        }
                    }
                    player.setLevel(time);
                });
                time--;
            } else {
                gameStateManager.setGameState(GameState.MAIN_STATE);
            }
        }, 0, 20);
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }
}
