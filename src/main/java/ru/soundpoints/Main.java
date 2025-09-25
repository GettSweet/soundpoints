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
import ru.soundpoints.commands.PointsListCommand;
import ru.soundpoints.commands.ReloadCommand;
import ru.soundpoints.commands.SetSoundPointCommand;
import ru.soundpoints.units.CustomSound;
import ru.soundpoints.units.SoundPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;

public class Main extends JavaPlugin {
    private Map<String, SoundPoint> points = new HashMap<>();
    private Map<String, List<CustomSound>> soundTypes = new HashMap<>();
    private FileConfiguration config;
    private boolean isMythicMobsEnabled = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        isMythicMobsEnabled = Bukkit.getPluginManager().getPlugin("MythicMobs") != null;
        loadSoundTypes();
        loadPoints();
        getCommand("setsoundpoint").setExecutor(new SetSoundPointCommand(this));
        getCommand("pointslist").setExecutor(new PointsListCommand(this));
        getCommand("reloadsoundpoints").setExecutor(new ReloadCommand(this));
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
                config.set(path + ".type_mob", point.getMobTypes());
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

                            String selectedMob = point.getMobTypes().get(random.nextInt(point.getMobTypes().size()));
                            World world = spawnLocation.getWorld();
                            LivingEntity mob = null;
                            boolean spawned = false;

                            if (isMythicMobsEnabled) {
                                MythicBukkit mythic = MythicBukkit.inst();
                                Optional<MythicMob> mmob = mythic.getMobManager().getMythicMob(selectedMob);
                                if (mmob.isPresent()) {
                                    ActiveMob am = mythic.getMobManager().spawnMob(selectedMob, spawnLocation);
                                    Entity entity = am.getEntity().getBukkitEntity();
                                    if (entity instanceof LivingEntity) {
                                        mob = (LivingEntity) entity;
                                        spawned = true;
                                    } else {
                                        getLogger().warning("MythicMob " + selectedMob + " не является живой сущностью!");
                                    }
                                }
                            }

                            if (!spawned) {
                                try {
                                    EntityType type = EntityType.valueOf(selectedMob.toUpperCase());
                                    Entity entity = world.spawnEntity(spawnLocation, type);
                                    if (entity instanceof LivingEntity) {
                                        mob = (LivingEntity) entity;
                                        spawned = true;
                                    } else {
                                        getLogger().warning("Сущность " + selectedMob + " не является живой!");
                                    }
                                } catch (IllegalArgumentException e) {
                                    getLogger().warning("Неверный тип сущности: " + selectedMob + ". Проверьте, является ли это ванильным мобом или мобом из MythicMobs.");
                                }
                            }

                            if (mob != null) {
                                mob.setAI(point.hasAi());

                                // Логика всплытия
                                if (point.isUpdraftToAir()) {
                                    LivingEntity finalMob = mob; // Для использования в runnable
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            if (!finalMob.isValid() || finalMob.getLocation().getBlock().getType().isAir()) {
                                                cancel();
                                                return;
                                            }
                                            Location newLoc = finalMob.getLocation().add(0, point.getSpeedUpdraft() * 0.1, 0);
                                            if (newLoc.getY() >= 319) {
                                                cancel();
                                                return;
                                            }
                                            finalMob.teleport(newLoc);
                                        }
                                    }.runTaskTimer(Main.this, 0L, 2L);
                                }
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

                List<String> mobTypes = new ArrayList<>();
                boolean updraftToAir = false;
                float speedUpdraft = 1.0f;
                boolean ai = true;

                if (spawnMob) {
                    Object mobTypeConfig = config.get(path + ".type_mob");
                    if (mobTypeConfig instanceof String) {
                        mobTypes.add((String) mobTypeConfig);
                    } else if (mobTypeConfig instanceof List) {
                        mobTypes = (List<String>) mobTypeConfig;
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