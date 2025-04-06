package ru.soundpoints.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatColor;
import ru.soundpoints.units.SoundPoint;
import ru.soundpoints.Main;

import java.util.ArrayList;
import java.util.List;

public class SetSoundPointCommand implements CommandExecutor {
    private final Main plugin;

    public SetSoundPointCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.only_players")));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("soundpoints.admin")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.no_permission")));
            return true;
        }

        Location loc = player.getLocation();
        String key = String.format("%02d", plugin.getPointsSize() + 1);
        List<EntityType> defaultMobTypes = new ArrayList<>();
        defaultMobTypes.add(EntityType.SALMON); // По умолчанию один моб
        SoundPoint newPoint = new SoundPoint(loc, "scream", 60, true, 30, true, defaultMobTypes, true, 1.0f, true);
        plugin.addPoint(key, newPoint);
        plugin.savePoints();
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.point_saved")));
        return true;
    }
}