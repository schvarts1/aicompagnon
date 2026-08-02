package git.schvarts11.aicompagnon.companion;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of all active companions.
 */
public final class CompanionManager {

    private static final CompanionManager INSTANCE = new CompanionManager();

    private final Map<java.util.UUID, CompanionData> companions = new ConcurrentHashMap<>();

    private CompanionManager() {
    }

    public static CompanionManager get() {
        return INSTANCE;
    }

    public boolean register(CompanionData data) {
        Objects.requireNonNull(data, "data");
        if (companions.containsKey(data.companionId())) {
            return false;
        }
        companions.put(data.companionId(), data);
        return true;
    }

    public void unregister(java.util.UUID companionId) {
        CompanionData data = companions.remove(companionId);
        if (data != null && data.npc() != null && data.npc().isValid()) {
            data.npc().remove();
        }
    }

    public Optional<CompanionData> getById(java.util.UUID companionId) {
        return Optional.ofNullable(companions.get(companionId));
    }

    public List<CompanionData> getByOwner(java.util.UUID ownerUuid) {
        List<CompanionData> out = new ArrayList<>();
        for (CompanionData data : companions.values()) {
            if (data.ownerUuid().equals(ownerUuid)) {
                out.add(data);
            }
        }
        return out;
    }

    public Collection<CompanionData> getAll() {
        return Collections.unmodifiableCollection(companions.values());
    }

    public int size() {
        return companions.size();
    }

    public void unregisterAll() {
        for (java.util.UUID id : new ArrayList<>(companions.keySet())) {
            unregister(id);
        }
        companions.clear();
    }

    public int cleanupOrphaned() {
        int cleaned = 0;
        Iterator<Map.Entry<java.util.UUID, CompanionData>> it = companions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<java.util.UUID, CompanionData> entry = it.next();
            CompanionData data = entry.getValue();
            if (data.npc() == null || !data.npc().isValid()) {
                it.remove();
                cleaned++;
            }
        }
        return cleaned;
    }
}
