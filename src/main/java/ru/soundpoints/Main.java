package ru.soundpoints;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import ru.soundpoints.commands.*;
import ru.soundpoints.units.CustomSound;
import ru.soundpoints.units.SoundPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Main extends JavaPlugin {
    private Map<String, SoundPoint> points = new HashMap<>();
    private Map<String, List<CustomSound>> soundTypes = new HashMap<>();
    private FileConfiguration config;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        loadSoundTypes();
        loadPoints();
        getCommand("setsoundpoint").setExecutor(new SetSoundPointCommand(this));
        getCommand("pointslist").setExecutor(new PointsListCommand(this));
        getCommand("reloadsoundpoints").setExecutor(new ReloadCommand(this));
        getCommand("forceactivate").setExecutor(new ForceActivateCommand(this));
        getCommand("delpoint").setExecutor(new DelPointCommand(this));
        startPointsTask();
    }

    public void reloadConfiguration() {
        reloadConfig();
        config = getConfig();
        points.clear();
        loadSoundTypes();
        loadPoints();
        getServer().getScheduler().cancelTasks(this);
        startPointsTask();
    }

    @Override
    public void onDisable() {
        savePoints();
    }

    public int getPointsSize() {
        return points.size();
    }

    public Map<String, SoundPoint> getPoints() {
        return points;
    }

    public void addPoint(String key, SoundPoint point) {
        points.put(key, point);
    }

    public void savePoints() {
        config.set("points", null);
        for (Map.Entry<String, SoundPoint> entry : points.entrySet()) {
            String key = entry.getKey();
            SoundPoint point = entry.getValue();
            Location loc = point.getLocation();
            String path = "points." + key;
            config.set(path + ".cords", loc.getX() + ", " + loc.getY() + ", " + loc.getZ() + ", " + loc.getWorld().getName());
            config.set(path + ".type", point.getSoundType());
            config.set(path + ".cooldown", point.getCooldown());
            config.set(path + ".random", point.isRandom());
            config.set(path + ".min_timeing", point.getMinTimeing());
            config.set(path + ".spawn_mob", point.isSpawnMob());
            if (point.isSpawnMob()) {
                List<String> mobTypeNames = new ArrayList<>();
                for (EntityType mobType : point.getMobTypes()) {
                    mobTypeNames.add(mobType.name().toLowerCase());
                }
                config.set(path + ".type_mob", mobTypeNames);
                config.set(path + ".updraft_to_air", point.isUpdraftToAir());
                config.set(path + ".speed_updraft", point.getSpeedUpdraft());
                config.set(path + ".ai", point.hasAi());
            }
        }
        saveConfig();
    }

    private void loadSoundTypes() {
        if (config.contains("type_sounds")) {
            for (String type : config.getConfigurationSection("type_sounds").getKeys(false)) {
                String path = "type_sounds." + type + ".sounds";
                List<CustomSound> sounds = new ArrayList<>();
                for (Object soundObj : config.getList(path)) {
                    Map<String, Object> soundData = (Map<String, Object>) soundObj;
                    Sound sound = Sound.valueOf((String) soundData.get("sound"));
                    float tonality = ((Number) soundData.get("tonality")).floatValue();
                    float volume = ((Number) soundData.get("volume")).floatValue();
                    float hearBlocks = ((Number) soundData.get("hear_blocks")).floatValue();
                    sounds.add(new CustomSound(sound, tonality, volume, hearBlocks));
                }
                soundTypes.put(type, sounds);
            }
        }
    }

    private void startPointsTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis() / 1000;
                Random random = new Random();
                for (SoundPoint point : points.values()) {
                    long timeSinceLast = currentTime - point.getLastActivation();
                    int delay;
                    if (point.isRandom()) {
                        int minTimeing = point.getMinTimeing();
                        int cooldown = point.getCooldown();
                        if (cooldown > minTimeing) {
                            delay = minTimeing + random.nextInt(cooldown - minTimeing);
                        } else {
                            delay = minTimeing;
                        }
                    } else {
                        delay = point.getCooldown();
                    }

                    if (timeSinceLast >= delay) {
                        // Воспроизведение звуков
                        List<CustomSound> sounds = soundTypes.get(point.getSoundType());
                        if (sounds != null) {
                            for (CustomSound customSound : sounds) {
                                for (Player player : point.getLocation().getNearbyPlayers(customSound.getHearBlocks())) {
                                    player.playSound(
                                            point.getLocation(),
                                            customSound.getSound(),
                                            customSound.getVolume(),
                                            customSound.getTonality()
                                    );
                                }
                            }
                        }

                        // Спавн моба
                        if (point.isSpawnMob() && !point.getMobTypes().isEmpty()) {
                            Location spawnLocation = point.getLocation().clone();
                            if (point.isUpdraftToAir()) {
                                if (spawnLocation.getY() < 50) {
                                    spawnLocation.setY(50);
                                }
                            } else {
                                while (spawnLocation.getY() > -2032 && !spawnLocation.getBlock().getType().isSolid()) {
                                    spawnLocation.subtract(0, 1, 0);
                                }
                                spawnLocation.add(0, 1, 0);
                            }

                            EntityType selectedMobType = point.getMobTypes().get(random.nextInt(point.getMobTypes().size()));
                            Entity entity = spawnLocation.getWorld().spawnEntity(spawnLocation, selectedMobType);
                            if (entity instanceof LivingEntity) {
                                LivingEntity mob = (LivingEntity) entity;
                                mob.setAI(point.hasAi());

                                // Логика всплытия
                                if (point.isUpdraftToAir()) {
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            if (!mob.isValid() || mob.getLocation().getBlock().getType().isAir()) {
                                                cancel();
                                                return;
                                            }
                                            Location newLoc = mob.getLocation().add(0, point.getSpeedUpdraft() * 0.1, 0);
                                            if (newLoc.getY() >= 319) {
                                                cancel();
                                                return;
                                            }
                                            mob.teleport(newLoc);
                                        }
                                    }.runTaskTimer(Main.this, 0L, 2L);
                                }
                            } else {
                                getLogger().warning("Сущность " + selectedMobType + " не является живой и не поддерживает AI!");
                            }
                        }

                        point.setLastActivation(currentTime);
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void loadPoints() {
        if (config.contains("points")) {
            for (String key : config.getConfigurationSection("points").getKeys(false)) {
                String path = "points." + key;
                String[] cords = config.getString(path + ".cords").split(", ");
                World world = Bukkit.getWorld(cords[3]);
                Location location = new Location(world, Double.parseDouble(cords[0]), Double.parseDouble(cords[1]), Double.parseDouble(cords[2]));
                String soundType = config.getString(path + ".type");
                int cooldown = config.getInt(path + ".cooldown");
                boolean random = config.getBoolean(path + ".random");
                int minTimeing = config.getInt(path + ".min_timeing");
                boolean spawnMob = config.getBoolean(path + ".spawn_mob");

                List<EntityType> mobTypes = new ArrayList<>();
                boolean updraftToAir = false;
                float speedUpdraft = 1.0f;
                boolean ai = true;

                if (spawnMob) {
                    Object mobTypeConfig = config.get(path + ".type_mob");
                    if (mobTypeConfig instanceof String) {
                        mobTypes.add(EntityType.valueOf(((String) mobTypeConfig).toUpperCase()));
                    } else if (mobTypeConfig instanceof List) {
                        List<String> mobTypeNames = (List<String>) mobTypeConfig;
                        for (String mobName : mobTypeNames) {
                            mobTypes.add(EntityType.valueOf(mobName.toUpperCase()));
                        }
                    }
                    updraftToAir = config.getBoolean(path + ".updraft_to_air");
                    speedUpdraft = (float) config.getDouble(path + ".speed_updraft");
                    ai = config.getBoolean(path + ".ai");
                }

                points.put(key, new SoundPoint(location, soundType, cooldown, random, minTimeing, spawnMob, mobTypes, updraftToAir, speedUpdraft, ai));
            }
        }
    }
}