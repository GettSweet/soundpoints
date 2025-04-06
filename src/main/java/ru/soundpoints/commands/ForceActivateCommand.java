package ru.soundpoints.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.soundpoints.Main;
import ru.soundpoints.units.SoundPoint;

public class ForceActivateCommand implements CommandExecutor {
    private final Main plugin;

    public ForceActivateCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(colorize(plugin.getConfig().getString("messages.only_players")));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("soundpoints.admin")) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.no_permission")));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.forceactivate_usage")));
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

        point.setLastActivation(0);
        String successMessage = plugin.getConfig().getString("messages.forceactivate_success")
                .replace("{key}", pointKey);
        player.sendMessage(colorize(successMessage));

        return true;
    }

    private String colorize(String message) {
        return message.replace("&", "§");
    }
}