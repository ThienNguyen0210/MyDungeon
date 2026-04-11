package org.ThienNguyen.commands;

import org.ThienNguyen.Main;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.stream.Stream;

public class EditDungeon implements SubCommand {
    public void execute(Player player, String[] args) {
        if (!player.hasPermission("dungeons.edit")) {
            player.sendMessage("§cBạn không có quyền sử dụng lệnh này!");
            return;
        }
        if (args.length < 2) {
            player.sendMessage("§cSử dụng: /dungeon edit <id>");
            return;
        }

        String id = args[1];
        File file = new File(Main.getInstance().getDataFolder(), "Dungeons/" + id + ".yml");
        if (!file.exists()) {
            player.sendMessage("§cID phó bản không tồn tại!");
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String templateName = config.getString("world-template", "world");

        
        
        String spawnLocStr = config.getString("spawn-location");

        File sourceFolder = new File(Main.getInstance().getDataFolder(), "Worlds/" + templateName);
        File targetFolder = new File(Bukkit.getWorldContainer(), templateName);

        if (!sourceFolder.exists()) {
            player.sendMessage("§cKhông tìm thấy folder world tại: " + sourceFolder.getPath());
            return;
        }

        player.sendMessage("§e§l[!] §fĐang chuẩn bị dữ liệu World...");
        player.setMetadata("editing_dungeon_id", new org.bukkit.metadata.FixedMetadataValue(Main.getInstance(), id));

        World world = Bukkit.getWorld(templateName);

        if (world == null) {
            try {
                if (targetFolder.exists()) {
                    deleteDirectory(targetFolder.toPath());
                }
                copyFolder(sourceFolder.toPath(), targetFolder.toPath());
                File uidFile = new File(targetFolder, "uid.dat");
                if (uidFile.exists()) uidFile.delete();

                WorldCreator creator = new WorldCreator(templateName);
                world = creator.createWorld();
            } catch (IOException e) {
                player.sendMessage("§cLỗi khi sao chép dữ liệu World: " + e.getMessage());
                return;
            }
        }

        if (world != null) {
            world.setGameRule(GameRule.MOB_GRIEFING, false);
            world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            world.setAutoSave(true);

            
            Location tpLocation = null;
            if (spawnLocStr != null && !spawnLocStr.isEmpty()) {
                try {
                    
                    String[] parts = spawnLocStr.split(",");
                    if (parts.length >= 4) {
                        double x = Double.parseDouble(parts[1].trim());
                        double y = Double.parseDouble(parts[2].trim());
                        double z = Double.parseDouble(parts[3].trim());
                        
                        tpLocation = new Location(world, x, y, z);
                    }
                } catch (Exception e) {
                    player.sendMessage("§e[!] §7Tọa độ spawn-location lỗi, dùng vị trí mặc định.");
                }
            }

            
            if (tpLocation == null) {
                tpLocation = world.getSpawnLocation();
            }

            player.teleport(tpLocation);
            player.setGameMode(GameMode.CREATIVE);
            player.getInventory().addItem(getEditTool());

            player.sendMessage("");
            player.sendMessage("§a§l▶ CHẾ ĐỘ CHỈNH SỬA PHÓ BẢN");
            player.sendMessage("§fVị trí: §b" + tpLocation.getBlockX() + ", " + tpLocation.getBlockY() + ", " + tpLocation.getBlockZ());
            player.sendMessage("§e§l[!] §6Lưu ý: §fSau khi xây xong, dùng §b/dungeon save " + id + " §fđể lưu.");
        }
    }

    private ItemStack getEditTool() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lDungeon Edit Tool");
            meta.setLore(Arrays.asList(
                    "§7Chuột phải vào Block để lấy tọa độ",
                    "§7Tọa độ sẽ hiện trong Chat để Copy"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    

    private void copyFolder(Path src, Path dest) throws IOException {
        try (Stream<Path> stream = Files.walk(src)) {
            stream.forEach(source -> {
                try {
                    Files.copy(source, dest.resolve(src.relativize(source)), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {}
            });
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (Stream<Path> stream = Files.walk(path)) {
            stream.sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}