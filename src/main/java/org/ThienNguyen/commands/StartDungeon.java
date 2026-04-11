package org.ThienNguyen.commands;

import org.ThienNguyen.Main;
import org.ThienNguyen.core.DungeonManager;
import org.ThienNguyen.core.DungeonStage;
import org.ThienNguyen.core.Party;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class StartDungeon implements SubCommand {

    @Override
    public void execute(Player player, String[] args) {
        FileConfiguration msgConfig = Main.getInstance().getMessagesConfig();

        
        if (args.length < 2) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    msgConfig.getString("dungeon.start.usage", "§cSử dụng: /dungeon start <id> [difficulty]")));
            return;
        }

        String id = args[1];

        
        String selectedDifficulty = (args.length >= 3) ? args[2].toUpperCase() : null;

        File file = new File(Main.getInstance().getDataFolder(), "Dungeons/" + id + ".yml");
        if (!file.exists()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    msgConfig.getString("dungeon.start.not-found", "§cKhông tìm thấy dữ liệu phó bản này!")));
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        
        String difficulty;
        if (selectedDifficulty == null) {
            difficulty = config.getString("difficulty", "EASY").toUpperCase();
        } else {
            difficulty = selectedDifficulty;
        }

        
        if (!Main.getInstance().getConfig().contains("difficulty-settings." + difficulty)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "§cĐộ khó §f" + difficulty + " §ckhông hợp lệ! Các mức: §eEASY§7, §aNORMAL§7, §cHARD§7, §4NIGHTMARE"));
            return;
        }

        final String finalDifficulty = difficulty; 

        
        String type = config.getString("Settings.Type", "SOLO").toUpperCase();
        int minPlayers = config.getInt("Settings.MinPlayers", 1);
        int maxPlayers = config.getInt("Settings.MaxPlayers", 4);
        Party party = Main.getInstance().getPartyManager().getParty(player);

        List<Player> participants = new ArrayList<>();
        if (type.equals("SOLO")) {
            if (party != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        msgConfig.getString("dungeon.start.solo-only", "§cPhó bản đơn, vui lòng rời tổ đội trước!")));
                return;
            }
            participants.add(player);
        }
        else if (type.equals("PARTY")) {
            if (party == null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        msgConfig.getString("dungeon.start.require-party", "§cPhó bản này yêu cầu phải có tổ đội!")));
                return;
            }
            if (!party.getLeader().equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        msgConfig.getString("dungeon.start.not-leader", "§cChỉ trưởng nhóm mới có thể bắt đầu phó bản!")));
                return;
            }
            int memberSize = party.getMembers().size();
            if (memberSize < minPlayers || memberSize > maxPlayers) {
                String msg = msgConfig.getString("dungeon.start.invalid-size", "§cSố lượng thành viên không hợp lệ (Cần %min%-%max% người)!");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        msg.replace("%min%", String.valueOf(minPlayers)).replace("%max%", String.valueOf(maxPlayers))));
                return;
            }
            if (!party.isAllReady()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        msgConfig.getString("dungeon.start.not-ready", "§cMột số thành viên trong đội chưa sẵn sàng!")));
                return;
            }
            for (UUID uuid : party.getMembers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            msgConfig.getString("dungeon.start.member-offline", "§cCó thành viên trong đội đang ngoại tuyến!")));
                    return;
                }
                participants.add(p);
            }
        }
        else if (type.equals("BOTH")) {
            if (party != null) {
                
                if (!party.getLeader().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            msgConfig.getString("dungeon.start.not-leader", "§cChỉ trưởng nhóm mới có thể bắt đầu phó bản!")));
                    return;
                }
                int memberSize = party.getMembers().size();
                if (memberSize < minPlayers || memberSize > maxPlayers) {
                    String msg = msgConfig.getString("dungeon.start.invalid-size", "§cSố lượng thành viên không hợp lệ (Cần %min%-%max% người)!");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            msg.replace("%min%", String.valueOf(minPlayers)).replace("%max%", String.valueOf(maxPlayers))));
                    return;
                }
                if (!party.isAllReady()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            msgConfig.getString("dungeon.start.not-ready", "§cMột số thành viên trong đội chưa sẵn sàng!")));
                    return;
                }
                for (UUID uuid : party.getMembers()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null || !p.isOnline()) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                msgConfig.getString("dungeon.start.member-offline", "§cCó thành viên trong đội đang ngoại tuyến!")));
                        return;
                    }
                    participants.add(p);
                }
            } else {
                
                participants.add(player);
            }
        }
        
        List<String> conditions = config.getStringList("Conditions");
        if (!conditions.isEmpty()) {
            for (Player p : participants) {
                if (!checkConditions(p, conditions)) {
                    String msg = msgConfig.getString("dungeon.start.condition-failed", "§cNgười chơi %player% không đủ điều kiện để vào phó bản!");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            msg.replace("%player%", p.getName())));
                    return; 
                }
            }
        }
        DungeonManager dm = Main.getInstance().getDungeonManager();

        
        for (Player p : participants) {
            if (dm.getPlayerState(p) != DungeonManager.DungeonState.NONE) {
                String msg = msgConfig.getString("dungeon.start.already-in-dungeon", "§c%player% đang trong phó bản khác!");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        msg.replace("%player%", p.getName())));
                return;
            }
        }

        
        List<String> requireList = config.getStringList("requires");
        if (!requireList.isEmpty()) {
            Map<Integer, Integer> neededItems = new HashMap<>();
            for (String req : requireList) {
                String[] split = req.split(":");
                if (split.length == 2) {
                    try {
                        neededItems.put(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
                    } catch (Exception ignored) {}
                }
            }

            for (Map.Entry<Integer, Integer> entry : neededItems.entrySet()) {
                ItemStack template = dm.getRequireItems().get(entry.getKey());
                if (template == null) continue;
                if (!hasEnough(player, template, entry.getValue())) {
                    String name = template.hasItemMeta() && template.getItemMeta().hasDisplayName()
                            ? template.getItemMeta().getDisplayName() : template.getType().name();

                    String msg = msgConfig.getString("dungeon.start.not-enough-item", "§cKhông đủ: %item% §7(x%amount%)");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            msg.replace("%item%", name).replace("%amount%", String.valueOf(entry.getValue()))));
                    return;
                }
            }

            
            for (Map.Entry<Integer, Integer> entry : neededItems.entrySet()) {
                ItemStack template = dm.getRequireItems().get(entry.getKey());
                if (template != null) removeItems(player, template, entry.getValue());
            }
        }

        
        String templateWorldName = config.getString("world-template");
        if (templateWorldName == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    msgConfig.getString("dungeon.start.missing-template", "§cThiếu 'world-template' trong config!")));
            return;
        }

        File sourceFolder = new File(Main.getInstance().getDataFolder(), "Worlds/" + templateWorldName);
        if (!sourceFolder.exists() || !sourceFolder.isDirectory()) {
            String msg = msgConfig.getString("dungeon.start.missing-map", "§cKhông tìm thấy map gốc: Worlds/%map%");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    msg.replace("%map%", templateWorldName)));
            return;
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String tempWorldName = "temp_" + id + "_" + player.getName() + "_" + timestamp;

        
        for (Player p : participants) {
            dm.setPlayerState(p, DungeonManager.DungeonState.ACTIVE);
        }

        int countdownSeconds = config.getInt("countdown-time", 5);
        String title = ChatColor.translateAlternateColorCodes('&', msgConfig.getString("dungeon.start.title", "&aBẮT ĐẦU"));
        String subtitleTemplate = ChatColor.translateAlternateColorCodes('&', msgConfig.getString("dungeon.start.subtitle", "&fVào sau %time%s"));

        final World[] loadedWorld = new World[1];

        
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                File targetFolder = new File(Bukkit.getWorldContainer(), tempWorldName);
                if (targetFolder.exists()) {
                    deleteDirectory(targetFolder.toPath());
                }
                copyFolder(sourceFolder.toPath(), targetFolder.toPath());
                Files.deleteIfExists(targetFolder.toPath().resolve("uid.dat"));

                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    WorldCreator creator = new WorldCreator(tempWorldName);
                    World world = creator.createWorld();
                    if (world != null) {
                        world.setAutoSave(false);
                        world.setGameRule(GameRule.KEEP_INVENTORY, true);
                        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
                        loadedWorld[0] = world;
                    } else {
                        loadedWorld[0] = null;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                loadedWorld[0] = null;
                Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                        participants.forEach(p -> dm.setPlayerState(p, DungeonManager.DungeonState.NONE)));
            }
        });

        
        new BukkitRunnable() {
            int totalTicks = countdownSeconds * 20;
            int currentTick = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    participants.forEach(Player::resetTitle);
                    cancel();
                    return;
                }

                if (currentTick >= totalTicks) {
                    if (loadedWorld[0] == null) {
                        String errorMsg = msgConfig.getString("dungeon.start.load-error", "§cLỗi: Không thể tải phó bản. Vui lòng thử lại!");
                        for (Player p : participants) {
                            p.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMsg));
                            dm.setPlayerState(p, DungeonManager.DungeonState.NONE);
                            p.resetTitle();
                        }
                        cancel();
                        return;
                    }

                    World tempWorld = loadedWorld[0];
                    String worldName = tempWorld.getName();

                    
                    dm.setWorldDifficulty(worldName, finalDifficulty);

                    
                    String diffDisplay = Main.getInstance().getConfig()
                            .getString("difficulty-settings." + finalDifficulty + ".display", finalDifficulty);
                    dm.setDungeonLeader(worldName, player.getUniqueId());

                    Set<UUID> memberUuids = new HashSet<>();
                    for (Player p : participants) {
                        memberUuids.add(p.getUniqueId());
                    }
                    dm.setDungeonPartyMembers(worldName, memberUuids);

                    String spawnStr = config.getString("spawn-location");
                    Location teleLoc = (spawnStr != null && !spawnStr.isEmpty())
                            ? dm.parseLocation(spawnStr)
                            : tempWorld.getSpawnLocation();

                    if (teleLoc != null) {
                        teleLoc.setWorld(tempWorld);
                    } else {
                        teleLoc = tempWorld.getSpawnLocation();
                    }

                    for (Player p : participants) {
                        if (!p.isOnline()) continue;
                        dm.addPlayerToDungeon(worldName, p.getUniqueId());
                        p.teleport(teleLoc);
                        p.setGameMode(GameMode.SURVIVAL);
                        p.resetTitle();

                        dm.startFirstStage(p);

                        DungeonStage firstStage = dm.getCurrentStage(p, id);

                        int maxLives = config.getInt("max-lives", 3);
                        String timeLeft = dm.getTimeLeft(worldName);

                        org.ThienNguyen.core.DungeonScoreboard.updateScoreboard(
                                p,
                                id,
                                0,
                                (firstStage != null ? firstStage.getGoal() : 0),
                                firstStage,
                                maxLives,
                                timeLeft
                        );
                    }
                    cancel();
                    return;
                }

                int secondsLeft = (totalTicks - currentTick - 1) / 20 + 1;
                String subtitle = subtitleTemplate.replace("%time%", String.valueOf(secondsLeft));

                for (Player p : participants) {
                    if (p.isOnline()) {
                        p.sendTitle(title, subtitle, 0, 22, 0);
                        if (currentTick % 20 == 0) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.2f);
                        }
                    }
                }
                currentTick++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    
    
    
    private void copyFolder(Path src, Path dest) throws java.io.IOException {
        try (java.util.stream.Stream<Path> stream = Files.walk(src)) {
            stream.forEach(source -> {
                try {
                    Files.copy(source, dest.resolve(src.relativize(source)), StandardCopyOption.REPLACE_EXISTING);
                } catch (java.io.IOException ignored) {}
            });
        }
    }

    private void deleteDirectory(Path path) throws java.io.IOException {
        if (!Files.exists(path)) return;
        try (java.util.stream.Stream<Path> stream = Files.walk(path)) {
            stream.sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(java.io.File::delete);
        }
    }
    private boolean checkConditions(Player player, List<String> conditions) {
        if (conditions == null || conditions.isEmpty()) return true;

        for (String condition : conditions) {
            
            String[] operators = {">=", "<=", ">", "<", "="};
            String selectedOp = null;
            String[] parts = null;

            for (String op : operators) {
                if (condition.contains(op)) {
                    selectedOp = op;
                    parts = condition.split(op);
                    break;
                }
            }

            if (parts == null || parts.length < 2) continue;

            
            String leftSide = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, parts[0].trim());
            String rightSide = parts[1].trim();

            try {
                double leftValue = Double.parseDouble(leftSide);
                double rightValue = Double.parseDouble(rightSide);

                switch (selectedOp) {
                    case ">=": if (!(leftValue >= rightValue)) return false; break;
                    case "<=": if (!(leftValue <= rightValue)) return false; break;
                    case ">":  if (!(leftValue > rightValue))  return false; break;
                    case "<":  if (!(leftValue < rightValue))  return false; break;
                    case "=":  if (!(leftValue == rightValue)) return false; break;
                }
            } catch (NumberFormatException e) {
                
                if (selectedOp.equals("=") && !leftSide.equalsIgnoreCase(rightSide)) return false;
            }
        }
        return true;
    }
    private boolean hasEnough(Player player, ItemStack template, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(template)) {
                count += item.getAmount();
            }
        }
        return count >= amount;
    }

    private void removeItems(Player player, ItemStack template, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.isSimilar(template)) {
                if (item.getAmount() > remaining) {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                } else {
                    remaining -= item.getAmount();
                    contents[i] = null;
                }
            }
        }
        player.getInventory().setContents(contents);
    }
}