package de.raindancer.modules.essentials.listener;

import de.raindancer.modules.essentials.service.JoinMusicService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/** Starts the music on logging on, and forgets it when they leave. */
public final class JoinMusicListener implements IEssentialsListener {

    private final JoinMusicService music;

    public JoinMusicListener(JoinMusicService music) {
        this.music = music;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        music.joined(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        forget(event.getPlayer().getUniqueId());
    }

    @Override
    public void forget(UUID player) {
        music.stop(player);
    }
}
