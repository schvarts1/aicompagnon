package git.schvarts11.aicompagnon.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import git.schvarts11.aicompagnon.AicompagnonMod;
import git.schvarts11.aicompagnon.companion.CompanionData;
import git.schvarts11.aicompagnon.companion.CompanionEntity;
import git.schvarts11.aicompagnon.companion.CompanionManager;
import git.schvarts11.aicompagnon.config.AIConfig;
import git.schvarts11.aicompagnon.ai.AIClient;

import java.util.*;
import java.util.stream.Collectors;

public class CompagnonCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_SPAWN = "aicompagnon.spawn";
    private static final String PERM_ADMIN = "aicompagnon.admin";

    private final AIClient aiClient;

    public CompagnonCommand(AIClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        return switch (sub) {
            case "spawn" -> handleSpawn(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender, args);
            case "info" -> handleInfo(sender, args);
            case "reload" -> handleReload(sender);
            default -> {
                sender.sendMessage("§cUnknown subcommand. Use /compagnon for help.");
                yield true;
            }
        };
    }

    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (!hasPerm(sender, PERM_SPAWN)) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can spawn companions.");
            return true;
        }

        String rawName = args.length >= 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : null;
        String name = (rawName == null || rawName.isBlank())
                ? "Companion-" + UUID.randomUUID().toString().substring(0, 6)
                : rawName.trim();

        int current = CompanionManager.get().getByOwner(player.getUniqueId()).size();
        if (current >= AIConfig.get().maxCompanionsPerPlayer()) {
            sender.sendMessage("§cYou have reached the maximum number of companions (" + AIConfig.get().maxCompanionsPerPlayer() + ").");
            return true;
        }

        CompanionEntity companion = CompanionEntity.spawn(player, name, player.getLocation(), aiClient);
        if (companion == null) {
            sender.sendMessage("§cFailed to spawn companion. Check console for details.");
            return true;
        }

        sender.sendMessage("§a[AI-Compagnon] §aSpawned companion §f" + name + " §awith ID §f" + companion.data().companionId());
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!hasPerm(sender, PERM_ADMIN)) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /compagnon remove <id|all>");
            return true;
        }

        String target = args[1].toLowerCase();
        if ("all".equalsIgnoreCase(target)) {
            int count = CompanionManager.get().size();
            CompanionManager.get().unregisterAll();
            sender.sendMessage("§a[AI-Compagnon] §aRemoved all companions. Count: §f" + count);
            return true;
        }

        try {
            UUID id = UUID.fromString(target);
            CompanionData data = CompanionManager.get().getById(id).orElse(null);
            if (data == null) {
                sender.sendMessage("§cCompanion not found: §f" + target);
                return true;
            }
            CompanionManager.get().unregister(id);
            sender.sendMessage("§a[AI-Compagnon] §aRemoved companion §f" + data.name() + " §a(ID: §f" + id + "§a)");
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid companion ID: §f" + target);
        }
        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        if (!hasPerm(sender, PERM_SPAWN)) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        Collection<CompanionData> all;
        if (args.length >= 2) {
            String targetName = args[1];
            Player target = AicompagnonMod.server.getPlayer(targetName);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: §f" + targetName);
                return true;
            }
            all = CompanionManager.get().getByOwner(target.getUniqueId());
        } else {
            all = CompanionManager.get().getAll();
        }

        if (all.isEmpty()) {
            sender.sendMessage("§7No active companions.");
            return true;
        }

        sender.sendMessage("§e--- Active Companions (" + all.size() + ") ---");
        for (CompanionData data : all) {
            String ownerName = AicompagnonMod.server.getOfflinePlayer(data.ownerUuid()).getName();
            sender.sendMessage(String.format("§f- %s §7(ID: %s | Owner: %s | Ticks: %d)",
                    data.name(), data.companionId(), ownerName, data.tickCount()));
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!hasPerm(sender, PERM_SPAWN)) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /compagnon info <id>");
            return true;
        }

        try {
            UUID id = UUID.fromString(args[1]);
            CompanionData data = CompanionManager.get().getById(id).orElse(null);
            if (data == null) {
                sender.sendMessage("§cCompanion not found: §f" + args[1]);
                return true;
            }

            String ownerName = AicompagnonMod.server.getOfflinePlayer(data.ownerUuid()).getName();
            sender.sendMessage("§e--- Companion Info ---");
            sender.sendMessage("§fName: §r" + data.name());
            sender.sendMessage("§fID: §r" + data.companionId());
            sender.sendMessage("§fOwner: §r" + ownerName);
            sender.sendMessage("§fSpawn: §r" + String.format("%.1f, %.1f, %.1f", data.spawnLocation().getX(), data.spawnLocation().getY(), data.spawnLocation().getZ()));
            sender.sendMessage("§fTicks: §r" + data.tickCount());
            sender.sendMessage("§fLast tick: §r" + new Date(data.lastTickTime()).toInstant().toString());
            if (!data.history().isEmpty()) {
                sender.sendMessage("§fLast actions:");
                int limit = Math.min(3, data.history().size());
                for (int i = data.history().size() - limit; i < data.history().size(); i++) {
                    var entry = data.history().get(i);
                    sender.sendMessage(String.format("  §7- %s -> %s", entry.actionType(), entry.result()));
                }
            }
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid companion ID: §f" + args[1]);
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!hasPerm(sender, PERM_ADMIN)) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        try {
            boolean ok = AIConfig.reload();
            if (ok) {
                AIConfig.get().validate();
                sender.sendMessage("§a[AI-Compagnon] §aConfig reloaded successfully.");
            } else {
                sender.sendMessage("§c[AI-Compagnon] §cConfig reload failed.");
            }
        } catch (Exception e) {
            sender.sendMessage("§c[AI-Compagnon] §cError reloading config: " + e.getMessage());
            AicompagnonMod.plugin.getLogger().severe("Failed to reload config: " + e.getMessage());
        }
        return true;
    }

    private boolean hasPerm(CommandSender sender, String perm) {
        if (sender.isOp()) {
            return true;
        }
        return sender.hasPermission(perm);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§e--- AI Compagnon Help ---");
        sender.sendMessage("§f/compagnon spawn [name] §7- Spawn a companion");
        sender.sendMessage("§f/compagnon remove <id|all> §7- Remove a companion");
        sender.sendMessage("§f/compagnon list [player] §7- List active companions");
        sender.sendMessage("§f/compagnon info <id> §7- Companion details");
        sender.sendMessage("§f/compagnon reload §7- Reload config");
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(List.of("spawn", "remove", "list", "info", "reload"), args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if ("remove".equals(sub) || "info".equals(sub)) {
                List<String> ids = CompanionManager.get().getAll().stream()
                        .map(d -> d.companionId().toString())
                        .collect(Collectors.toList());
                List<String> options = new ArrayList<>(ids);
                options.add("all");
                return filter(options, args[1]);
            }
            if ("list".equals(sub)) {
                List<String> playerNames = AicompagnonMod.server.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
                return filter(playerNames, args[1]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        if (input == null || input.isBlank()) {
            return options;
        }
        String lower = input.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String s : options) {
            if (s.toLowerCase().startsWith(lower)) {
                out.add(s);
            }
        }
        return out;
    }
}
