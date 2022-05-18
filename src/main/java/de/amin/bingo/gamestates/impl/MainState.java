package de.amin.bingo.gamestates.impl;

import de.amin.bingo.BingoPlugin;
import de.amin.bingo.game.BingoGame;
import de.amin.bingo.game.board.BingoItem;
import de.amin.bingo.game.board.map.BoardRenderer;
import de.amin.bingo.gamestates.GameState;
import de.amin.bingo.gamestates.GameStateManager;
import de.amin.bingo.utils.Config;
import de.amin.bingo.utils.Localization;
import de.amin.bingo.utils.TimeUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainState extends GameState {

    private int time = Config.GAME_DURATION;
    private BukkitTask timerTask;
    private BukkitTask gameLoop;
    private final BingoPlugin plugin;
    private final GameStateManager gameStateManager;
    private final BingoGame game;
    private final BoardRenderer renderer;
    private final List<UUID> offlinePlayers;


    public MainState(BingoPlugin plugin, GameStateManager gameStateManager, BingoGame game, BoardRenderer renderer) {
        this.plugin = plugin;
        this.gameStateManager = gameStateManager;
        this.game = game;
        this.renderer = renderer;
        this.offlinePlayers = new ArrayList<>();

    }
    public int getTimeLeft(){
        return this.time;
    }
    @Override
    public void start() {
        if (game.getBoards().size() == 0) {
            game.createBoards(this.game.getPlayers());
        }
        renderer.updateImages();
        ItemStack boardMap = getRenderedMapItem();
        game.setBoardItem(boardMap);

        for(UUID player: this.game.getPlayers()){
            Bukkit.getPlayer(player).getInventory().addItem(boardMap);
        }

        startTimer();
    }

    @Override
    public void end() {
        timerTask.cancel();
        gameLoop.cancel();
    }

    private void startTimer() {

        Scoreboard scoreboard = plugin.getServer().getScoreboardManager().getMainScoreboard();
        Objective score = scoreboard.getObjective(DisplaySlot.PLAYER_LIST) == null ? scoreboard.registerNewObjective("score","moneyboy","swag", RenderType.INTEGER) : scoreboard.getObjective(DisplaySlot.PLAYER_LIST);
        score.setDisplaySlot(DisplaySlot.PLAYER_LIST);

        gameLoop = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            this.game.getPlayers().forEach(playerID -> {
                Player player = Bukkit.getPlayer(playerID);
                if (player == null) {
                    setPlayerStatus(playerID, false);
                    handleOfflinePlayer(score);
                } else {
                    setPlayerStatus(playerID, true);
                    score.getScore(player.getName()).setScore(game.getBoard(player).getFoundItems());

                    //check for all players if they have a new item from the board
                    for (BingoItem item : game.getBoard(player).getItems()) {
                        if (!item.isFound()) {
                            for (ItemStack content : player.getInventory().getContents()) {
                                if (content != null && content.getType().equals(item.getMaterial())) {
                                    item.setFound(true);
                                    plugin.getServer().broadcastMessage(Localization.get(player, "game.mainstate.itemfound",
                                            String.valueOf(game.getBoard(player).getFoundItems()),
                                            String.valueOf(Config.BOARD_SIZE)));
                                    this.game.getPlayers().forEach(playerIDAll -> {
                                        Player playerAll = Bukkit.getPlayer(playerIDAll);
                                        playerAll.playSound(playerAll.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1, 1);
                                    });
                                    this.game.saveGame();
                                }
                            }
                        }
                    }
                    if (game.checkWin(player)) {
                        game.getPlayers().forEach(playerUUID -> {
                            Bukkit.getPlayer(playerUUID).sendMessage(Localization.get(player, "game.mainstate.win"));
                        });
                        score.unregister();
                        gameStateManager.setGameState(GameState.END_STATE);
                    }
                }
            });

        }, 0, 5);

        timerTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (time > 0) {
                this.game.getPlayers().forEach(playerID -> {
                    Player player = Bukkit.getPlayer(playerID);
                    if (player == null) {
                        setPlayerStatus(playerID, false);
                        handleOfflinePlayer(score);
                    } else {
                        setPlayerStatus(playerID, true);
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new ComponentBuilder(ChatColor.GREEN + TimeUtils.formatTime(time)).create());

                        switch (time) {
                            case 30:
                            case 15:
                            case 10:
                            case 5:
                            case 3:
                            case 2:
                            case 1: {
                                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1, 1);
                                player.sendMessage(Localization.get(player, "game.mainstate.end", String.valueOf(time)));
                            }
                        }
                    }
                });


                time--;
                game.setTimeLeft(time);
            } else {
                this.game.getPlayers().forEach(playerID -> {
                    Player player = Bukkit.getPlayer(playerID);
                    player.sendMessage(Localization.get(player, "game.mainstate.no_winner"));
                });
                gameLoop.cancel();
                score.unregister();
                gameStateManager.setGameState(GameState.END_STATE);
            }
        }, 0, 20);

    }
    private void handleOfflinePlayer(Objective score){
        if (offlinePlayers.size() == this.game.getPlayers().size()) {
            gameLoop.cancel();
            score.unregister();
            gameStateManager.setGameState(GameState.END_STATE);
        }
        this.game.getPlayers().forEach(playerIDOnline -> {
            Player playerOnline = Bukkit.getPlayer(playerIDOnline);
            if (playerOnline != null) {
                playerOnline.sendMessage("A player has left the game, you can pause the game or carry on.");
            }
        });
    }
    private void setPlayerStatus(UUID playerID, Boolean online) {
        if (this.offlinePlayers.contains(playerID) && online) {
            this.offlinePlayers.remove(playerID);
        } else if(!this.offlinePlayers.contains(playerID) && !online){
            this.offlinePlayers.add(playerID);
        }
    }
    public void setTime(int time) {
        this.time = time;
    }
    public ItemStack getRenderedMapItem() {
        ItemStack itemStack = new ItemStack(Material.FILLED_MAP);
        MapView view = Bukkit.createMap(Bukkit.getWorlds().get(0));
        //clear renderers one by one
        for (MapRenderer renderer : view.getRenderers())
            view.removeRenderer(renderer);

        view.addRenderer(renderer);
        MapMeta mapMeta = (MapMeta) itemStack.getItemMeta();
        mapMeta.setMapView(view);
        mapMeta.setUnbreakable(true);
        mapMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Bingo Board");
        itemStack.setItemMeta(mapMeta);
        return itemStack;
    }
}
