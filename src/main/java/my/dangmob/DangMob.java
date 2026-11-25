package my.dangmob;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Boss;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.WaterMob;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DangMob extends JavaPlugin implements CommandExecutor {

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        // Default settings, saved to plugins/DangMob/config.yml
        getConfig().addDefault("radius", 30);
        getConfig().addDefault("cooldown-seconds", 10);
        getConfig().addDefault("skip-bosses", true);
        getConfig().addDefault("skip-tamed", true);
        getConfig().addDefault("skip-named", true);
        getConfig().addDefault("skip-npcs", true);
        getConfig().addDefault("skip-villagers", true);
        getConfig().addDefault("skip-passive", false);
        getConfig().addDefault("messages.removed-near", "&aRemoved &f%count% &amobs within &f%radius% &ablocks.");
        getConfig().addDefault("messages.removed-all", "&aRemoved &f%count% &amobs from the whole server.");
        getConfig().addDefault("messages.cooldown", "&cPlease wait &f%time% &cseconds before using this again.");
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
        String sub = args.length > 0 ? args[0].toLowerCase() : "";

        // ---------- Admin: reload ----------
        if (sub.equals("reload")) {
            if (!sender.hasPermission("dangmob.admin")) {
                sender.sendMessage(msg("no-permission"));
                return true;
            }
            reloadConfig();
            sender.sendMessage(msg("reloaded"));
            return true;
        }

        // ---------- Admin: whole server ----------
        if (sub.equals("all")) {
            if (!sender.hasPermission("dangmob.admin")) {
                sender.sendMessage(msg("no-permission"));
                return true;
            }
            int count = 0;
            for (World w : Bukkit.getWorlds()) {
                for (Mob m : w.getEntitiesByClass(Mob.class)) {
                    if (shouldSkip(m, false)) continue;
                    m.remove();
                    count++;
                }
            }
            sender.sendMessage(msg("removed-all").replace("%count%", String.valueOf(count)));
            return true;
        }

        // ---------- Everyone: around the player ----------
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can use this command. Use /mob all from the console.");
            return true;
        }
        if (!p.hasPermission("dangmob.use")) {
            sender.sendMessage(msg("no-permission"));
            return true;
        }

        long cooldownMs = getConfig().getLong("cooldown-seconds", 10) * 1000L;
        if (cooldownMs > 0 && !p.hasPermission("dangmob.bypass")) {
            long left = cooldowns.getOrDefault(p.getUniqueId(), 0L) + cooldownMs - System.currentTimeMillis();
            if (left > 0) {
                p.sendMessage(msg("cooldown").replace("%time%", String.valueOf(left / 1000 + 1)));
                return true;
            }
        }

        boolean hostileOnly = sub.equals("hostile");
        int radius = Math.max(1, Math.min(getConfig().getInt("radius", 30), 128));

        int count = 0;
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof Mob m)) continue;
            if (shouldSkip(m, hostileOnly)) continue;
            m.remove();
            count++;
        }

        cooldowns.put(p.getUniqueId(), System.currentTimeMillis());
        p.sendMessage(msg("removed-near")
                .replace("%count%", String.valueOf(count))
                .replace("%radius%", String.valueOf(radius)));
        return true;
    }

    private boolean shouldSkip(Mob m, boolean hostileOnly) {
        if (hostileOnly && !(m instanceof Enemy)) return true;
        if (getConfig().getBoolean("skip-passive", false)
                && (m instanceof Animals || m instanceof WaterMob)) return true;
        if (getConfig().getBoolean("skip-villagers", true) && m instanceof AbstractVillager) return true;
        if (getConfig().getBoolean("skip-bosses", true) && m instanceof Boss) return true;
        if (getConfig().getBoolean("skip-tamed", true)
                && m instanceof Tameable t && t.isTamed()) return true;
        if (getConfig().getBoolean("skip-named", true) && m.getCustomName() != null) return true;
        if (getConfig().getBoolean("skip-npcs", true) && m.hasMetadata("NPC")) return true;
        return false;
    }
            }
