package org.ThienNguyen.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class DungeonCommand implements CommandExecutor {

    
    private final Map<String, SubCommand> commands = new HashMap<>();

    public DungeonCommand() {
        
        commands.put("create", new CreateDungeon());
        commands.put("start", new StartDungeon());
        commands.put("quit", new QuitDungeon());
        commands.put("reload", new ReloadDungeon()); 
        commands.put("database", new RequireCommand()); 
        commands.put("edit", new EditDungeon()); 
        commands.put("save", new SaveDungeon()); 

        
        
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cHệ thống Dungeon chỉ dành cho người chơi trong game!");
            return true;
        }

        
        if (args.length == 0 || !commands.containsKey(args[0].toLowerCase())) {
            sendHelp(player);
            return true;
        }

        
        SubCommand sub = commands.get(args[0].toLowerCase());

        
        if (sub != null) {
            sub.execute(player, args);
        } else {
            player.sendMessage("§cLỗi hệ thống: Lệnh này đã được đăng ký nhưng không tìm thấy bộ xử lý!");
        }

        return true;
    }

    /**
     * Gửi danh sách các lệnh con hiện có cho người chơi
     */
    private void sendHelp(Player player) {
        player.sendMessage("");
        
        player.sendMessage("§c§l☠ §m      §r §4§l[ MYDUNGEON ] §4§l§m      §4 §c§l☠");
        player.sendMessage("");
        for (String cmd : commands.keySet()) {
            player.sendMessage(" §f뀁 §e/dungeon §b" + cmd.toLowerCase() + " §8» §7Thực hiện");
        }

        player.sendMessage("");
        player.sendMessage("§4§l☠ §m                          §r §4§l☠");
    }
}