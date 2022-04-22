package de.amin.bingo.game;

import de.amin.bingo.BingoPlugin;
import de.amin.bingo.game.board.BingoBoard;
import de.amin.bingo.game.board.BingoMaterial;
import de.amin.bingo.game.board.map.BoardRenderer;
import de.amin.bingo.utils.Config;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.util.*;

public class BingoGame {

    private BingoPlugin plugin;
    private HashMap<Object, BingoBoard> boards;
    BingoMaterial[] items = new BingoMaterial[Config.BOARD_SIZE];
    private BoardRenderer renderer;
    private final List<UUID> players;
    private ItemStack boardItem;
    public BingoGame(BingoPlugin plugin, List<UUID> players) {
        this.plugin = plugin;
        this.players = players;
        boards = new HashMap<>();
    }
    public void setBoardItem(ItemStack boardItem) {
        this.boardItem = boardItem;
    }
    public ItemStack getBoardItem() {
        return this.boardItem;
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
//        boards.put(team, new BingoBoard(items));

//        .getTeams().forEach(team -> {
//        });

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
        for (UUID player: players) {
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

    public BingoBoard getBoard(Player player) {
        return boards.get(player.getUniqueId());
    }

    public BingoMaterial[] getItems() {
        return items;
    }

    public BingoMaterial getRandomMaterial() {
        return BingoMaterial.values()[new Random().nextInt(BingoMaterial.values().length)];
    }

    public List<UUID> getPlayers() {
        return this.players;
    }
}
