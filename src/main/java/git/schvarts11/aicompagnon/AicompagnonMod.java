/*
 *	MCreator note:
 *
 *	If you lock base mod element files, you can edit this file and the proxy files
 *	and they won't get overwritten. If you change your mod package or modid, you
 *	need to apply these changes to this file MANUALLY.
 *
 *
 *	If you do not lock base mod element files in Workspace settings, this file
 *	will be REGENERATED on each build.
 *
 */
package git.schvarts11.aicompagnon;

import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Server;
import git.schvarts11.aicompagnon.command.CompagnonCommand;
import git.schvarts11.aicompagnon.companion.CompanionEntity;
import git.schvarts11.aicompagnon.companion.CompanionManager;
import git.schvarts11.aicompagnon.config.AIConfig;
import git.schvarts11.aicompagnon.ai.AIClient;

public class AicompagnonMod extends JavaPlugin {
	public static JavaPlugin plugin;
	public static Server server;
	private AIClient aiClient;
	private int cleanupTaskId = -1;

	@Override
	public void onEnable() {
		plugin = this;
		server = this.getServer();

		try {
			AIConfig.init(this);
			AIConfig.get().validate();

			this.aiClient = AIClient.create(AIConfig.get());

			PluginCommand compagnonCommand = getCommand("compagnon");
			if (compagnonCommand != null) {
				compagnonCommand.setExecutor(new CompagnonCommand(aiClient));
				compagnonCommand.setTabCompleter(new CompagnonCommand(aiClient));
			} else {
				getLogger().severe("Command 'compagnon' is not registered in paper-plugin.yml");
			}

			getServer().getPluginManager().registerEvents(new CompanionListener(), this);

			cleanupTaskId = getServer().getScheduler().runTaskTimer(this, () -> {
				int cleaned = CompanionManager.get().cleanupOrphaned();
				if (cleaned > 0) {
					getLogger().info("Cleanup task removed " + cleaned + " orphaned companions.");
				}
			}, 6000L, 6000L).getTaskId();

			getLogger().info("Enabled. API endpoint: " + AIConfig.get().apiEndpoint());
			getLogger().info("Model: " + AIConfig.get().model() + " | Tick interval: " + AIConfig.get().tickIntervalTicks() + " ticks");
		} catch (Exception e) {
			getLogger().severe("Failed to enable AI-Compagnon: " + e.getMessage());
			e.printStackTrace();
			setEnabled(false);
		}
	}

	@Override
	public void onDisable() {
		if (cleanupTaskId != -1) {
			getServer().getScheduler().cancelTask(cleanupTaskId);
		}
		CompanionManager.get().unregisterAll();
		getLogger().info("Disabled. Removed all companions.");
	}

	public AIClient getAiClient() {
		return aiClient;
	}

	private class CompanionListener implements Listener {
		@EventHandler
		public void onPlayerQuit(PlayerQuitEvent event) {
			Player player = event.getPlayer();
			if (player == null) return;

			var companions = CompanionManager.get().getByOwner(player.getUniqueId());
			if (companions.isEmpty()) return;

			if (AIConfig.get().removeOnOwnerQuit()) {
				for (var companion : companions) {
					CompanionManager.get().unregister(companion.companionId());
				}
				getLogger().info("Removed " + companions.size() + " companion(s) for quitting player " + player.getName());
			} else {
				for (var companion : companions) {
					if (companion.npc() != null && companion.npc().isValid()) {
						companion.npc().getPathfinder().stopPathfinding();
					}
				}
				getLogger().info("Player " + player.getName() + " quit. Companions are now orphaned.");
			}
		}
	}
}
