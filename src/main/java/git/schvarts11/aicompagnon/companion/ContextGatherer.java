package git.schvarts11.aicompagnon.companion;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import git.schvarts11.aicompagnon.config.AIConfig;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a text snapshot of the companion's environment for the AI.
 */
public final class ContextGatherer {

    private ContextGatherer() {
    }

    public static String gather(CompanionData data, AIConfig config) {
        StringBuilder sb = new StringBuilder();
        Mob npc = data.npc();
        if (npc == null || !npc.isValid() || npc.getLocation() == null) {
            return "[Invalid NPC context]";
        }

        var loc = npc.getLocation();
        World world = loc.getWorld();
        if (world == null) {
            return "[Null world context]";
        }

        sb.append("You are ").append(data.name()).append(", an AI companion.\n");
        sb.append("Position: ").append(String.format("%.1f, %.1f, %.1f in %s", loc.getX(), loc.getY(), loc.getZ(), world.getName())).append("\n");

        long time = world.getTime();
        String timeOfDay = time < 6000 ? "morning" : time < 12000 ? "day" : time < 18000 ? "evening" : "night";
        sb.append("Time: ").append(timeOfDay).append("\n");

        boolean raining = world.hasStorm();
        sb.append("Weather: ").append(raining ? "rainy" : "clear").append("\n");

        int radius = config.contextRadiusBlocks();
        List<String> nearbyPlayers = new ArrayList<>();
        List<String> nearbyEntities = new ArrayList<>();

        for (var e : npc.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof Player p && p.isOnline()) {
                double d = e.getLocation().distanceSquared(loc);
                nearbyPlayers.add(p.getName() + " (" + String.format("%.1f", Math.sqrt(d)) + " blocks)");
            } else if (e instanceof Mob m && m.isValid() && m.getHealth() > 0) {
                double d = e.getLocation().distanceSquared(loc);
                String type = m.getType().name();
                nearbyEntities.add(type + " (" + String.format("%.1f", Math.sqrt(d)) + " blocks)");
            }
        }

        if (!nearbyPlayers.isEmpty()) {
            sb.append("Nearby players: ").append(String.join(", ", nearbyPlayers)).append("\n");
        }
        if (!nearbyEntities.isEmpty()) {
            sb.append("Nearby entities: ").append(String.join(", ", nearbyEntities)).append("\n");
        }

        List<CompanionData.ActionHistoryEntry> history = data.history();
        if (!history.isEmpty()) {
            sb.append("Recent actions (last ").append(Math.min(3, history.size())).append("):\n");
            int limit = Math.min(3, history.size());
            for (int i = history.size() - limit; i < history.size(); i++) {
                var entry = history.get(i);
                sb.append("  - ").append(entry.actionType()).append(" -> ").append(entry.result()).append("\n");
            }
        }

        return sb.toString();
    }
}
