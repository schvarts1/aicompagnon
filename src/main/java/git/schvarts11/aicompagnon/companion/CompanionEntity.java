package git.schvarts11.aicompagnon.companion;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import git.schvarts11.aicompagnon.AicompagnonMod;
import git.schvarts11.aicompagnon.ai.*;
import git.schvarts11.aicompagnon.config.AIConfig;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a spawned AI companion in the world.
 */
public final class CompanionEntity {

    private final CompanionData data;
    private BukkitTask tickTask;
    private final AIClient aiClient;

    private CompanionEntity(CompanionData data, AIClient aiClient) {
        this.data = data;
        this.aiClient = aiClient;
    }

    public static CompanionEntity spawn(Player owner, String name, Location location, AIClient aiClient) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(aiClient, "aiClient");

        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("Location world is null");
        }

        UUID companionId = UUID.randomUUID();
        String prefix = AIConfig.get().companionPrefix();
        String displayName = prefix + name;

        try {
            Mob npc = world.spawn(location, Zombie.class, CreatureSpawnEvent.SpawnReason.CUSTOM, false, entity -> {
                entity.setCustomName(displayName);
                entity.setCustomNameVisible(true);
                entity.setAI(true);
                entity.setGravity(true);
                entity.setCollidable(false);
                entity.setPersistent(true);
                entity.getPersistentDataContainer().set(
                        new NamespacedKey(AicompagnonMod.plugin, "companion_id"),
                        PersistentDataType.STRING,
                        companionId.toString()
                );
            });

            if (npc == null) {
                throw new IllegalStateException("Failed to spawn companion entity");
            }

            CompanionData companionData = new CompanionData(
                    companionId,
                    owner.getUniqueId(),
                    name,
                    location.clone(),
                    npc,
                    new ArrayList<>(),
                    System.currentTimeMillis(),
                    0
            );

            CompanionEntity entity = new CompanionEntity(companionData, aiClient);
            entity.startTickLoop();
            CompanionManager.get().register(companionData);
            return entity;
        } catch (Exception e) {
            AicompagnonMod.plugin.getLogger().severe("Failed to spawn companion: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void startTickLoop() {
        int interval = AIConfig.get().tickIntervalTicks();
        this.tickTask = Bukkit.getScheduler().runTaskTimer(AicompagnonMod.plugin, this::aiTick, interval, interval);
    }

    private void aiTick() {
        if (data.npc() == null || !data.npc().isValid()) {
            cancelTask();
            CompanionManager.get().unregister(data.companionId());
            return;
        }

        String context = ContextGatherer.gather(data, AIConfig.get());
        String systemPrompt = buildSystemPrompt();

        aiClient.requestAction(systemPrompt, context).thenAccept(action -> {
            Bukkit.getScheduler().runTask(AicompagnonMod.plugin, () -> {
                if (data.npc() == null || !data.npc().isValid()) {
                    return;
                }
                try {
                    ActionExecutor.execute(data.npc(), action);
                    recordHistory(action, "executed");
                } catch (Exception e) {
                    AicompagnonMod.plugin.getLogger().warning("Failed to execute action: " + e.getMessage());
                    recordHistory(action, "error:" + e.getMessage());
                }
            });
        });
    }

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a Minecraft companion NPC named ").append(data.name()).append(".\n");
        sb.append("You can perform ONE of the following actions each turn:\n");
        sb.append("- ACTION: move_to <x> <y> <z>\n");
        sb.append("- ACTION: follow <player_name>\n");
        sb.append("- ACTION: look_at <x> <y> <z>\n");
        sb.append("- ACTION: say <message>\n");
        sb.append("- ACTION: attack <entity_name>\n");
        sb.append("- ACTION: wander\n");
        sb.append("- ACTION: wait\n");
        sb.append("\n");
        sb.append("Respond with EXACTLY ONE line starting with 'ACTION:'. Do not add extra text.\n");
        return sb.toString();
    }

    private void recordHistory(Action action, String result) {
        List<CompanionData.ActionHistoryEntry> newHistory = new ArrayList<>(data.history());
        newHistory.add(new CompanionData.ActionHistoryEntry(System.currentTimeMillis(), actionType(action), result));
        int max = AIConfig.get().maxHistoryEntries();
        while (newHistory.size() > max) {
            newHistory.remove(0);
        }
        // Update data in manager
        CompanionData updated = new CompanionData(
                data.companionId(),
                data.ownerUuid(),
                data.name(),
                data.spawnLocation(),
                data.npc(),
                newHistory,
                System.currentTimeMillis(),
                data.tickCount() + 1
        );
        CompanionManager.get().register(updated);
    }

    private String actionType(Action action) {
        if (action instanceof Action.MoveTo) return "move_to";
        if (action instanceof Action.Follow) return "follow";
        if (action instanceof Action.LookAt) return "look_at";
        if (action instanceof Action.Say) return "say";
        if (action instanceof Action.Attack) return "attack";
        if (action instanceof Action.Wander) return "wander";
        if (action instanceof Action.Wait) return "wait";
        return "unknown";
    }

    public void cancelTask() {
        if (tickTask != null && !tickTask.isCancelled()) {
            tickTask.cancel();
        }
    }

    public CompanionData data() {
        return data;
    }
}
