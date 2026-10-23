package de.raindancer.modules.essentials.listener;

import de.raindancer.modules.essentials.EssentialsServices;
import de.raindancer.modules.essentials.rules.GameModeRule;
import de.raindancer.core.platform.rule.Verdict;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

/**
 * Holds vanilla {@code /gamemode} and the F3+F4 switcher to {@link GameModeRule}. The command is checked as it is
 * typed, because the change event that follows names only the target, not who gave the order.
 */
public final class GameModeListener implements IEssentialsListener {

    private final EssentialsServices services;
    private final GameModeRule rule = new GameModeRule();

    public GameModeListener(EssentialsServices services) {
        this.services = services;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        GameModeRule.wanted(event.getMessage()).ifPresent(mode -> {
            if (refuse(event.getPlayer(), mode)) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSwitcher(PlayerGameModeChangeEvent event) {
        if (event.getCause() == PlayerGameModeChangeEvent.Cause.GAMEMODE_SWITCHER
                && refuse(event.getPlayer(), event.getNewGameMode())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void forget(java.util.UUID player) {
        // Holds nothing per player.
    }

    private boolean refuse(Player player, GameMode mode) {
        Verdict verdict = rule.judge(new GameModeRule.Request(player::hasPermission, mode));
        if (verdict.isAllowed()) {
            return false;
        }
        services.messages().send(player, verdict.reason(), "mode", mode.name().toLowerCase(java.util.Locale.ROOT));
        return true;
    }
}
