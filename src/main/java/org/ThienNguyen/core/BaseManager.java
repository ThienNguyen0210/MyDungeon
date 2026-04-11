package org.ThienNguyen.core;

import org.ThienNguyen.Main;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class BaseManager {
    private final Map<String, Double> currentHealth = new HashMap<>();
    private final Map<String, Double> maxHealth = new HashMap<>();
    private final Map<String, Location> baseLocations = new HashMap<>();
    private final Map<String, TextDisplay> healthDisplays = new HashMap<>();
    private final Map<String, Double> baseRadius = new HashMap<>();
    private final Map<String, String> zoneTypes = new HashMap<>();

    
    private final Map<String, Long> lastDamageTime = new HashMap<>();
    private final Map<String, Double> regenAmountMap = new HashMap<>();
    private final Map<String, Integer> regenDelayMap = new HashMap<>();
    private final Map<String, Boolean> isExploding = new HashMap<>(); 
    private BukkitTask regenTask;

    public BaseManager() {
        startRegenTask();
    }

    public void setupBase(String worldName, String dungeonId) {
        File file = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (config.getBoolean("base-defend.enabled", false)) {
            double hp = config.getDouble("base-defend.health", 100.0);
            String rawLoc = config.getString("base-defend.location");
            double radius = config.getDouble("base-defend.radius", 5.0);
            String type = config.getString("base-defend.zone-type", "CIRCLE").toUpperCase();

            double regenAmount = config.getDouble("base-defend.regen-amount", 2.0);
            int regenDelay = config.getInt("base-defend.regen-delay", 10);
            String displayName = config.getString("base-defend.display-name", "&e&lCĂN CỨ");

            if (rawLoc != null) {
                Location loc = Main.getInstance().getDungeonManager().parseLocation(rawLoc);
                World world = Bukkit.getWorld(worldName);
                if (world == null) return;

                loc.setWorld(world);

                
                
                if (healthDisplays.containsKey(worldName)) {
                    TextDisplay oldTd = healthDisplays.get(worldName);
                    if (oldTd != null && oldTd.isValid()) {
                        oldTd.remove();
                    }
                }

                
                world.getNearbyEntities(loc.clone().add(0, 2.5, 0), 0.5, 0.5, 0.5).stream()
                        .filter(e -> e instanceof TextDisplay)
                        .forEach(Entity::remove);

                
                baseLocations.put(worldName, loc);
                maxHealth.put(worldName, hp);
                currentHealth.put(worldName, hp);
                baseRadius.put(worldName, radius);
                zoneTypes.put(worldName, type);
                isExploding.put(worldName, false);
                regenAmountMap.put(worldName, regenAmount);
                regenDelayMap.put(worldName, regenDelay);
                lastDamageTime.put(worldName, 0L);

                
                TextDisplay display = world.spawn(loc.clone().add(0, 2.5, 0), TextDisplay.class, td -> {
                    td.setBillboard(Display.Billboard.CENTER);
                    td.setShadowed(true);
                    
                    td.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));

                    
                    td.setInterpolationDuration(3);
                    td.setInterpolationDelay(0);

                    
                    updateTextDisplay(td, hp, hp, displayName);
                });

                healthDisplays.put(worldName, display);
            }
        }
    }

    private void updateTextDisplay(TextDisplay td, double current, double max, String name) {
        String healthBar = "§7[";
        int totalBars = 30;
        int greenBars = (int) ((Math.max(0, current) / max) * totalBars);

        for (int i = 0; i < totalBars; i++) {
            if (i < greenBars) healthBar += "§a|";
            else healthBar += "§c|";
        }
        healthBar += "§7]";

        td.setText(ChatColor.translateAlternateColorCodes('&', name) + "\n" + healthBar + " §f" + (int)Math.max(0, current) + "/" + (int)max);
    }

    public void damageBase(String worldName, double damage) {
        if (!currentHealth.containsKey(worldName) || isExploding.getOrDefault(worldName, false)) return;

        lastDamageTime.put(worldName, System.currentTimeMillis());
        double currentHp = currentHealth.get(worldName);
        double newHp = Math.max(0, currentHp - damage);
        currentHealth.put(worldName, newHp);

        double max = maxHealth.get(worldName);
        TextDisplay td = healthDisplays.get(worldName);

        if (td != null) {
            String name = td.getText().split("\n")[0];
            updateTextDisplay(td, newHp, max, name);
        }

        
        if (newHp <= 0) {
            isExploding.put(worldName, true); 

            
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                world.getPlayers().forEach(p -> {
                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 0.5f);
                });
            }

            
            new BukkitRunnable() {
                @Override
                public void run() {
                    World w = Bukkit.getWorld(worldName);
                    if (w != null && !w.getPlayers().isEmpty()) {
                        Main.getInstance().getDungeonManager().failDungeon(w.getPlayers().get(0));
                    }
                }
            }.runTaskLater(Main.getInstance(), 100L); 
        }
    }

    private void startRegenTask() {
        regenTask = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            for (String worldName : new HashSet<>(currentHealth.keySet())) {
                double current = currentHealth.get(worldName);
                TextDisplay td = healthDisplays.get(worldName);
                if (td == null || !td.isValid()) continue;

                
                if (current <= 0 || isExploding.getOrDefault(worldName, false)) {
                    Location loc = td.getLocation();
                    World world = td.getWorld();

                    
                    world.spawnParticle(Particle.EXPLOSION, loc, 1);
                    world.spawnParticle(Particle.LARGE_SMOKE, loc, 15, 0.5, 0.5, 0.5, 0.05);
                    world.spawnParticle(Particle.FLAME, loc, 10, 0.3, 0.3, 0.3, 0.1);
                    world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.5f);
                    continue;
                }

                
                long lastHit = lastDamageTime.getOrDefault(worldName, 0L);
                long delayMs = regenDelayMap.getOrDefault(worldName, 10) * 1000L;

                if (System.currentTimeMillis() - lastHit > delayMs) {
                    double max = maxHealth.get(worldName);
                    if (current < max) {
                        double regen = regenAmountMap.getOrDefault(worldName, 2.0);
                        double newHp = Math.min(max, current + regen);
                        currentHealth.put(worldName, newHp);

                        String name = td.getText().split("\n")[0];
                        updateTextDisplay(td, newHp, max, name);
                        td.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, td.getLocation(), 3, 0.5, 0.2, 0.5, 0.01);
                    }
                }
            }
        }, 20L, 20L);
    }

    public void cleanup(String worldName) {
        TextDisplay td = healthDisplays.remove(worldName);
        if (td != null) td.remove();

        currentHealth.remove(worldName);
        maxHealth.remove(worldName);
        baseLocations.remove(worldName);
        baseRadius.remove(worldName);
        zoneTypes.remove(worldName);
        lastDamageTime.remove(worldName);
        regenAmountMap.remove(worldName);
        regenDelayMap.remove(worldName);
        isExploding.remove(worldName);
    }

    
    public boolean isInZone(String worldName, Location loc) {
        Location baseLoc = baseLocations.get(worldName);
        if (baseLoc == null) return false;
        double radius = baseRadius.getOrDefault(worldName, 5.0);
        if ("SQUARE".equals(zoneTypes.get(worldName))) {
            return Math.abs(loc.getX() - baseLoc.getX()) <= radius && Math.abs(loc.getZ() - baseLoc.getZ()) <= radius;
        }
        return loc.distanceSquared(baseLoc) <= (radius * radius);
    }
    public Location getBaseLocation(String worldName) { return baseLocations.get(worldName); }
    public boolean isBaseEnabled(String worldName) { return currentHealth.containsKey(worldName); }
}