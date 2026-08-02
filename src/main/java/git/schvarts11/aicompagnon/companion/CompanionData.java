package git.schvarts11.aicompagnon.companion;

import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

/**
 * Immutable snapshot of a companion's state.
 */
public record CompanionData(
        java.util.UUID companionId,
        java.util.UUID ownerUuid,
        String name,
        Location spawnLocation,
        Mob npc,
        java.util.List<ActionHistoryEntry> history,
        long lastTickTime,
        int tickCount
) {
    public CompanionData {
        Objects.requireNonNull(companionId, "companionId");
        Objects.requireNonNull(ownerUuid, "ownerUuid");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(spawnLocation, "spawnLocation");
        Objects.requireNonNull(npc, "npc");
        Objects.requireNonNull(history, "history");
    }

    public record ActionHistoryEntry(long timestamp, String actionType, String result) {
        public ActionHistoryEntry {
            Objects.requireNonNull(actionType, "actionType");
            Objects.requireNonNull(result, "result");
        }
    }
}
