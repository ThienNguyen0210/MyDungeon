package org.ThienNguyen.gui;

import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.File;
import java.util.*;

public class DungeonEditGUI {

    public static final HashMap<UUID, String[]> pendingInputs = new HashMap<>();
    public static final NamespacedKey GUI_ITEM_KEY = new NamespacedKey(Main.getInstance(), "dungeon_edit_key");

    public static void open(Player player, String dungeonId) {
        player.setMetadata("editing_dungeon_id", new FixedMetadataValue(Main.getInstance(), dungeonId));

        File file = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
        if (!file.exists()) {
            player.sendMessage("§c[Lỗi] Không tìm thấy file tại: §f" + file.getPath());
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Inventory inv = Bukkit.createInventory(null, 54, "§0Chỉnh sửa: §8" + dungeonId);

        
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName("§8 ");
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);

        
        for (int i = 0; i < 54; i++) {
            placeFunctionalItem(inv, config, i);
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
    }

    private static void placeFunctionalItem(Inventory inv, YamlConfiguration config, int slot) {
        switch (slot) {
            
            case 10:
                inv.setItem(10, createBtn(Material.NAME_TAG, "§eTên hiển thị",
                        "§fHiện tại: §r" + config.getString("display-name", "Chưa đặt"), "display-name"));
                break;
            case 11:
                inv.setItem(11, createBtn(Material.MAP, "§eWorld Template",
                        "§fThư mục: §b" + config.getString("world-template", "Chưa đặt"), "world-template"));
                break;
            case 12:
                inv.setItem(12, createBtn(Material.PAPER, "§eThông báo đầu tiên",
                        "§fNội dung: §7" + config.getString("message-first", "Chưa đặt"), "message-first"));
                break;

            
            case 13:
                boolean respawn = config.getBoolean("respawn-at-spawn", true);
                inv.setItem(13, createBtn(Material.RED_BED, "§eRespawn tại Spawn",
                        "§fHiện tại: " + (respawn ? "§aBật" : "§cTắt") + "\n§7Click để chuyển đổi", "respawn-at-spawn"));
                break;
            case 19:
                inv.setItem(19, createBtn(Material.CLOCK, "§eThời gian đếm ngược",
                        "§fHiện tại: §a" + config.getInt("countdown-time", 5) + " giây", "countdown-time"));
                break;
            case 20:
                inv.setItem(20, createBtn(Material.OAK_DOOR, "§eThời gian thoát thắng",
                        "§7Sau khi thắng\n§fHiện tại: §a" + config.getInt("quit-time", 5) + " giây", "quit-time"));
                break;
            case 21:
                inv.setItem(21, createBtn(Material.SOUL_CAMPFIRE, "§eThời gian chờ fail",
                        "§7Sau khi thua\n§fHiện tại: §a" + config.getInt("fail-time", 5) + " giây", "fail-time"));
                break;
            case 22:
                inv.setItem(22, createBtn(Material.RECOVERY_COMPASS, "§eThời gian chờ hồi sinh",
                        "§fHiện tại: §a" + config.getInt("respawn-cooldown", 5) + " giây", "respawn-cooldown"));
                break;

            
            case 28:
                
                String lore = "§fTọa độ: §7" + config.getString("spawn-location", "Chưa đặt") +
                        " §6- Cấu trúc: §e world, x, y, z, yaw, pitch";

                inv.setItem(28, createBtn(Material.COMPASS, "§eVị trí Spawn", lore, "spawn-location"));
                break;
            case 29:
                inv.setItem(29, createBtn(Material.IRON_DOOR, "§eVị trí Exit",
                        "§fTọa độ: §7" + config.getString("exit-location", "Chưa đặt"), "exit-location"));
                break;
            case 43:
                boolean allowFly = config.getBoolean("allow-fly", false);
                inv.setItem(43, createBtn(Material.FEATHER, "§eCho phép bay (Fly)",
                        "§fTrạng thái: " + (allowFly ? "§aBật" : "§cTắt") + "\n§7Click để chuyển đổi", "allow-fly"));
                break;
            case 44:
                boolean allowElytra = config.getBoolean("allow-elytra", false);
                inv.setItem(44, createBtn(Material.ELYTRA, "§eCho phép dùng Elytra",
                        "§fTrạng thái: " + (allowElytra ? "§aBật" : "§cTắt") + "\n§7Click để chuyển đổi", "allow-elytra"));
                break;
            
            case 31:
                inv.setItem(31, createBtn(Material.TOTEM_OF_UNDYING, "§eSố mạng tối đa",
                        "§fHiện tại: §c" + config.getInt("max-lives", 5), "max-lives"));
                break;
            case 37:
                String type = config.getString("Settings.Type", "BOTH").toUpperCase();
                String color = type.equals("SOLO") ? "§a" : (type.equals("PARTY") ? "§c" : "§e");
                inv.setItem(37, createBtn(Material.PLAYER_HEAD, "§eLoại phó bản",
                        "§fLoại: " + color + type + "\n§7Click để xoay vòng", "Settings.Type"));
                break;
            case 38:
                inv.setItem(38, createBtn(Material.BEACON, "§eSố người tối thiểu",
                        "§fHiện tại: §a" + config.getInt("Settings.MinPlayers", 1), "Settings.MinPlayers"));
                break;
            case 39:
                inv.setItem(39, createBtn(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, "§eSố người tối đa",
                        "§fHiện tại: §a" + config.getInt("Settings.MaxPlayers", 4), "Settings.MaxPlayers"));
                break;

            
            case 14:
                boolean baseEnabled = config.getBoolean("base-defend.enabled", false);
                inv.setItem(14, createBtn(Material.SHIELD, "§eBảo vệ căn cứ (Base Defend)",
                        "§fTrạng thái: " + (baseEnabled ? "§aBật" : "§cTắt") + "\n§7Click để chuyển đổi", "base-defend.enabled"));
                break;
            case 15:
                inv.setItem(15, createBtn(Material.TOTEM_OF_UNDYING, "§eMáu căn cứ",
                        "§fHiện tại: §c" + config.getInt("base-defend.health", 50) + " ❤", "base-defend.health"));
                break;
            case 16:
                inv.setItem(16, createBtn(Material.NAME_TAG, "§eTên hiển thị căn cứ",
                        "§fHiện tại: §r" + config.getString("base-defend.display-name", "&a&lCĂN CỨ"), "base-defend.display-name"));
                break;
            case 23:
                inv.setItem(23, createBtn(Material.CLOCK, "§eThời gian chờ hồi máu",
                        "§fHiện tại: §a" + config.getInt("base-defend.regen-delay", 10) + " giây", "base-defend.regen-delay"));
                break;
            case 24:
                double regen = config.getDouble("base-defend.regen-amount", 2.0);
                inv.setItem(24, createBtn(Material.POTION, "§eLượng hồi máu mỗi giây",
                        "§fHiện tại: §a" + String.format("%.1f", regen), "base-defend.regen-amount"));
                break;
            case 25:
                inv.setItem(25, createBtn(Material.COMPASS, "§eVị trí tâm căn cứ",
                        "§fHiện tại: §7" + config.getString("base-defend.location", "Chưa đặt"), "base-defend.location"));
                break;
            case 32:
                double radius = config.getDouble("base-defend.radius", 5.0);
                inv.setItem(32, createBtn(Material.SPIDER_EYE, "§eBán kính tấn công",
                        "§fHiện tại: §a" + String.format("%.1f", radius), "base-defend.radius"));
                break;
            case 33:
                String zone = config.getString("base-defend.zone-type", "CIRCLE").toUpperCase();
                inv.setItem(33, createBtn(Material.STRUCTURE_VOID, "§eKiểu vùng tấn công",
                        "§fHiện tại: §b" + zone + "\n§7Click để chuyển đổi (CIRCLE ↔ SQUARE)", "base-defend.zone-type"));
                break;
            case 34:
                inv.setItem(34, createBtn(Material.BOOK, "§eThông báo khi căn cứ phá hủy",
                        "§fHiện tại: §7" + config.getString("base-defend.fail-message", "Chưa đặt"), "base-defend.fail-message"));
                break;

            
            case 46:
                boolean timeEnabled = config.getBoolean("time-limit.enabled", false);
                inv.setItem(46, createBtn(Material.BARRIER, "§eGiới hạn thời gian",
                        "§fTrạng thái: " + (timeEnabled ? "§aBật" : "§cTắt") + "\n§7Click để chuyển đổi", "time-limit.enabled"));
                break;
            case 47:
                inv.setItem(47, createBtn(Material.REPEATER, "§eThời gian tối đa",
                        "§fHiện tại: §a" + config.getInt("time-limit.seconds", 600) + " giây", "time-limit.seconds"));
                break;

            case 40:
                boolean damageSummary = config.getBoolean("send-damage-summary", true);
                inv.setItem(40, createBtn(Material.SKELETON_SKULL, "§eBảng xếp hạng Damage",
                        "§fTrạng thái: " + (damageSummary ? "§aBật" : "§cTắt") + "\n§7Click để chuyển đổi", "send-damage-summary"));
                break;

            case 41:
                inv.setItem(41, createBtn(Material.TNT, "§c§lThiết lập Break Group",
                        "§7(Chế độ đánh dấu khối nổ)\n" +
                                "§f- Click để bật chế độ Edit.\n" +
                                "§f- Phá block để đánh dấu (biến thành TNT).\n" +
                                "§f- Chat §bsave <id> §fđể lưu & hoàn tác.", "EDIT_BREAK_GROUP"));
                break;
            case 42:
                inv.setItem(42, createBtn(Material.BRICKS, "§a§lThiết lập Place Group",
                        "§7(Chế độ xây cấu trúc khởi đầu)\n" +
                                "§f- Click để bật chế độ Edit.\n" +
                                "§f- Đặt block để ghi nhận.\n" +
                                "§f- Chat §bsave <id> §fđể lưu & xóa block.", "EDIT_PLACE_GROUP"));
                break;

            
            case 45:
                inv.setItem(45, createBtn(Material.STONE_STAIRS, "§b§lQUẢN LÝ STAGES",
                        "§cAuthor đang lười:D, không đụng cái này", "OPEN_STAGE_SETTINGS"));
                break;
            case 49:
                inv.setItem(49, createBtn(Material.REDSTONE_BLOCK, "§cĐóng Menu",
                        "§7Thoát khỏi chế độ chỉnh sửa", "CLOSE_GUI"));
                break;
        }
    }

    private static ItemStack createBtn(Material mat, String name, String lore, String configKey) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(name);
        List<String> lores = new ArrayList<>();
        String[] splitLore = lore.split("\n");
        for (String s : splitLore) {
            if (!s.isEmpty()) lores.add(s);
        }

        lores.add("");
        lores.add("§b▶ Click để " + (lore.contains("Click để") ? "chuyển đổi/xoay" : "chỉnh sửa"));
        meta.setLore(lores);

        meta.getPersistentDataContainer().set(GUI_ITEM_KEY, PersistentDataType.STRING, configKey);
        item.setItemMeta(meta);
        return item;
    }
}