package org.ThienNguyen.gui;

import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DungeonRequireGUI {

    public static void open(Player player, int page) {
        
        String title = "§0Require Manager - Trang " + page;
        Inventory inv = Bukkit.createInventory(null, 54, title);

        Map<Integer, ItemStack> allItems = Main.getInstance().getDungeonManager().getRequireItems();

        
        int startId = (page - 1) * 45 + 1;
        int endId = page * 45;

        for (int id = startId; id <= endId; id++) {
            if (allItems.containsKey(id)) {
                ItemStack item = allItems.get(id).clone();
                ItemMeta meta = item.getItemMeta();
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add("");
                lore.add("§7ID: §e" + id);
                lore.add("§cShift + Left-Click để xóa");
                meta.setLore(lore);
                item.setItemMeta(meta);

                
                inv.setItem(id - startId, item);
            }
        }

        

        
        if (page > 1) {
            inv.setItem(45, createItem(Material.ARROW, "§e« Trang trước", List.of("§7Quay lại trang " + (page - 1))));
        }

        
        inv.setItem(49, createItem(Material.NETHER_STAR, "§a§lTHÊM VẬT PHẨM", List.of("§7Thêm item trên tay vào Database")));

        
        
        if (allItems.size() > endId) {
            inv.setItem(52, createItem(Material.ARROW, "§eTrang sau »", List.of("§7Tiến tới trang " + (page + 1))));
        }

        player.openInventory(inv);
    }

    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}