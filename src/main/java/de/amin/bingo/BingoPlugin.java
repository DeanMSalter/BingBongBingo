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
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class BingoPlugin extends JavaPlugin {

    public static BingoPlugin INSTANCE;
    public static HashMap<Integer, BingoGame> games = new HashMap<Integer, BingoGame>();
    private int highestID = 0;
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
        getCommand("createGame").setExecutor(new createGameCommand(this));
        getCommand("invite").setExecutor(new inviteCommand(this));


    }
    public static Location findLocation(World world){
        WorldBorder worldBorder = world.getWorldBorder();
        double X = ThreadLocalRandom.current().nextDouble(worldBorder.getCenter().getX(), worldBorder.getSize()/2 - worldBorder.getSize() * 0.1 + 1);
        double Z = ThreadLocalRandom.current().nextDouble(worldBorder.getCenter().getZ(), worldBorder.getSize()/2 - worldBorder.getSize() * 0.1 + 1);
        double Y = world.getHighestBlockYAt((int) X, (int) Z);
        return new Location(world,X,Y, Z);
    }
    public static boolean isSafeLocation(Location location) {
        Block feet = location.getBlock();
        if (!feet.isPassable() && !feet.getLocation().add(0, 1, 0).getBlock().isPassable()) {
            return false; // not transparent (will suffocate)
        }
        Block head = feet.getRelative(BlockFace.UP);
        if (!head.isPassable()) {
            return false; // not transparent (will suffocate)
        }
        Block ground = feet.getRelative(BlockFace.DOWN);
        if (!ground.getType().isSolid()) {
            return false; // not solid
        }
        return true;
    }
    public int getHighestID() {
        return highestID;
    }

    public void loadGame(int gameID){
        if (gameID > highestID) {
            highestID = gameID;
        }
        HashMap<Object, BingoBoard> boards = new HashMap<>();
        List<UUID> players = new ArrayList<UUID>();
        HashMap<UUID, Location> positions = new HashMap<>();
        HashMap<UUID, Location> previousPositions = new HashMap<>();

        File gamesFile = new File(INSTANCE.getDataFolder(), "games.yml");
        FileConfiguration gamesConfig = YamlConfiguration.loadConfiguration(gamesFile);
        ConfigurationSection gameSection = gamesConfig.getConfigurationSection(String.valueOf(gameID));
        int timeLeft = gameSection.getInt("timeLeft");
        boolean active = gameSection.getBoolean("active");

        ConfigurationSection positionsSection = gameSection.getConfigurationSection("positions");
        for(String key : positionsSection.getKeys(false)) {
            ConfigurationSection position = positionsSection.getConfigurationSection(key);
            Location location = new Location(Bukkit.getWorld(position.getString("WORLD")), position.getDouble("X"),position.getDouble("Y"), position.getDouble("Z"), (float) position.getDouble("YAW"), (float) position.getDouble("PITCH"));
            positions.put(UUID.fromString(key), location);
        }

        ConfigurationSection previousPositionsSection = gameSection.getConfigurationSection("previousPositions");
        if (previousPositionsSection != null){
            for(String key : previousPositionsSection.getKeys(false)) {
                ConfigurationSection position = previousPositionsSection.getConfigurationSection(key);
                Location location = new Location(Bukkit.getWorld(position.getString("WORLD")), position.getDouble("X"),position.getDouble("Y"), position.getDouble("Z"), (float) position.getDouble("YAW"), (float) position.getDouble("PITCH"));
                previousPositions.put(UUID.fromString(key), location);
            }
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
        games.put(gameID, new BingoGame(INSTANCE, players, gameID, boards, loadedGameItems, timeLeft, positions, active, previousPositions));
    }
    public void setGame(int gameID, BingoGame game) {
        this.games.put(gameID, game);
    }
    public BingoGame getGamePlayerIsIn(Player player){
        for(Map.Entry<Integer, BingoGame> entry : INSTANCE.getGames().entrySet()) {
            BingoGame game = entry.getValue();
            if (game.getActive()) {
                List<UUID> players = game.getPlayers();
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
