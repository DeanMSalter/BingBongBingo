package de.amin.bingo;

import de.amin.bingo.commands.*;
import de.amin.bingo.game.BingoGame;
import de.amin.bingo.game.board.BingoBoard;
import de.amin.bingo.game.board.BingoItem;
import de.amin.bingo.game.board.BingoMaterial;
import de.amin.bingo.game.board.map.BoardRenderer;
import de.amin.bingo.gamestates.GameState;
import de.amin.bingo.gamestates.GameStateManager;
import de.amin.bingo.gamestates.impl.MainState;
import de.amin.bingo.utils.Config;
import de.amin.bingo.utils.Localization;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

public final class BingoPlugin extends JavaPlugin {

    public static BingoPlugin INSTANCE;
    public static HashMap<Integer, BingoGame> games = new HashMap<Integer, BingoGame>();
    @Override
    public void onLoad() {
        INSTANCE = this;
    }

    @Override
    public void onEnable() {

        Localization.load();
        getLogger().info(ChatColor.GREEN + "Plugin has been initialized");

        saveDefaultConfig();
        registerCommands();
        File gamesFile = new File(INSTANCE.getDataFolder(), "games.yml");
        FileConfiguration gamesConfig = YamlConfiguration.loadConfiguration(gamesFile);
        for(String key : gamesConfig.getKeys(false)) {
            loadGame(Integer.parseInt(key));
        }
    }

    private void registerCommands() {
//        getCommand("board").setExecutor(new BoardCommand(game));
//        getCommand("reroll").setExecutor(new RerollCommand(this, game));
        getCommand("singleplayer").setExecutor(new SingleplayerCommand(this));
        getCommand("resume").setExecutor(new ResumeGameCommand(this));
        getCommand("pause").setExecutor(new PauseCommand(this));


    }

    public void loadGame(int gameID){
        HashMap<Object, BingoBoard> boards = new HashMap<>();
        List<UUID> players = new ArrayList<UUID>();
        HashMap<UUID, Location> positions = new HashMap<>();
        File gamesFile = new File(INSTANCE.getDataFolder(), "games.yml");
        FileConfiguration gamesConfig = YamlConfiguration.loadConfiguration(gamesFile);
        ConfigurationSection gameSection = gamesConfig.getConfigurationSection(String.valueOf(gameID));
        int timeLeft = gameSection.getInt("timeLeft");

        ConfigurationSection positionsSection = gameSection.getConfigurationSection("positions");
        for(String key : positionsSection.getKeys(false)) {
            ConfigurationSection position = positionsSection.getConfigurationSection(key);
            Location location = new Location(Bukkit.getWorld(position.getString("WORLD")), position.getDouble("X"),position.getDouble("Y"), position.getDouble("Z"), (float) position.getDouble("YAW"), (float) position.getDouble("PITCH"));
            positions.put(UUID.fromString(key), location);
        }
        List loadedPlayers = gameSection.getList("players");
        for (Object loadedPlayer: loadedPlayers) {
            players.add(UUID.fromString((String) loadedPlayer));
        }
        ConfigurationSection boardSection = gameSection.getConfigurationSection("boards");
        BingoBoard bingoBoard;
        BingoMaterial[] loadedGameItems = new BingoMaterial[Config.BOARD_SIZE];

        for(String key : boardSection.getKeys(false)) {
            ConfigurationSection board = boardSection.getConfigurationSection(key);
            ConfigurationSection items = board.getConfigurationSection("items");
            for(String itemIndex : items.getKeys(false)) {
                ConfigurationSection item = items.getConfigurationSection(itemIndex);
                boolean found = item.getBoolean("found");
                BingoMaterial bingoMaterial = BingoMaterial.valueOf(item.getString("bingoMaterial"));
                loadedGameItems[Integer.parseInt(itemIndex)] = bingoMaterial;
            }
            bingoBoard = new BingoBoard(loadedGameItems);
            BingoItem[] bingoItems = bingoBoard.getItems();

            int index = 0;
            for (BingoItem bingoItem : bingoItems) {
                bingoItem.setFound(items.getConfigurationSection(String.valueOf(index)).getBoolean("found"));
                index += 1;
            }
            boards.put(UUID.fromString(key), bingoBoard);
        }
        games.put(gameID, new BingoGame(INSTANCE, players, gameID, boards, loadedGameItems, timeLeft, positions));
    }
    public BingoGame getGamePlayerIsIn(Player player){
        for(Map.Entry<Integer, BingoGame> entry : INSTANCE.getGames().entrySet()) {
            Integer gameID = entry.getKey();
            BingoGame game = entry.getValue();
            List<UUID> players = game.getPlayers();
            GameStateManager gameStateManager = game.getGameStateManager();

            if (gameStateManager != null && gameStateManager.getCurrentGameState() instanceof MainState) {
                if (players.contains(player.getUniqueId())) {
                    return game;
                }
            }
        }
        return null;
    }
    public HashMap<Integer, BingoGame> getGames(){
        return games;
    }
    public BingoGame getGame(int gameID){
        return games.get(gameID);
    }
    public void addGame(int gameID, BingoGame game){
        games.put(gameID, game);
    }

}
