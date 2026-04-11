package org.ThienNguyen.commands;

import org.ThienNguyen.Main;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CreateDungeon implements SubCommand {

    @Override
    public void execute(Player player, String[] args) {
        if (!player.hasPermission("mydungeon.setup")) {
            player.sendMessage("§cBạn không có quyền!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cSử dụng: /dungeon create <id>");
            return;
        }

        String id = args[1];
        File dungeonFile = new File(Main.getInstance().getDataFolder(), "Dungeons/" + id + ".yml");

        if (dungeonFile.exists()) {
            player.sendMessage("§cID phó bản §f" + id + " §cđã tồn tại!");
            return;
        }

        try {
            dungeonFile.getParentFile().mkdirs();
            YamlConfiguration config = new YamlConfiguration();

            
            config.options().setHeader(Arrays.asList(
                    "============================================================",
                    "Nhà văn viết code",
                    "Cau hinh pho ban: " + id,
                    "Phien ban nang cap ho tro Base Defend & Multi-Targets",
                    "============================================================"
            ));

            
            config.set("dungeon-id", id);
            config.set("display-name", "&e&lTHỬ THÁCH " + id.toUpperCase());
            config.set("world-template", "dungeonez");

            Location loc = player.getLocation();
            String currentLoc = String.format("%s, %.1f, %.1f, %.1f, %.1f, %.1f",
                    loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

            config.set("spawn-location", currentLoc);
            config.set("exit-location", "world, 11, 120, -180, 0, 0");
            config.set("countdown-time", 5);
            config.set("quit-time", 5);
            config.set("fail-time", 5);
            config.set("respawn-cooldown", 1);
            config.set("max-lives", 2);
            config.set("allow-fly",  true);
            config.set("allow-fly", true);
            config.set("send-damage-summary", true);

            
            config.set("base-defend.enabled", true);
            config.set("base-defend.health", 50.0);
            config.set("base-defend.display-name", "&a&lCĂN CỨ");
            config.set("base-defend.regen-delay", 10);
            config.set("base-defend.regen-amount", 2.0);
            config.set("base-defend.location", currentLoc); 
            config.set("base-defend.radius", 5.0);
            config.set("base-defend.zone-type", "CIRCLE");
            config.set("base-defend.fail-message", "&c&lCăn cứ đã bị san phẳng! Phó bản thất bại.");
            config.set("damage-rewards.enabled", true);


            config.set("damage-rewards.tiers.EASY", Arrays.asList(
                    "50000:10:1"
            ));


            config.set("damage-rewards.tiers.HARD", Arrays.asList(
                    "100000:100:1",
                    "500000:100:2"
            ));


            config.set("damage-rewards.tiers.NIGHTMARE", Arrays.asList(
                    "1000000:50:2",
                    "2000000:10:3"
            ));
            
            config.set("Settings.Type", "BOTH");
            config.set("#Both cho cả 2 , Party chỉ cho party , solo chỉ cho solo, nhớ ghi hoa", "ThienNguyen dev");
            config.set("Settings.MinPlayers", 1);
            config.set("Settings.MaxPlayers", 4);
            config.set("time-limit.enabled", true);
            config.set("time-limit.seconds", 600);

            
            config.set("start-commands", Arrays.asList("[CONSOLE] bc &e%player% &fđã vào hầm ngục &a" + id));
            config.set("quit-commands", Arrays.asList("[OP] bc &e%player% &frời khỏi phó bản."));
            config.set("win-commands", Arrays.asList(
                    "[CONSOLE] eco give %player% 5000",
                    "[CONSOLE] bc &e%player% &fđã phá đảo phó bản &b" + id + "!"
            ));
            config.set("requires", Arrays.asList("2:1"));
            config.set("message-first", "&a&l[!] &fChào mừng đến với " + id);

            
            config.set("break-groups.cong_chinh", Arrays.asList(
                    "10.0, 60.0, 10.0",
                    "10.0, 61.0, 10.0",
                    "10.0, 62.0, 10.0"
            ));

            config.set("place-groups.cau_da", Arrays.asList(
                    "20.0, 60.0, 20.0, STONE",
                    "21.0, 60.0, 20.0, STONE",
                    "22.0, 60.0, 20.0, COBBLESTONE"
            ));
            



            List<Map<String, Object>> breakActions = new ArrayList<>();
            Map<String, Object> breakAction1 = new LinkedHashMap<>();
            breakAction1.put("id", "cong_chinh"); 
            breakAction1.put("delay", 20);        
            breakActions.add(breakAction1);
            config.set("stages.1.break-actions", breakActions);


            List<Map<String, Object>> placeActions = new ArrayList<>();
            Map<String, Object> placeAction1 = new LinkedHashMap<>();
            placeAction1.put("id", "cau_da");     
            placeAction1.put("delay", 40);        
            placeActions.add(placeAction1);
            config.set("stages.1.place-actions", placeActions);
            List<Map<String, Object>> reachTargets = new ArrayList<>();
            Map<String, Object> target1 = new LinkedHashMap<>();
            target1.put("location", "-18, 50, -19");
            target1.put("goal", 1);
            reachTargets.add(target1);
            config.set("stages.1.targets", reachTargets);

            
            config.set("stages.2.type", "KILL_MYTHIC_MOB");
            config.set("stages.2.message", "&fTiêu diệt quái vật đang tấn công căn cứ!");
            config.set("stages.2.ai", true);
            config.set("stages.2.ai-target", "5, 55, 11"); 

            List<Map<String, Object>> killTargets = new ArrayList<>();
            Map<String, Object> mob1 = new LinkedHashMap<>();
            mob1.put("mob", "Zombie_Brute");
            mob1.put("location", "-17, 51, -25");
            mob1.put("goal", 5);
            killTargets.add(mob1);

            config.set("stages.2.targets", killTargets);
            config.set("stages.2.commands", Arrays.asList("[CONSOLE] playsound entity.experience_orb.pickup master %player%"));

            
            config.save(dungeonFile);

            player.sendMessage("§a§l[!] Đã tạo mẫu phó bản đầy đủ: §f" + id);
            player.sendMessage("§7- Hỗ trợ: Base Defend, Multi-Targets, AI Target.");

        } catch (IOException e) {
            player.sendMessage("§cLỗi khi tạo file cấu hình!");
            e.printStackTrace();
        }
    }
}