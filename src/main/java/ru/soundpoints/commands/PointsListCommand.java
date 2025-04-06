package ru.soundpoints.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.soundpoints.units.SoundPoint;
import ru.soundpoints.Main;

import java.util.List;

public class PointsListCommand implements CommandExecutor {
    private final Main plugin;

    public PointsListCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.only_players", "Только игроки могут использовать эту команду!")));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("soundpoints.admin")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.no_permission", "У вас нет прав для использования этой команды!")));
            return true;
        }

        if (args.length == 0) {
            // Вывод списка всех точек
            if (plugin.getPointsSize() == 0) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.points_list_empty", "Список точек пуст!")));
                return true;
            }

            String title = plugin.getConfig().getString("messages.points_list_title", "Список точек ({size}):").replace("{size}", String.valueOf(plugin.getPointsSize()));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', title));
            for (String key : plugin.getPoints().keySet()) {
                SoundPoint point = plugin.getPoints().get(key);

                TextComponent pointMessage = new TextComponent(ChatColor.YELLOW + "Точка " + key);
                pointMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pointslist " + key));
                pointMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GREEN + "Нажмите, чтобы увидеть информацию")));
                player.spigot().sendMessage(pointMessage);
            }
        } else if (args.length == 1) {
            // Вывод информации о конкретной точке
            String key = args[0];
            SoundPoint point = plugin.getPoints().get(key);
            if (point == null) {
                String notFound = plugin.getConfig().getString("messages.point_not_found", "Точка {key} не найдена!").replace("{key}", key);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', notFound));
                return true;
            }

            Location loc = point.getLocation();
            String enabled = plugin.getConfig().getString("messages.enabled", "вкл");
            String disabled = plugin.getConfig().getString("messages.disabled", "выкл");

            // Список из конфига
            List<String> infoLines = plugin.getConfig().getStringList("messages.point_info");
            String coordsLabelText = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.coords_label", "Координаты: "));

            for (String line : infoLines) {
                String mobTypesStr = point.isSpawnMob() ? String.join(", ", point.getMobTypes().stream().map(type -> type.name().toLowerCase()).toList()) : "";
                String formattedLine = line
                        .replace("{key}", key)
                        .replace("{coords}", String.format("%.0f, %.0f, %.0f", loc.getX(), loc.getY(), loc.getZ()))
                        .replace("{coords_label}", plugin.getConfig().getString("messages.coords_label", "Координаты: "))
                        .replace("{world}", loc.getWorld().getName())
                        .replace("{sound_type}", point.getSoundType())
                        .replace("{cooldown}", String.valueOf(point.getCooldown()))
                        .replace("{random}", point.isRandom() ? enabled : disabled)
                        .replace("{random_extra}", point.isRandom() ? plugin.getConfig().getString("messages.random_extra", " (мин. время: {min_time} сек)").replace("{min_time}", String.valueOf(point.getMinTimeing())) : "")
                        .replace("{spawn_mob}", point.isSpawnMob() ? enabled : disabled)
                        .replace("{mob_type}", mobTypesStr) // Обновлено для списка
                        .replace("{updraft}", point.isSpawnMob() ? (point.isUpdraftToAir() ? enabled : disabled) : "")
                        .replace("{updraft_extra}", point.isSpawnMob() && point.isUpdraftToAir() ? plugin.getConfig().getString("messages.updraft_extra", " (скорость: {speed})").replace("{speed}", String.valueOf(point.getSpeedUpdraft())) : "")
                        .replace("{speed}", point.isSpawnMob() && point.isUpdraftToAir() ? String.valueOf(point.getSpeedUpdraft()) : "")
                        .replace("{ai}", point.isSpawnMob() ? (point.hasAi() ? enabled : disabled) : "");

                formattedLine = ChatColor.translateAlternateColorCodes('&', formattedLine);

                // кликабельные координаты
                if (formattedLine.contains(coordsLabelText)) {
                    TextComponent coordsLabel = new TextComponent(formattedLine.split(coordsLabelText)[0] + coordsLabelText);
                    TextComponent coords = new TextComponent(formattedLine.split(coordsLabelText)[1].trim());
                    coords.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pointslist teleport " + key));
                    String hoverText = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.coords_hover", "Нажмите, чтобы телепортироваться"));
                    coords.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText)));
                    coordsLabel.addExtra(coords);
                    player.spigot().sendMessage(coordsLabel);
                } else {
                    player.sendMessage(formattedLine);
                }
            }
        } else if (args.length == 2 && args[0].equals("teleport")) {
            // Обработка телепортации
            String teleportKey = args[1];
            SoundPoint teleportPoint = plugin.getPoints().get(teleportKey);
            if (teleportPoint != null) {
                player.teleport(teleportPoint.getLocation());
            } else {
                String notFound = plugin.getConfig().getString("messages.point_not_found", "Точка {key} не найдена!").replace("{key}", teleportKey);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', notFound));
            }
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.point_info_usage", "Использование: /pointslist [номер точки]")));
        }

        return true;
    }
}