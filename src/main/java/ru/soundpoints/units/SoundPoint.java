package ru.soundpoints.units;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class SoundPoint {
    private Location location;
    private String soundType;
    private int cooldown;
    private boolean random;
    private int minTimeing;
    private boolean spawnMob;
    private List<String> mobTypes;
    private boolean updraftToAir;
    private float speedUpdraft;
    private boolean ai;
    private long lastActivation;

    public SoundPoint(Location location, String soundType, int cooldown, boolean random, int minTimeing,
                      boolean spawnMob, List<String> mobTypes, boolean updraftToAir, float speedUpdraft, boolean ai) {
        this.location = location;
        this.soundType = soundType;
        this.cooldown = cooldown;
        this.random = random;
        this.minTimeing = minTimeing;
        this.spawnMob = spawnMob;
        this.mobTypes = mobTypes != null ? new ArrayList<>(mobTypes) : new ArrayList<>(); // Защита от null
        this.updraftToAir = updraftToAir;
        this.speedUpdraft = speedUpdraft;
        this.ai = ai;
        this.lastActivation = 0;
    }

    // Геттеры
    public Location getLocation() { return location; }
    public String getSoundType() { return soundType; }
    public int getCooldown() { return cooldown; }
    public boolean isRandom() { return random; }
    public int getMinTimeing() { return minTimeing; }
    public boolean isSpawnMob() { return spawnMob; }
    public List<String> getMobTypes() { return mobTypes; }
    public boolean isUpdraftToAir() { return updraftToAir; }
    public float getSpeedUpdraft() { return speedUpdraft; }
    public boolean hasAi() { return ai; }
    public long getLastActivation() { return lastActivation; }
    public void setLastActivation(long time) { this.lastActivation = time; }
}