package my.dangmob;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Boss;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Tameable;
import org.bukkit.plugin.java.JavaPlugin;

public class DangMob extends JavaPlugin implements CommandExecutor {

    @Override
    public void onEnable() {
        // Default settings, saved to plugins/DangMob/config.yml
        getConfig().addDefault("skip-bosses", true);
        getConfig().addDefault("skip-tamed", true);
        getConfig().addDefault("skip-named", true);
        getConfig().addDefault("skip-npcs", true);
        getConfig().addDefault("messages.removed", "&aRemoved &f%count% &amobs.");
        getConfig().addDefault("messages.removed-hostile", "&aRemoved &f%count% &ahostile mobs.");
        getConfig().addDefault("messages.reloaded", "&aConfig reloaded.");
        getConfig().addDefault("messages.no-permission", "&cYou don't have permission.");
        getConfig().options().copyDefaults(true);
        saveConfig();

        if (getCommand("mob") != null) getCommand("mob").setExecutor(this);
    }

    private String msg(String key) {
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages." + key, ""));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("dangmob.use")) {
            sender.sendMessage(msg("no-permission"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            sender.sendMessage(msg("reloaded"));
            return true;
        }

        boolean hostileOnly = args.length > 0 && args[0].equalsIgnoreCase("hostile");

        int count = 0;
        for (World w : Bukkit.getWorlds()) {
            for (Mob m : w.getEntitiesByClass(Mob.class)) {
                if (shouldSkip(m, hostileOnly)) continue;
                m.remove();
                count++;
            }
        }

        sender.sendMessage(msg(hostileOnly ? "removed-hostile" : "removed")
                .replace("%count%", String.valueOf(count)));
        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean shouldSkip(Mob m, boolean hostileOnly) {
        if (hostileOnly && !(m instanceof Enemy)) return true;
        if (getConfig().getBoolean("skip-bosses", true) && m instanceof Boss) return true;
        if (getConfig().getBoolean("skip-tamed", true)
                && m instanceof Tameable t && t.isTamed()) return true;
        if (getConfig().getBoolean("skip-named", true) && m.getCustomName() != null) return true;
        if (getConfig().getBoolean("skip-npcs", true) && m.hasMetadata("NPC")) return true;
        return false;
    }
          }
