package git.schvarts11.aicompagnon.ai;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Objects;

/**
 * Structured action produced by the AI and executed by the ActionExecutor.
 */
public sealed interface Action {

    record MoveTo(double x, double y, double z) implements Action {
        public MoveTo {
            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
                throw new IllegalArgumentException("MoveTo coordinates must not be NaN");
            }
        }

        public Location toLocation(org.bukkit.World world) {
            return new Location(world, x, y, z);
        }
    }

    record Follow(String entityName) implements Action {
        public Follow {
            if (entityName == null || entityName.isBlank()) {
                throw new IllegalArgumentException("Follow requires a non-empty entityName");
            }
        }
    }

    record LookAt(double x, double y, double z) implements Action {
        public LookAt {
            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
                throw new IllegalArgumentException("LookAt coordinates must not be NaN");
            }
        }

        public Location toLocation(org.bukkit.World world) {
            return new Location(world, x, y, z);
        }
    }

    record Say(String message) implements Action {
        public Say {
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("Say requires a non-empty message");
            }
        }
    }

    record Attack(String entityName) implements Action {
        public Attack {
            if (entityName == null || entityName.isBlank()) {
                throw new IllegalArgumentException("Attack requires a non-empty entityName");
            }
        }
    }

    record Wander() implements Action {
        public static final Wander INSTANCE = new Wander();
    }

    record Wait(String reason) implements Action {
        public Wait {
            if (reason == null) {
                throw new IllegalArgumentException("Wait reason must not be null");
            }
        }

        public Wait() {
            this("unspecified");
        }
    }

    static Wait waitAction() {
        return new Wait("unspecified");
    }

    static Wait waitAction(String reason) {
        return new Wait(Objects.requireNonNullElse(reason, "unspecified"));
    }
}
