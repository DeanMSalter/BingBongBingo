package de.amin.bingo.game;

import de.amin.bingo.BingoPlugin;
import de.amin.bingo.game.board.BingoBoard;
import de.amin.bingo.game.board.BingoItem;
import de.amin.bingo.game.board.BingoMaterial;
import de.amin.bingo.game.board.map.BoardRenderer;
import de.amin.bingo.gamestates.GameState;
import de.amin.bingo.gamestates.GameStateManager;
import de.amin.bingo.gamestates.impl.EndState;
import de.amin.bingo.gamestates.impl.MainState;
import de.amin.bingo.gamestates.impl.PauseState;
import de.amin.bingo.utils.Config;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
//import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class BingoGame {
    private BingoPlugin plugin;
    private HashMap<Object, BingoBoard> boards;
    private final List<UUID> players;
    private int gameID;
    private ItemStack boardItem;
    private BoardRenderer renderer;
    private GameStateManager gameStateManager;
    BingoMaterial[] items = new BingoMaterial[Config.BOARD_SIZE];
    private int timeLeft = Config.GAME_DURATION;
    private HashMap<UUID, Location> positions;
    private HashMap<UUID, Location> previousPositions;

    private boolean active = true;
    private World world = Bukkit.getWorld("Bingo");
    public BingoGame(BingoPlugin plugin, List<UUID> players) {
        this.plugin = plugin;
        this.players = players;
        this.gameID = this.plugin.getHighestID() + 1;
        boards = new HashMap<>();
        positions = new HashMap<>();
        previousPositions = new HashMap<>();
        saveGame();
    }
    public BingoGame(BingoPlugin plugin, List<UUID> players, int gameID, HashMap<Object, BingoBoard> boards, BingoMaterial[] items, int timeLeft, HashMap<UUID, Location> positions, boolean active, HashMap<UUID, Location> previousPositions) {
        this.plugin = plugin;
        this.players = players;
        this.gameID = gameID;
        this.boards = boards;
        this.items = items;
        this.timeLeft = timeLeft;
        this.positions = positions;
        this.active = active;
        this.previousPositions = previousPositions;
    }
    public void startGame(){
        this.renderer = new BoardRenderer(plugin, this);
        this.gameStateManager = new GameStateManager(plugin, this, renderer);
        gameStateManager.setGameState(GameState.MAIN_STATE);
        if (this.timeLeft == Config.GAME_DURATION) {
            Player player = Bukkit.getPlayer(this.players.get(0));
            Location locationToUse = null;
            for(int i = 0; i < 20; i++) {
                Location newLocation = BingoPlugin.findLocation(this.world);
                player.sendMessage("finding location....");
                if (BingoPlugin.isSafeLocation(newLocation)){
                    player.sendMessage("found safe location!");
                    locationToUse = newLocation;
                    break;
                } else {
                    player.sendMessage("location not safe :(");
                }
            }
            for (UUID uuid : this.players){
                this.previousPositions.put(uuid, player.getLocation());
                if (locationToUse == null) {
                    player.sendMessage("Could not find a safe location so using current position, please contact staff or try again.");
                    this.positions.put(uuid,player.getLocation());
                } else{
                    this.positions.put(uuid,locationToUse);
                }
            }
            this.savePreviousPositions();
        }
        this.players.forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            player.teleport(positions.get(uuid));
        });
        ((MainState) gameStateManager.getCurrentGameState()).setTime(this.timeLeft);
        renderer.updateImages();
        this.saveGame();
    }
    public World getWorld(){
        return this.world;
    }
    public void endGame(){
        this.gameStateManager.setGameState(GameState.END_STATE);
    }
    public void setTimeLeft(int timeLeft){
        this.timeLeft = timeLeft;
    }
    public HashMap<UUID, Location> getPreviousPositions(){
        return this.previousPositions;
    }

    public void pauseGame(){
        this.gameStateManager.setGameState(GameState.PAUSE_STATE);
    }
    public void startGame(Player player){
        if (!this.players.contains(player.getUniqueId())) {
            player.sendMessage("You are not allowed to start this game.");
            return;
        }
        if (!this.active){
            player.sendMessage("You are not allowed to start this game as it is not active.");
            return;
        }
        if (playerAlreadyPlaying()) {
            player.sendMessage("Can not start game as a player is already in a game.");
            return;
        }
        for (UUID playerInGameUUID: this.players) {
            Player playerInGame = Bukkit.getPlayer(playerInGameUUID);
            if (playerInGame == null || !playerInGame.isOnline()) {
                player.sendMessage("Can not start game as a player is offline.");
                return;
            }
        }
        startGame();
    }
    public boolean playerAlreadyPlaying(){
        boolean playerAlreadyPlaying = false;
        for(Map.Entry<Integer, BingoGame> entry : this.plugin.getGames().entrySet()) {
            Integer gameID = entry.getKey();
            BingoGame game = entry.getValue();
            List<UUID> players = game.getPlayers();
            GameStateManager gameStateManager = game.getGameStateManager();

            if (gameStateManager != null && gameStateManager.getCurrentGameState() instanceof MainState) {
                for (UUID playerID : this.players) {
                    if (players.contains(playerID)) {
                        playerAlreadyPlaying = true;
                        break;
                    }
                }
            }
        }
        return playerAlreadyPlaying;
    }
    public void createBoards() {
        //generation of random items
        for (int i = 0; i < items.length; i++) {
            BingoMaterial bingoMaterial = getRandomMaterial();
            while (Arrays.asList(items).contains(bingoMaterial)) {
                bingoMaterial = getRandomMaterial();
            }
            items[i] = bingoMaterial;
        }
    }

    public void createBoards(List<UUID> players) {
        //generation of random items
        for (int i = 0; i < items.length; i++) {
            BingoMaterial bingoMaterial = getRandomMaterial();
            while (Arrays.asList(items).contains(bingoMaterial)) {
                bingoMaterial = getRandomMaterial();
            }
            items[i] = bingoMaterial;
        }
        for (UUID player : players) {
            boards.put(player, new BingoBoard(items));
        }
    }

    public boolean checkWin(Player player) {
        int[][] winSituations = new int[][]{
                //horizonzal
                {0, 1, 2, 3},
                {4, 5, 6, 7},
                {8, 9, 10, 11},
                {12, 13, 14, 15},
                //vertical
                {0, 4, 8, 12},
                {1, 5, 9, 13},
                {2, 6, 10, 14},
                {3, 7, 11, 15},
                //diagonal
                {0, 5, 10, 15},
                {3, 6, 9, 12}
        };
        for (int[] winSituation : winSituations) {
            boolean win = true;
            for (int i : winSituation) {
                if (!getBoard(player).getItems()[i].isFound()) {
                    win = false;
                }
            }
            if (win) {
                return true;
            }
        }
        return false;
    }

    public boolean getActive(){
        return this.active;
    }
    public void setActive(Boolean active){
        this.active = active;
    }
    public BingoMaterial getRandomMaterial() {
        return BingoMaterial.values()[new Random().nextInt(BingoMaterial.values().length)];
    }
    public HashMap<Object, BingoBoard> getBoards(){
        return this.boards;
    }
    public int getGameID(){
        return this.gameID;
    }
    public void setBoardItem(ItemStack boardItem) {
        this.boardItem = boardItem;
    }

    public ItemStack getBoardItem() {
        return this.boardItem;
    }

    public BingoBoard getBoard(Player player) {
        return boards.get(player.getUniqueId());
    }

    public BingoMaterial[] getItems() {
        return items;
    }

    public GameStateManager getGameStateManager(){
        return this.gameStateManager;
    }
    public List<UUID> getPlayers() {
        return this.players;
    }
    public void addPlayer(Player player) {
        this.players.add(player.getUniqueId());
    }
    public void saveGame() {
        try {
            File gamesFile = new File(this.plugin.getDataFolder(), "games.yml");
            FileConfiguration gamesConfig = YamlConfiguration.loadConfiguration(gamesFile);
            ConfigurationSection newGameSection = gamesConfig.getConfigurationSection(String.valueOf(this.gameID));
            if (newGameSection == null) {
                newGameSection = gamesConfig.createSection(String.valueOf(this.gameID));
            }
            newGameSection.set("active", this.active);
            newGameSection.set("timeLeft", this.timeLeft);

            newGameSection.set("players", this.players.stream().map(UUID::toString).collect(Collectors.toList()));
            ConfigurationSection positionsSection = newGameSection.createSection("positions");
            this.players.forEach(uuid -> {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    ConfigurationSection positionSection = positionsSection.createSection(String.valueOf(uuid));
                    Location location = player.getLocation();
                    positionSection.set("X", location.getX());
                    positionSection.set("Y", location.getY());
                    positionSection.set("Z", location.getZ());
                    positionSection.set("YAW", location.getYaw());
                    positionSection.set("PITCH", location.getPitch());
                    positionSection.set("WORLD", location.getWorld().getName());
                }
            });
            ConfigurationSection boardsSection = newGameSection.createSection("boards");

            for (Map.Entry<Object, BingoBoard> board : this.boards.entrySet()) {
                Object key = board.getKey();
                BingoBoard value = board.getValue();

                ConfigurationSection playerBoardSection = boardsSection.createSection(key.toString());
                playerBoardSection.set("founditems", value.getFoundItems());
                ConfigurationSection playerBoardItemsSection = playerBoardSection.createSection("items");

                int itemIndex = 0;
                for(BingoItem item : value.getItems()) {
                    ConfigurationSection boardItemSection = playerBoardItemsSection.createSection(String.valueOf(itemIndex));
                    boardItemSection.set("found", item.isFound());
                    boardItemSection.set("material", item.getMaterial().name());
                    boardItemSection.set("bingoMaterial", item.getBingoMaterial().getMaterial().name());
                    itemIndex += 1;
                }

            }
            gamesConfig.save(gamesFile);
            Bukkit.getLogger().info("done saving");
            plugin.setGame(this.gameID, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void savePreviousPositions(){
        try {
            File gamesFile = new File(this.plugin.getDataFolder(), "games.yml");
            FileConfiguration gamesConfig = YamlConfiguration.loadConfiguration(gamesFile);
            ConfigurationSection newGameSection = gamesConfig.createSection(String.valueOf(this.gameID));
            ConfigurationSection previousPositionsSection = newGameSection.createSection("previousPositions");
            this.players.forEach(uuid -> {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    ConfigurationSection positionSection = previousPositionsSection.createSection(String.valueOf(uuid));
                    Location location = player.getLocation();
                    positionSection.set("X", location.getX());
                    positionSection.set("Y", location.getY());
                    positionSection.set("Z", location.getZ());
                    positionSection.set("YAW", location.getYaw());
                    positionSection.set("PITCH", location.getPitch());
                    positionSection.set("WORLD", location.getWorld().getName());
                }
            });
            gamesConfig.save(gamesFile);
            Bukkit.getLogger().info("done saving prev positions");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
