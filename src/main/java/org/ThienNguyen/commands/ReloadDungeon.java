package org.ThienNguyen.commands;

import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReloadDungeon implements SubCommand {

    @Override
    public void execute(Player player, String[] args) {
        if (!player.hasPermission("mydungeon.admin")) {
            player.sendMessage("§cBạn không có quyền sử dụng lệnh này!");
            return;
        }

        try {
            Main main = Main.getInstance();

            
            main.reloadConfig();
            main.reloadMessages();
            main.getDungeonManager().reloadRequireConfig();

            
            processAndSortDungeons();

            
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "papi reload");
            }

            player.sendMessage("");
            player.sendMessage("§a§lMyDungeon §8» §fĐã reload và đánh số lại Stage (1 → N)!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.8f);
            player.sendTitle("§aSuccess Reload", "§7Stages renumbered from 1 to N", 10, 50, 20);

        } catch (Exception e) {
            player.sendMessage("§c[!] Có lỗi khi reload! Kiểm tra console.");
            e.printStackTrace();
        }
    }

    private void processAndSortDungeons() {
        File dungeonDir = new File(Main.getInstance().getDataFolder(), "Dungeons");
        if (!dungeonDir.exists() || !dungeonDir.isDirectory()) {
            Bukkit.getLogger().info("[MyDungeon] Thư mục Dungeons không tồn tại hoặc không phải thư mục.");
            return;
        }

        File[] files = dungeonDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            Bukkit.getLogger().info("[MyDungeon] Không tìm thấy file .yml nào trong Dungeons.");
            return;
        }

        for (File file : files) {
            try {
                List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                List<String> newLines = renumberStages(lines);

                
                Files.write(file.toPath(), newLines, StandardCharsets.UTF_8);

                Bukkit.getLogger().info("[MyDungeon] Đã renumber thành công " + newLines.size() + " dòng → "
                        + (newStageCount) + " stages trong file: " + file.getName());

            } catch (IOException e) {
                Bukkit.getLogger().warning("[MyDungeon] Không thể đọc/ghi file: " + file.getName());
                e.printStackTrace();
            }
        }
    }

    
    private int newStageCount;

    private List<String> renumberStages(List<String> lines) {
        List<String> newLines = new ArrayList<>();
        boolean inStages = false;
        int stageIndent = -1;
        newStageCount = 0;
        int newId = 1;

        String pendingKeyLine = null;
        List<String> pendingContent = null;

        for (String line : lines) {
            String trimmed = line.trim();

            
            if (trimmed.equals("stages:") || trimmed.startsWith("stages:")) {
                inStages = true;
                stageIndent = getIndent(line) + 2; 
                newLines.add(line);
                resetPending(pendingKeyLine, pendingContent, newLines, newId);
                pendingKeyLine = null;
                pendingContent = null;
                continue;
            }

            if (!inStages) {
                newLines.add(line);
                continue;
            }

            int currentIndent = getIndent(line);

            
            if (currentIndent == stageIndent && isStageKey(trimmed)) {
                
                if (pendingKeyLine != null) {
                    String newKeyLine = replaceStageKey(pendingKeyLine, newId);
                    newLines.add(newKeyLine);
                    newLines.addAll(pendingContent);
                    newId++;
                    newStageCount++;
                }

                
                pendingKeyLine = line;
                pendingContent = new ArrayList<>();
                continue;
            }

            
            if (pendingContent != null && currentIndent > stageIndent) {
                pendingContent.add(line);
                continue;
            }

            
            if (pendingKeyLine != null) {
                String newKeyLine = replaceStageKey(pendingKeyLine, newId);
                newLines.add(newKeyLine);
                newLines.addAll(pendingContent);
                newId++;
                newStageCount++;
                pendingKeyLine = null;
                pendingContent = null;
            }

            
            inStages = false;
            newLines.add(line);
        }

        
        if (pendingKeyLine != null) {
            String newKeyLine = replaceStageKey(pendingKeyLine, newId);
            newLines.add(newKeyLine);
            newLines.addAll(pendingContent);
            newStageCount++;
        }

        return newLines;
    }

    private void resetPending(String pendingKeyLine, List<String> pendingContent, List<String> newLines, int newId) {
        if (pendingKeyLine != null) {
            String newKeyLine = replaceStageKey(pendingKeyLine, newId);
            newLines.add(newKeyLine);
            newLines.addAll(pendingContent);
        }
    }

    private int getIndent(String line) {
        int indent = 0;
        while (indent < line.length() && Character.isWhitespace(line.charAt(indent))) {
            indent++;
        }
        return indent;
    }

    private boolean isStageKey(String trimmed) {
        
        return trimmed.matches("^'?\\d+'?\\s*:.*$");
    }

    private String replaceStageKey(String originalLine, int newId) {
        Pattern p = Pattern.compile("^(\\s*)('?)(\\d+)('?)(\\s*:.*)$");
        Matcher m = p.matcher(originalLine);

        if (m.matches()) {
            String indent = m.group(1);
            String quoteOpen = m.group(2);
            String afterColon = m.group(5);

            String newKeyPart = quoteOpen.isEmpty() ? String.valueOf(newId) : "'" + newId + "'";
            return indent + newKeyPart + afterColon;
        }

        
        return originalLine.replaceFirst("\\b\\d+\\b", String.valueOf(newId));
    }
}