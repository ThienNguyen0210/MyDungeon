package org.ThienNguyen;

import org.ThienNguyen.api.PlaceholderAPI;
import org.ThienNguyen.commands.DungeonCommand;
import org.ThienNguyen.commands.DungeonTabCompleter;
import org.ThienNguyen.commands.PartyCMD; 
import org.ThienNguyen.commands.PartyTab;
import org.ThienNguyen.core.*;
import org.ThienNguyen.database.Database;
import org.ThienNguyen.listeners.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.ThienNguyen.listeners.DungeonQuitListener;  
import java.io.File;
import java.io.IOException;
public class Main extends JavaPlugin {
    private FileConfiguration messagesConfig;
    private File messagesFile;
    private static Main instance;
    private DungeonManager dungeonManager;
    private PartyManager partyManager; 
    private BaseManager baseManager;
    private Database database;
    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        createWorldsFolder();
        createExampleDungeon();
        this.database = new Database();
        this.dungeonManager = new DungeonManager();
        this.partyManager = new PartyManager();
        this.baseManager = new BaseManager();
        this.partyManager.loadAllPartiesFromDatabase();
        getServer().getPluginManager().registerEvents(new StaffPenaty(), this);
        getServer().getPluginManager().registerEvents(new DungeonDamageListener(), this);
        getServer().getPluginManager().registerEvents(new DungeonEditListener(), this);
        getServer().getPluginManager().registerEvents(new DungeonJoinListener(), this);
        getServer().getPluginManager().registerEvents(new DungeonQuitListener(), this);
        getServer().getPluginManager().registerEvents(new StageListener(), this);
        getServer().getPluginManager().registerEvents(new DungeonEventListener(), this);
        getServer().getPluginManager().registerEvents(new DungeonRequireListener(), this);
        getServer().getPluginManager().registerEvents(new DungeonMobListener(), this);
        getServer().getPluginManager().registerEvents(new DungeonListener(), this);
        getServer().getPluginManager().registerEvents(new PartyListener(), this);
//        Bukkit.getPluginManager().registerEvents(new FabledExpSharing(), this);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPI(this).register();
        } else {
        }
        
        DungeonCommand dungeonExecutor = new DungeonCommand();
        DungeonTabCompleter dungeonTab = new DungeonTabCompleter();
        if (getCommand("dungeon") != null) {
            getCommand("dungeon").setExecutor(dungeonExecutor);
            getCommand("dungeon").setTabCompleter(dungeonTab);
        }

        
        if (getCommand("party") != null) {
            getCommand("party").setExecutor(new PartyCMD());
            getCommand("party").setTabCompleter(new PartyTab()); 
        }
        String banner =
                "\n§b  ■■      ■■  ■■■■■■\n" +
                        "§b  ■■■    ■■■  ■■   ■■\n" +
                        "§b  ■■ ■  ■ ■■  ■■   ■■  §fMyDungeon Plugin\n" +
                        "§b  ■■  ■■  ■■  ■■   ■■  §7Version: §e1.20 -> 1.21\n" +
                        "§b  ■■      ■■  ■■■■■■   §7Author: §dNhà văn viết code\n" +
                        "§b                       §6Status: §aBản Beta\n";













        
        createMessagesConfig();
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String worldName = p.getWorld().getName();
                    if (worldName.startsWith("temp_")) {
                        String[] parts = worldName.split("_");
                        if (parts.length < 2) continue;

                        String dId = parts[1];
                        DungeonManager dm = Main.getInstance().getDungeonManager();
                        DungeonStage stage = dm.getCurrentStage(p, dId);

                        
                        int maxLives = dm.getMaxLives(dId);
                        int currentDeaths = dm.getDeathCount(p);
                        int livesLeft = Math.max(0, maxLives - currentDeaths);
                        

                        String timeLeft = dm.getTimeLeft(worldName);

                        DungeonScoreboard.updateScoreboard(
                                p,
                                dId,
                                dm.getStageProgress(worldName),
                                (stage != null ? stage.getGoal() : 0),
                                stage,
                                livesLeft,
                                timeLeft
                        );
                    }
                }
            }
        }.runTaskTimer(this, 0L, 4L); 
    }

    
    public static Main getInstance() {
        return instance;
    }

    public DungeonManager getDungeonManager() {
        return dungeonManager;
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    
    private void createMessagesConfig() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            messagesFile.getParentFile().mkdirs();
            if (getResource("messages.yml") != null) {
                saveResource("messages.yml", false);
            } else {
                try { messagesFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
                getLogger().warning("Khong tim thay messages.yml trong Jar! Da tao file trong.");
            }
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reloadMessages() {
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        Bukkit.getLogger().info("[MyDungeon] Da load lai messages.yml!");
    }
    
    public FileConfiguration getDungeonConfig(String dungeonId) {
        File file = new File(getDataFolder(), "Dungeons/" + dungeonId + ".yml");
        return YamlConfiguration.loadConfiguration(file);
    }
    public BaseManager getBaseManager() {
        return baseManager;
    }
    public void reloadAllConfigs() {
        reloadConfig(); 
        createMessagesConfig(); 
        dungeonManager.reloadRequireConfig(); 

        
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().getName().startsWith("temp_")) {
                
            }
        }
        getLogger().info("§a[MyDungeon] Tat ca cau hinh da duoc tai lai!");
    }
    @Override
    public void onDisable() {
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            DungeonScoreboard.removeScoreboard(player);
        }
    }
    public FileConfiguration getPluginConfig() {  
        return getConfig();
    }
    private void createWorldsFolder() {
        File worldsFolder = new File(getDataFolder(), "Worlds");
        if (!worldsFolder.exists()) {
            worldsFolder.mkdirs();
            getLogger().info("§a[MyDungeon] Da tao thu muc Worlds!");
        }
    }
    private void createExampleDungeon() {
        File dungeonsFolder = new File(getDataFolder(), "Dungeons");
        if (!dungeonsFolder.exists()) {
            dungeonsFolder.mkdirs();
        }

        File exampleFile = new File(dungeonsFolder, "example.yml");
        
        if (!exampleFile.exists()) {
            
            if (getResource("Dungeons/example.yml") != null) {
                saveResource("Dungeons/example.yml", false);
                getLogger().info("§a[MyDungeon] Da tao file mau example.yml trong thu muc Dungeons!");
            } else {
                
                if (getResource("example.yml") != null) {
                    saveResource("example.yml", false);
                    
                    File tempFile = new File(getDataFolder(), "example.yml");
                    tempFile.renameTo(exampleFile);
                }
            }
        }
    }
    public Database getDatabase() {
        return database;
    }
}