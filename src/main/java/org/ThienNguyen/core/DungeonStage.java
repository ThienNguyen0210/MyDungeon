package org.ThienNguyen.core;

import java.util.List;

public class DungeonStage {
    private final String type;
    private final String target;
    private final int goal;
    private final String name; 
    private final String message;
    private final String location;
    private final double distance;
    private final List<String> commands;
    private final boolean aiEnabled;
    private final String aiTarget;
    private final List<MobTarget> multiTargets;

    
    public DungeonStage(String type, String target, int goal, String name, String message, String location, double distance,
                        List<String> commands, boolean aiEnabled, String aiTarget, List<MobTarget> multiTargets) {
        this.type = type;
        this.target = target;
        this.goal = goal;
        
        this.name = (name == null || name.isEmpty()) ? message : name;
        this.message = message;
        this.location = location;
        this.distance = distance;
        this.commands = commands;
        this.aiEnabled = aiEnabled;
        this.aiTarget = aiTarget;
        this.multiTargets = multiTargets;
    }

    
    public String getName() {
        return name;
    }

    public List<MobTarget> getMultiTargets() { return multiTargets; }
    public String getAiTarget() { return aiTarget; }
    public boolean isAiEnabled() { return aiEnabled; }
    public String getType() { return type; }
    public String getTarget() { return target; }
    public int getGoal() { return goal; }
    public String getMessage() { return message; }
    public String getLocation() { return location; }
    public double getDistance() { return distance; }
    public List<String> getCommands() { return commands; }

    public static class MobTarget {
        private final String mobId;
        private final String spawnLocation;
        private final int mobGoal;

        public MobTarget(String mobId, String spawnLocation, int mobGoal) {
            this.mobId = mobId;
            this.spawnLocation = spawnLocation;
            this.mobGoal = mobGoal;
        }
        public String getMobId() { return mobId; }
        public String getSpawnLocation() { return spawnLocation; }
        public int getMobGoal() { return mobGoal; }
    }
}