package org.ThienNguyen.listeners;

import org.ThienNguyen.Main;
import org.ThienNguyen.gui.DungeonEditGUI;
import org.ThienNguyen.core.DungeonManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;

import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DungeonEventListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDungeonDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        World world = player.getWorld();
        String worldName = world.getName();

        
        if (worldName.startsWith("temp_")) {
            
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);

            
            DungeonManager dm = Main.getInstance().getDungeonManager();
            String dungeonId = worldName.split("_")[1];
            File dungeonFile = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
            YamlConfiguration dungeonConfig = YamlConfiguration.loadConfiguration(dungeonFile);
            FileConfiguration msgConfig = Main.getInstance().getMessagesConfig();

            
            int respawnCD = dungeonConfig.getInt("respawn-cooldown", 2);
            int maxLives = dungeonConfig.getInt("max-lives", 3);
            boolean respawnAtSpawn = dungeonConfig.getBoolean("respawn-at-spawn", false);

            
            dm.incrementDeath(player);
            int currentDeaths = dm.getDeathCount(player);
            Location deathLocation = player.getLocation().clone();

            
            Location finalRespawnLocation;

            if (respawnAtSpawn) {
                
                Location checkpoint = dm.getWorldCheckpoint(worldName);

                if (checkpoint != null) {
                    finalRespawnLocation = checkpoint;
                } else {
                    
                    String spawnStr = dungeonConfig.getString("spawn-location");
                    finalRespawnLocation = parseLocation(world, spawnStr);

                    
                    if (finalRespawnLocation == null) {
                        finalRespawnLocation = world.getSpawnLocation();
                    }
                }
            } else {
                
                finalRespawnLocation = deathLocation;
            }

            
            if (currentDeaths >= maxLives) {
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage("§c§lBẠN ĐÃ HẾT MẠNG! §fBạn hiện là linh hồn quan sát đồng đội.");

                if (!dm.isAnyMemberAlive(world)) {
                    for (Player p : new ArrayList<>(world.getPlayers())) {
                        p.sendMessage("§c§lCẢ ĐỘI ĐÃ THẤT BẠI! §fKhông còn ai có thể chiến đấu.");
                        dm.failDungeon(p);
                    }
                }
            } else {
                
                Location displayLoc = deathLocation.clone().add(0, 1.5, 0);
                spawnDeathDisplay(player, displayLoc, respawnCD, msgConfig);

                
                player.setGameMode(GameMode.SPECTATOR);
                player.teleport(deathLocation.clone().add(0, 1, 0));

                
                handleRespawnCooldown(player, respawnCD, msgConfig, finalRespawnLocation);
            }
        }
    }

    /**
     * Hàm parse tọa độ thông minh: hỗ trợ cả "x,y,z" và "world,x,y,z"
     */
    private Location parseLocation(World world, String str) {
        if (str == null || str.isEmpty()) return null;
        try {
            String[] parts = str.split(",");
            
            int idx = 0;
            try {
                Double.parseDouble(parts[0].trim());
            } catch (NumberFormatException e) {
                idx = 1;
            }

            if (parts.length < idx + 3) return null;

            double x = Double.parseDouble(parts[idx].trim());
            double y = Double.parseDouble(parts[idx + 1].trim());
            double z = Double.parseDouble(parts[idx + 2].trim());

            float yaw = (parts.length > idx + 3) ? Float.parseFloat(parts[idx + 3].trim()) : 0;
            float pitch = (parts.length > idx + 4) ? Float.parseFloat(parts[idx + 4].trim()) : 0;

            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[Dungeon] Khong the parse toa do: " + str);
            return null;
        }
    }


    private void spawnDeathDisplay(Player player, Location loc, int cooldown, FileConfiguration msgConfig) {
        World world = loc.getWorld();
        if (world == null) return;

        
        Location asLoc = loc.clone().add(0, -1.45, 0);
        ArmorStand as = (ArmorStand) world.spawnEntity(asLoc, EntityType.ARMOR_STAND);

        as.setVisible(false);
        as.setGravity(false);
        as.setInvulnerable(true);
        as.setMarker(false);
        as.setBasePlate(false);
        as.setArms(true);

        
        
        as.setBodyPose(new org.bukkit.util.EulerAngle(Math.toRadians(90), 0, 0));
        as.setHeadPose(new org.bukkit.util.EulerAngle(Math.toRadians(90), 0, 0));

        
        as.setLeftArmPose(new org.bukkit.util.EulerAngle(Math.toRadians(90), Math.toRadians(10), 0));
        as.setRightArmPose(new org.bukkit.util.EulerAngle(Math.toRadians(90), Math.toRadians(-10), 0));

        
        as.setLeftLegPose(new org.bukkit.util.EulerAngle(Math.toRadians(90), 0, 0));
        as.setRightLegPose(new org.bukkit.util.EulerAngle(Math.toRadians(90), 0, 0));

        
        
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) skull.getItemMeta();
        if (headMeta != null) {
            headMeta.setOwningPlayer(player);
            skull.setItemMeta(headMeta);
        }
        as.getEquipment().setHelmet(skull);
        TextDisplay td = (TextDisplay) world.spawnEntity(loc.clone().add(0, 0.5, 0), EntityType.TEXT_DISPLAY);
        td.setBillboard(Display.Billboard.CENTER);
        td.setSeeThrough(true);
        td.setShadowed(true);
        td.setBrightness(new Display.Brightness(15, 15));

        Transformation t = td.getTransformation();
        t.getScale().set(0.6f, 0.6f, 0.6f);
        td.setTransformation(t);

        new BukkitRunnable() {
            int remaining = cooldown;
            @Override
            public void run() {
                
                if (!player.isOnline() || remaining <= 0 || !player.getWorld().getName().startsWith("temp_")) {
                    as.remove();
                    td.remove();
                    cancel();
                    return;
                }

                String template = msgConfig.getString("respawn.death-display", "&c%player%\n&7Hồi sinh sau &e%time%s");
                td.setText(ChatColor.translateAlternateColorCodes('&', template
                        .replace("%player%", player.getName())
                        .replace("%time%", String.valueOf(remaining))));

                remaining--;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 20L);
    }

    
    private ItemStack createColoredArmor(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        org.bukkit.inventory.meta.LeatherArmorMeta meta = (org.bukkit.inventory.meta.LeatherArmorMeta) item.getItemMeta();
        if (meta != null) {
            meta.setColor(color);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void handleRespawnCooldown(Player player, int cooldown, FileConfiguration msgConfig, Location deathLocation) {
        new BukkitRunnable() {
            int timeLeft = cooldown;
            @Override
            public void run() {
                if (!player.isOnline() || !player.getWorld().getName().startsWith("temp_")) {
                    cancel();
                    return;
                }

                if (timeLeft <= 0) {
                    player.setGameMode(GameMode.SURVIVAL);
                    player.teleport(deathLocation);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', msgConfig.getString("respawn.message", "&a[!] Hồi sinh thành công!")));
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                    cancel();
                    return;
                }

                String title = ChatColor.translateAlternateColorCodes('&', msgConfig.getString("respawn.title", "&aHỒI SINH"));
                String sub = ChatColor.translateAlternateColorCodes('&', msgConfig.getString("respawn.subtitle", "&fSau %time%s")).replace("%time%", String.valueOf(timeLeft));
                player.sendTitle(title, sub, 0, 21, 0);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);

                timeLeft--;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 20L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDungeonPvP(EntityDamageByEntityEvent event) {
        
        Player victim = null;
        Player attacker = null;

        if (event.getEntity() instanceof Player) victim = (Player) event.getEntity();

        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player) {
            attacker = (Player) proj.getShooter();
        }

        if (victim != null && attacker != null) {
            if (victim.getWorld().getName().startsWith("temp_") && victim.getWorld().equals(attacker.getWorld())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEditToolInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.BLAZE_ROD && item.hasItemMeta() &&
                item.getItemMeta().getDisplayName().contains("Dungeon Edit Tool")) {

            event.setCancelled(true);
            if (!player.hasMetadata("editing_dungeon_id")) return;
            String dungeonId = player.getMetadata("editing_dungeon_id").get(0).asString();
            Block block = event.getClickedBlock();

            
            if (player.isSneaking() && block != null) {
                Location loc = block.getLocation();
                DungeonManager dm = Main.getInstance().getDungeonManager();

                if (event.getAction().name().contains("LEFT")) {
                    
                    String format = String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
                    dm.addBlockToGroup(dungeonId, "break-groups", "auto_group", format);

                    player.sendMessage("§c§l[Break] §fĐã thêm block " + block.getType() + " vào auto_group");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 2f);
                }
                else if (event.getAction().name().contains("RIGHT")) {
                    
                    String format = String.format("%.1f, %.1f, %.1f, %s", loc.getX(), loc.getY(), loc.getZ(), block.getType().name());
                    dm.addBlockToGroup(dungeonId, "place-groups", "auto_group", format);

                    player.sendMessage("§a§l[Place] §fĐã lưu " + block.getType() + " vào auto_group");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 2f);
                }
                return;
            }

            
            if (event.getAction().name().contains("RIGHT")) {
                DungeonEditGUI.open(player, dungeonId);
            } else if (event.getAction().name().contains("LEFT") && block != null) {
                player.sendMessage("§e§l[Tọa độ] §f" + String.format("%.1f, %.1f, %.1f", block.getLocation().getX(), block.getLocation().getY(), block.getLocation().getZ()));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDungeonRespawn(PlayerRespawnEvent event) {
        if (event.getPlayer().getWorld().getName().startsWith("temp_")) {
            event.setRespawnLocation(event.getPlayer().getLocation());
        }
    }
    /**
     * CẤM LỆNH TRONG PHÓ BẢN
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();

        
        if (!worldName.startsWith("temp_")) return;

        DungeonManager dm = Main.getInstance().getDungeonManager();
        String fullCommand = event.getMessage().toLowerCase().trim();

        if (dm.isCommandRestrictionEnabled()) {
            
            if (fullCommand.startsWith("/dungeon") || fullCommand.startsWith("/party") || fullCommand.startsWith("/todoi")) {
                return; 
            }

            event.setCancelled(true);
            player.sendMessage("§c§l[!] §fBạn không được sử dụng lệnh này khi đang ở trong phó bản!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            return;
        }
        else {
            
            List<String> blocked = dm.getBlockedCommands();
            if (blocked.isEmpty()) return;

            
            String cmdLabel = fullCommand.split(" ")[0].substring(1);

            if (blocked.contains(cmdLabel)) {
                event.setCancelled(true);
                player.sendMessage("§c§l[!] §fLệnh §e/" + cmdLabel + " §fđã bị cấm trong phó bản!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            }
        }
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDungeonDamageControl(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        Entity damager = event.getDamager();

        
        if (!victim.getWorld().getName().startsWith("temp_")) return;

        
        Entity actualAttacker = damager;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Entity shooter) {
            actualAttacker = shooter;
        }

        
        
        boolean allowMobDamage = Main.getInstance().getConfig().getBoolean("dungeon-settings.allow-mob-damage", false);

        
        if (!allowMobDamage) {
            
            if (!(victim instanceof Player) && !(actualAttacker instanceof Player)) {
                if (victim instanceof LivingEntity && actualAttacker instanceof LivingEntity) {
                    event.setCancelled(true);
                    return; 
                }
            }
        }

        
        if (victim instanceof Player pVictim && actualAttacker instanceof Player pAttacker) {
            if (pVictim.getWorld().getName().equals(pAttacker.getWorld().getName())) {
                event.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onMobUnload(org.bukkit.event.world.EntitiesUnloadEvent event) {
        if (event.getWorld().getName().startsWith("temp_")) {
            event.getEntities().forEach(entity -> {
                if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                    entity.setPersistent(true); 
                }
            });
        }
    }
    
    @EventHandler
    public void onPlayerToggleFlight(org.bukkit.event.player.PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();

        if (world.getName().startsWith("temp_")) {
            String dungeonId = world.getName().split("_")[1];
            File dungeonFile = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
            YamlConfiguration dungeonConfig = YamlConfiguration.loadConfiguration(dungeonFile);

            
            if (!dungeonConfig.getBoolean("allow-fly", false) && player.getGameMode() != GameMode.CREATIVE) {
                event.setCancelled(true);
                player.setAllowFlight(false);
                player.setFlying(false);

                
                String msg = Main.getInstance().getMessagesConfig().getString("dungeon.restrictions.fly-disabled",
                        "&c&l[!] &fPhó bản này không cho phép sử dụng khả năng bay!");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            }
        }
    }

    
    @EventHandler
    public void onElytraGlide(org.bukkit.event.entity.EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        World world = player.getWorld();
        if (world.getName().startsWith("temp_")) {
            String dungeonId = world.getName().split("_")[1];
            File dungeonFile = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
            YamlConfiguration dungeonConfig = YamlConfiguration.loadConfiguration(dungeonFile);

            
            if (!dungeonConfig.getBoolean("allow-elytra", false)) {
                event.setCancelled(true);
                String msg = Main.getInstance().getMessagesConfig().getString("dungeon.restrictions.elytra-disabled",
                        "&c&l[!] &fPhó bản này không cho phép sử dụng Elytra!");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                
                player.setGliding(false);
            }
        }
    }
}