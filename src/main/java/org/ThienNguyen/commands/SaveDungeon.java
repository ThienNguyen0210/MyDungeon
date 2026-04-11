package org.ThienNguyen.commands;

import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.stream.Stream;

public class SaveDungeon implements SubCommand {

    @Override
    public void execute(Player player, String[] args) {
        if (!player.hasPermission("dungeons.save")) {
            player.sendMessage("§cBạn không có quyền sử dụng lệnh này!");
            return;
        }
        if (args.length < 2) {
            player.sendMessage("§cSử dụng: /dungeon save <id>");
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

        World world = Bukkit.getWorld(templateName);
        if (world == null) {
            player.sendMessage("§cWorld '" + templateName + "' hiện không được load. Bạn phải dùng /dungeon edit trước!");
            return;
        }

        player.sendMessage("§e§l[!] §fĐang tiến hành lưu và đóng World chỉnh sửa...");

        
        Location mainSpawn = Bukkit.getWorlds().get(0).getSpawnLocation();
        for (Player p : world.getPlayers()) {
            p.teleport(mainSpawn);
            p.sendMessage("§eWorld đang được lưu và đóng lại...");
        }

        
        world.save();

        File serverWorldFolder = new File(Bukkit.getWorldContainer(), templateName);
        File pluginWorldFolder = new File(Main.getInstance().getDataFolder(), "Worlds/" + templateName);

        
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                
                if (pluginWorldFolder.exists()) {
                    deleteDirectory(pluginWorldFolder.toPath());
                }
                copyFolder(serverWorldFolder.toPath(), pluginWorldFolder.toPath());

                
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    
                    Bukkit.unloadWorld(world, true);

                    
                    try {
                        deleteDirectory(serverWorldFolder.toPath());

                        
                        if (player.hasMetadata("editing_dungeon_id")) {
                            player.removeMetadata("editing_dungeon_id", Main.getInstance());
                        }

                        player.sendMessage("§a§l▶ ĐÃ LƯU & DỌN DẸP XONG");
                        player.sendMessage("§fMetadata đã được xóa. Gậy Edit Tool sẽ không còn hiệu lực cho ID này.");
                        player.sendMessage("§fDữ liệu đã lưu vào: §7" + pluginWorldFolder.getPath());
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    } catch (IOException e) {
                        player.sendMessage("§6[!] §7Dữ liệu đã lưu nhưng không thể xóa folder tạm.");
                    }
                });

            } catch (IOException e) {
                player.sendMessage("§cLỗi nghiêm trọng khi xử lý dữ liệu: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void copyFolder(Path src, Path dest) throws IOException {
        if (!Files.exists(dest)) Files.createDirectories(dest);
        try (Stream<Path> stream = Files.walk(src)) {
            stream.forEach(source -> {
                try {
                    String fileName = source.getFileName().toString();
                    if (fileName.equals("session.lock") || fileName.equals("uid.dat")) return;

                    Path target = dest.resolve(src.relativize(source));
                    if (!Files.isDirectory(source)) {
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    } else if (!Files.exists(target)) {
                        Files.createDirectories(target);
                    }
                } catch (IOException ignored) {}
            });
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (Stream<Path> stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}