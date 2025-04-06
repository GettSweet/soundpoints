package ru.soundpoints.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import ru.soundpoints.Main;

public class ReloadCommand implements CommandExecutor {
    private final Main plugin;

    public ReloadCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("soundpoints.admin")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав для выполнения этой команды!");
            return true;
        }

        plugin.reloadConfiguration();
        sender.sendMessage(ChatColor.GREEN + "Конфигурация SoundPoints успешно перезагружена!");
        return true;
    }
}