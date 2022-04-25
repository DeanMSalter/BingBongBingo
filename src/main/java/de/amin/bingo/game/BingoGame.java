package de.amin.bingo.game;

import de.amin.bingo.BingoPlugin;
import de.amin.bingo.game.board.BingoBoard;
import de.amin.bingo.game.board.BingoItem;
import de.amin.bingo.game.board.BingoMaterial;
import de.amin.bingo.game.board.map.BoardRenderer;
import de.amin.bingo.gamestates.GameState;
import de.amin.bingo.gamestates.GameStateManager;
import de.amin.bingo.gamestates.impl.MainState;
import de.amin.bingo.utils.Config;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
//import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.*;
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

    public BingoGame(BingoPlugin plugin, List<UUID> players) {
        this.plugin = plugin;
        this.players = players;
        this.gameID = this.plugin.getGames().size() + 1;
        boards = new HashMap<>();
    }
    public BingoGame(BingoPlugin plugin, List<UUID> players, int gameID, HashMap<Object, BingoBoard> boards, BingoMaterial[] items) {
        this.plugin = plugin;
        this.players = players;
        this.gameID = gameID;
        this.boards = boards;
        this.items = items;
    }
    public void startGame(){
        this.renderer = new BoardRenderer(plugin, this);
        this.gameStateManager = new GameStateManager(plugin, this, renderer);
        gameStateManager.setGameState(GameState.MAIN_STATE);
        ((MainState) gameStateManager.getCurrentGameState()).setTime(Config.GAME_DURATION);
        renderer.updateImages();
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

    public List<UUID> getPlayers() {
        return this.players;
    }

    public void saveGame() {
        try {
            File gamesFile = new File(this.plugin.getDataFolder(), "games.yml");
            FileConfiguration gamesConfig = YamlConfiguration.loadConfiguration(gamesFile);
            ConfigurationSection newGameSection = gamesConfig.createSection(String.valueOf(this.gameID));
            newGameSection.set("players", this.players.stream().map(UUID::toString).collect(Collectors.toList()));
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
