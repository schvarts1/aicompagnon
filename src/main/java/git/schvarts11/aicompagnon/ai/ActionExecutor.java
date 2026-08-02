package git.schvarts11.aicompagnon.ai;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import io.papermc.paper.entity.LookAnchor;
import git.schvarts11.aicompagnon.AicompagnonMod;
import git.schvarts11.aicompagnon.config.AIConfig;

/**
 * Executes a parsed {@link Action} using the Paper API.
 *
 * <p>All calls must run on the main server thread.
 */
public final class ActionExecutor {

    private ActionExecutor() {
    }

    public static void execute(Mob npc, Action action) {
        if (npc == null || action == null) {
            return;
        }

        try {
            if (!npc.isValid()) {
                AicompagnonMod.plugin.getLogger().warning("Cannot execute action: NPC is not valid.");
                return;
            }

            LivingEntity entity = npc;
            Location loc = entity.getLocation();
            var world = loc.getWorld();

            if (world == null) {
                AicompagnonMod.plugin.getLogger().warning("Cannot execute action: NPC world is null.");
                return;
            }

            switch (action) {
                case Action.MoveTo moveTo -> handleMoveTo(npc, moveTo, world);
                case Action.Follow follow -> handleFollow(npc, entity, follow);
                case Action.LookAt lookAt -> handleLookAt(entity, lookAt);
                case Action.Say say -> handleSay(say);
                case Action.Attack attack -> handleAttack(npc, entity, attack);
                case Action.Wander wander -> handleWander(npc);
                case Action.Wait wait -> {
                    // no-op
                }
                default -> AicompagnonMod.plugin.getLogger().warning("Unhandled action type: " + action.getClass().getSimpleName());
            }
        } catch (Exception e) {
            AicompagnonMod.plugin.getLogger().warning("Action execution failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleMoveTo(Mob npc, Action.MoveTo moveTo, org.bukkit.World world) {
        Location target = new Location(world, moveTo.x(), moveTo.y(), moveTo.z());
        npc.getPathfinder().moveTo(target);
        AicompagnonMod.plugin.getLogger().fine("NPC moving to " + fmt(target));
    }

    private static void handleFollow(Mob npc, LivingEntity entity, Action.Follow follow) {
        var target = findNearbyEntity(entity, follow.entityName(), 16.0);
        if (target == null) {
            AicompagnonMod.plugin.getLogger().warning("Follow target not found: " + follow.entityName() + "; falling back to wander.");
            npc.getPathfinder().stopPathfinding();
            return;
        }
        npc.getPathfinder().moveTo(target);
        AicompagnonMod.plugin.getLogger().fine("NPC following " + target.getName());
    }

    private static void handleLookAt(LivingEntity entity, Action.LookAt lookAt) {
        entity.lookAt(lookAt.x(), lookAt.y(), lookAt.z(), LookAnchor.EYES);
        AicompagnonMod.plugin.getLogger().fine("NPC looking at " + fmt(new Location(entity.getWorld(), lookAt.x(), lookAt.y(), lookAt.z())));
    }

    private static void handleSay(Action.Say say) {
        String prefix = AIConfig.get().companionPrefix();
        String message = prefix + say.message();
        for (Player player : AicompagnonMod.server.getOnlinePlayers()) {
            player.sendMessage(message);
        }
        AicompagnonMod.plugin.getLogger().fine("NPC said: " + say.message());
    }

    private static void handleAttack(Mob npc, LivingEntity entity, Action.Attack attack) {
        var target = findNearbyEntity(entity, attack.entityName(), 16.0);
        if (target == null) {
            AicompagnonMod.plugin.getLogger().warning("Attack target not found: " + attack.entityName() + "; falling back to wander.");
            npc.getPathfinder().stopPathfinding();
            return;
        }
        npc.getPathfinder().moveTo(target);
        AicompagnonMod.plugin.getLogger().fine("NPC attacking " + target.getName());
    }

    private static void handleWander(Mob npc) {
        npc.getPathfinder().stopPathfinding();
        AicompagnonMod.plugin.getLogger().fine("NPC wandering.");
    }

    private static LivingEntity findNearbyEntity(LivingEntity center, String name, double radius) {
        double bestDist = radius;
        LivingEntity best = null;
        for (var e : center.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof LivingEntity le && le.isValid() && le.getHealth() > 0) {
                String n = le.getName();
                if (n != null && n.equalsIgnoreCase(name)) {
                    double d = le.getLocation().distanceSquared(center.getLocation());
                    if (d < bestDist) {
                        bestDist = d;
                        best = le;
                    }
                }
            }
        }
        return best;
    }

    private static String fmt(Location loc) {
        return String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
    }
}
