package org.ThienNguyen.listeners;

import org.ThienNguyen.Main;
import org.ThienNguyen.gui.DungeonEditGUI;
import org.ThienNguyen.core.DungeonManager;
import org.ThienNguyen.core.StageEditGUI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class DungeonEditListener implements Listener {

    
    public static final HashMap<UUID, HashMap<Location, BlockData>> breakTemp = new HashMap<>();
    
    public static final HashMap<UUID, List<Location>> placeTemp = new HashMap<>();

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        if (title.startsWith("§0Chỉnh sửa:") || title.startsWith("§0Stages:")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;

            Player player = (Player) event.getWhoClicked();
            if (!player.hasMetadata("editing_dungeon_id")) return;
            String dungeonId = player.getMetadata("editing_dungeon_id").get(0).asString();

            String configKey = event.getCurrentItem().getItemMeta()
                    .getPersistentDataContainer().get(DungeonEditGUI.GUI_ITEM_KEY, PersistentDataType.STRING);

            if (configKey == null) return;

            
            if (configKey.equals("Settings.Type")) {
                cycleDungeonType(player, dungeonId);
                return;
            }
            
            if (configKey.equals("respawn-at-spawn")) {
                toggleBooleanConfig(player, dungeonId, "respawn-at-spawn", null);
                return;
            }


            if (configKey.equals("base-defend.enabled")) {
                toggleBooleanConfig(player, dungeonId, "base-defend.enabled", null);
                return;
            }


            if (configKey.equals("send-damage-summary")) {
                toggleBooleanConfig(player, dungeonId, "send-damage-summary", null);
                return;
            }
            if (configKey.equals("allow-fly")) {
                toggleBooleanConfig(player, dungeonId, "allow-fly", null);
                return;
            }

            if (configKey.equals("allow-elytra")) {
                toggleBooleanConfig(player, dungeonId, "allow-elytra", null);
                return;
            }

            if (configKey.equals("damage-rewards.enabled")) {
                toggleBooleanConfig(player, dungeonId, "damage-rewards.enabled", null);
                return;
            }
            if (configKey.equals("time-limit.enabled")) {
                toggleTimeLimit(player, dungeonId);
                return;
            }

            

            if (configKey.equals("EDIT_BREAK_GROUP")) {
                player.closeInventory();
                
                player.setMetadata("editing_break_mode", new FixedMetadataValue(Main.getInstance(), true));

                player.sendMessage("");
                player.sendMessage("§c§l[EDIT MODE] §fĐã bật chế độ §e§lĐÁNH DẤU KHỐI NỔ");
                player.sendMessage("§7- Hãy phá các block bạn muốn quái/boss làm nổ.");
                player.sendMessage("§7- Chat §bsave <id> §7để lưu Group.");
                player.sendMessage("§7- Chat §bcancel §7để hủy bỏ.");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                return;
            }

            if (configKey.equals("EDIT_PLACE_GROUP")) {
                player.closeInventory();
                
                player.setMetadata("editing_place_mode", new FixedMetadataValue(Main.getInstance(), true));

                player.sendMessage("");
                player.sendMessage("§a§l[EDIT MODE] §fĐã bật chế độ §e§lTHIẾT KẾ CẤU TRÚC");
                player.sendMessage("§7- Hãy đặt các block bạn muốn xuất hiện khi bắt đầu Stage.");
                player.sendMessage("§7- Chat §bsave <id> §7để lưu Group.");
                player.sendMessage("§7- Chat §bcancel §7để hủy bỏ.");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                return;
            }

            
            if (configKey.equals("OPEN_STAGE_SETTINGS")) { StageEditGUI.open(player, dungeonId); return; }
            if (configKey.equals("CLOSE_GUI")) { player.closeInventory(); return; }

            
            DungeonEditGUI.pendingInputs.put(player.getUniqueId(), new String[]{dungeonId, configKey});
            player.closeInventory();
            player.sendMessage("§e§l[EDIT] §fVui lòng nhập giá trị cho §b" + configKey);
        }
    }

    /**
     * Hàm phụ trợ để xoay vòng Type phó bản
     */
    private void cycleDungeonType(Player player, String dungeonId) {
        File file = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String current = config.getString("Settings.Type", "BOTH").toUpperCase();
        String next;

        
        if (current.equals("BOTH")) next = "SOLO";
        else if (current.equals("SOLO")) next = "PARTY";
        else next = "BOTH";

        config.set("Settings.Type", next);

        try {
            config.save(file);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
            
            DungeonEditGUI.open(player, dungeonId);
        } catch (IOException e) {
            player.sendMessage("§cLỗi khi lưu cấu hình Type!");
        }
    }

    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreakEdit(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata("editing_break_mode")) {
            event.setCancelled(true);
            Block block = event.getBlock();
            Location loc = block.getLocation();
            UUID uuid = player.getUniqueId();

            breakTemp.putIfAbsent(uuid, new HashMap<>());
            if (!breakTemp.get(uuid).containsKey(loc)) {
                breakTemp.get(uuid).put(loc, block.getBlockData());
                block.setType(Material.TNT);
                player.sendMessage("§7[+] Đã đánh dấu block phá tại: §e" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
            }
        }
    }
    /**
     * Toggle giá trị boolean cho bất kỳ path nào và refresh GUI
     */
    private void toggleBooleanConfig(Player player, String dungeonId, String configPath, String sound) {
        File file = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        boolean current = config.getBoolean(configPath, false);
        config.set(configPath, !current);

        try {
            config.save(file);
            player.playSound(player.getLocation(), sound != null ? Sound.valueOf(sound) : Sound.BLOCK_LEVER_CLICK, 1f, 1.5f);
            DungeonEditGUI.open(player, dungeonId);
        } catch (IOException e) {
            player.sendMessage("§cLỗi khi lưu cấu hình " + configPath + "!");
        }
    }
    private void toggleTimeLimit(Player player, String dungeonId) {
        File file = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        
        boolean current = config.getBoolean("time-limit.enabled", false);

        
        config.set("time-limit.enabled", !current);

        try {
            config.save(file);
            player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1.5f); 

            
            DungeonEditGUI.open(player, dungeonId);
        } catch (IOException e) {
            player.sendMessage("§cLỗi khi lưu cấu hình Time Limit!");
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlaceEdit(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata("editing_place_mode")) {
            Location loc = event.getBlock().getLocation();
            UUID uuid = player.getUniqueId();
            placeTemp.putIfAbsent(uuid, new ArrayList<>());
            placeTemp.get(uuid).add(loc);
            player.sendMessage("§7[+] Đã ghi nhận block đặt: §e" + event.getBlock().getType().name());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChatInput(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        UUID uuid = player.getUniqueId();

        
        boolean hasBreakMeta = player.hasMetadata("editing_break_mode");
        boolean hasPlaceMeta = player.hasMetadata("editing_place_mode");

        
        if (message.startsWith("save ") || message.startsWith("saveplace ")) {
            if (!hasBreakMeta && !hasPlaceMeta) return; 

            event.setCancelled(true);
            String[] args = message.split(" ");
            if (args.length < 2) {
                player.sendMessage("§cVui lòng nhập ID group! Ví dụ: save test");
                return;
            }

            String groupId = args[1];
            String dungeonId = player.getMetadata("editing_dungeon_id").get(0).asString();
            List<String> locStrings = new ArrayList<>();

            
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                
                if (hasBreakMeta) {
                    
                    HashMap<Location, BlockData> blocks = breakTemp.get(uuid);
                    if (blocks != null && !blocks.isEmpty()) {
                        for (Location loc : blocks.keySet()) {
                            
                            locStrings.add(String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ()));
                            
                            loc.getBlock().setBlockData(blocks.get(loc));
                        }
                    }
                } else if (hasPlaceMeta) {
                    
                    List<Location> locations = placeTemp.get(uuid);
                    if (locations != null && !locations.isEmpty()) {
                        for (Location loc : locations) {
                            Block b = loc.getBlock();
                            if (b.getType() != Material.AIR) {
                                
                                locStrings.add(String.format("%.1f, %.1f, %.1f, %s",
                                        loc.getX(), loc.getY(), loc.getZ(), b.getType().name()));
                                
                                b.setType(Material.AIR);
                            }
                        }
                    }
                }

                
                if (!locStrings.isEmpty()) {
                    String path = hasBreakMeta ? "break-groups" : "place-groups";
                    Main.getInstance().getDungeonManager().saveBlockGroup(dungeonId, path, groupId, locStrings);

                    player.sendMessage("§a§l[SUCCESS] §fĐã lưu §e" + locStrings.size() + " §fblock vào §b" + path + "." + groupId);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                } else {
                    player.sendMessage("§c[!] Không tìm thấy dữ liệu block để lưu!");
                }

                
                player.removeMetadata("editing_break_mode", Main.getInstance());
                player.removeMetadata("editing_place_mode", Main.getInstance());
                breakTemp.remove(uuid);
                placeTemp.remove(uuid);
            });
            return;
        }

        
        if (message.equalsIgnoreCase("cancel")) {
            if (hasBreakMeta || hasPlaceMeta || DungeonEditGUI.pendingInputs.containsKey(uuid)) {
                event.setCancelled(true);
                DungeonEditGUI.pendingInputs.remove(uuid);

                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    if (breakTemp.containsKey(uuid)) {
                        breakTemp.get(uuid).forEach((loc, data) -> loc.getBlock().setBlockData(data));
                    }
                    if (placeTemp.containsKey(uuid)) {
                        placeTemp.get(uuid).forEach(loc -> loc.getBlock().setType(Material.AIR));
                    }
                    player.removeMetadata("editing_break_mode", Main.getInstance());
                    player.removeMetadata("editing_place_mode", Main.getInstance());
                    breakTemp.remove(uuid);
                    placeTemp.remove(uuid);
                    player.sendMessage("§c§l[!] §fĐã hủy và dọn dẹp hiện trường.");
                });
                return;
            }
        }

        
        if (DungeonEditGUI.pendingInputs.containsKey(uuid)) {
            event.setCancelled(true);
            String[] data = DungeonEditGUI.pendingInputs.remove(uuid);
            String dId = data[0];
            String key = data[1];

            File file = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dId + ".yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            
            if (isNumber(message)) {
                config.set(key, Integer.parseInt(message));
            } else if (message.equalsIgnoreCase("true") || message.equalsIgnoreCase("false")) {
                config.set(key, Boolean.parseBoolean(message));
            } else {
                config.set(key, message);
            }

            try {
                config.save(file);
                player.sendMessage("§a§l[SUCCESS] §fĐã cập nhật: §e" + key + " §fthành §b" + message);
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> DungeonEditGUI.open(player, dId));
            } catch (IOException e) {
                player.sendMessage("§cLỗi khi lưu file cấu hình!");
            }
        }
    }

    private boolean isNumber(String s) { try { Integer.parseInt(s); return true; } catch (Exception e) { return false; } }
}