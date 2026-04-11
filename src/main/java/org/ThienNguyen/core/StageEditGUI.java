package org.ThienNguyen.core;

import org.ThienNguyen.Main;
import org.ThienNguyen.gui.DungeonEditGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StageEditGUI {

    public static void open(Player player, String dungeonId) {
        Inventory inv = Bukkit.createInventory(null, 36, "§0Stages: " + dungeonId);

        File file = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection stages = config.getConfigurationSection("stages");

        if (stages != null) {
            int slot = 10;
            for (String stageKey : stages.getKeys(false)) {
                String type = stages.getString(stageKey + ".type");
                String goal = stages.getString(stageKey + ".goal", "1");

                ItemStack item = new ItemStack(Material.PAPER);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("§eStage " + stageKey);
                List<String> lore = new ArrayList<>();
                lore.add("§fLoại: §b" + type);
                lore.add("§fMục tiêu: §b" + goal);
                lore.add("");
                lore.add("§a▶ Click để sửa Stage này");
                meta.setLore(lore);

                
                meta.getPersistentDataContainer().set(DungeonEditGUI.GUI_ITEM_KEY, PersistentDataType.STRING, "STAGE_EDIT_" + stageKey);
                item.setItemMeta(meta);

                inv.setItem(slot++, item);
                if (slot == 17) slot = 19; 
            }
        }

        
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bMeta = back.getItemMeta();
        bMeta.setDisplayName("§cQuay lại");
        bMeta.getPersistentDataContainer().set(DungeonEditGUI.GUI_ITEM_KEY, PersistentDataType.STRING, "BACK_TO_MAIN");
        back.setItemMeta(bMeta);
        inv.setItem(31, back);

        player.openInventory(inv);
    }
}