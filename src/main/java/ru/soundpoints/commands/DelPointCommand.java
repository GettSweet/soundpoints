package ru.soundpoints.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.soundpoints.Main;
import ru.soundpoints.units.SoundPoint;

public class DelPointCommand implements CommandExecutor {
    private final Main plugin;

    public DelPointCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(colorize(plugin.getConfig().getString("messages.only_players")));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("soundpoints.delpoint")) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.no_permission")));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.delpoint_usage")));
            return true;
        }

        String pointKey = args[0];
        SoundPoint point = plugin.getPoints().get(pointKey);

        if (point == null) {
            String message = plugin.getConfig().getString("messages.point_not_found")
                    .replace("{key}", pointKey);
            player.sendMessage(colorize(message));
            player.sendMessage(colorize(plugin.getConfig().getString("messages.use_pointslist")));
            return true;
        }

        plugin.getPoints().remove(pointKey);
        plugin.savePoints(); // Сохраняем изменения в конфиг
        String successMessage = plugin.getConfig().getString("messages.delpoint_success")
                .replace("{key}", pointKey);
        player.sendMessage(colorize(successMessage));

        return true;
    }

    private String colorize(String message) {
        return message.replace("&", "§");
    }
}