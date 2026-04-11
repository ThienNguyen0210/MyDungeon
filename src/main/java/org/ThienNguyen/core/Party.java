package org.ThienNguyen.core;

import java.util.*;

public class Party {
    private UUID leader;
    private final List<UUID> members = new ArrayList<>();
    private final Set<UUID> readyPlayers = new HashSet<>();
    private final String name;

    public Party(UUID leader, String name) {
        this.leader = leader;
        this.name = name;
        this.members.add(leader);
    }

    public UUID getLeader() { return leader; }

    public void setLeader(UUID leader) {
        this.leader = leader;
    }

    public List<UUID> getMembers() { return members; }
    public String getName() { return name; }

    public boolean isReady(UUID uuid) { return readyPlayers.contains(uuid); }

    

    /**
     * Gán trực tiếp trạng thái sẵn sàng (Dùng cho joinParty)
     */
    public void setReady(UUID uuid, boolean ready) {
        if (ready) {
            readyPlayers.add(uuid);
        } else {
            readyPlayers.remove(uuid);
        }
    }

    /**
     * Đảo ngược trạng thái (Dùng cho Click GUI)
     */
    public void toggleReady(UUID uuid) {
        if (readyPlayers.contains(uuid)) {
            readyPlayers.remove(uuid);
        } else {
            readyPlayers.add(uuid);
        }
    }

    /**
     * Kiểm tra xem tất cả mọi người có đang sẵn sàng không
     */
    public boolean isAllReady() {
        
        if (members.isEmpty()) return false;
        return readyPlayers.size() >= members.size();
    }
}