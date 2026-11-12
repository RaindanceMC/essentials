package de.raindancer.modules.essentials.service;

import de.raindancer.core.content.music.MusicSession;
import de.raindancer.core.content.music.Track;
import de.raindancer.core.content.pack.ResourcePacks;
import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.core.ui.profile.PlayerSwitch;
import de.raindancer.modules.essentials.EssentialsSettings;
import de.raindancer.modules.essentials.JoinMusicSettings;
import de.raindancer.modules.essentials.rules.JoinMusicRule;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * A track for each player as they log on, heard by them alone, once — after their resource pack is on, since the
 * track is in it. {@code /joinmusic} switches it off for them, and stops it if it is playing.
 */
public final class JoinMusicService {

    public static final PlayerSwitch WANTED = new PlayerSwitch("rainsessentials", "join-music", true);

    private final Plugin plugin;
    private final Supplier<JoinMusicSettings> settings;
    private final Supplier<List<Track>> tracks;
    private final ResourcePacks packs;
    private final Messages messages;
    private final LongSupplier clock;
    /** Per player: a newer join (or a stop) makes an older timer end itself. */
    private final Map<UUID, Long> runs = new ConcurrentHashMap<>();
    private final Map<UUID, MusicSession> playing = new ConcurrentHashMap<>();
    private final Map<UUID, Long> startedAt = new ConcurrentHashMap<>();
    private final Map<UUID, Track> playingTrack = new ConcurrentHashMap<>();

    public JoinMusicService(Plugin plugin, Supplier<JoinMusicSettings> settings, Supplier<List<Track>> tracks,
                            ResourcePacks packs, Messages messages, LongSupplier clock) {
        this.plugin = plugin;
        this.settings = settings;
        this.tracks = tracks;
        this.packs = packs;
        this.messages = messages;
        this.clock = clock;
    }

    /** Its own settings are {@link JoinMusicSettings}, read fresh each time. */
    public void settings(EssentialsSettings fresh) {
    }

    public void joined(Player player) {
        List<Track> own = tracks.get();
        if (!JoinMusicRule.plays(settings.get().enabled(), WANTED.isOn(player), !own.isEmpty())) {
            return;
        }
        UUID id = player.getUniqueId();
        long run = clock.getAsLong();
        runs.put(id, run);
        Scheduling.entityTimer(plugin, player, 10, 10, task -> {
            if (!player.isOnline() || !Long.valueOf(run).equals(runs.get(id))) {
                task.cancel();
                return;
            }
            MusicSession session = playing.get(id);
            if (session == null) {
                switch (JoinMusicRule.step(packs.statusOf(id), (clock.getAsLong() - run) / 1000.0)) {
                    case WAIT -> {
                    }
                    case GIVE_UP -> {
                        runs.remove(id);
                        task.cancel();
                    }
                    case PLAY -> start(player, own.get(ThreadLocalRandom.current().nextInt(own.size())));
                }
                return;
            }
            session.tick();
            if (JoinMusicRule.over(startedAt.getOrDefault(id, 0L), clock.getAsLong(), playingTrack.get(id).seconds())) {
                stop(id);
                task.cancel();
            }
        }, () -> stop(id));
    }

    private void start(Player player, Track track) {
        JoinMusicSettings live = settings.get();
        UUID id = player.getUniqueId();
        startedAt.put(id, clock.getAsLong());
        playingTrack.put(id, track);
        playing.put(id, new MusicSession(player, player, List.of(track), true, "", live.volume() / 100f, clock));
        if (live.credit() != null && !live.credit().isBlank()) {
            messages.send(player, "essentials.joinmusic.playing", "credit", live.credit().strip());
        } else {
            messages.send(player, "essentials.joinmusic.playing-plain");
        }
    }

    /** Flips the player's switch; answers whether it is on now. Switched off, what plays stops at once. */
    public boolean toggle(Player player) {
        boolean on = WANTED.toggle(player);
        if (!on) {
            stop(player.getUniqueId());
        }
        return on;
    }

    public void set(Player player, boolean on) {
        WANTED.set(player, on);
        if (!on) {
            stop(player.getUniqueId());
        }
    }

    public boolean isPlaying(UUID player) {
        return playing.containsKey(player);
    }

    public void stop(UUID player) {
        runs.remove(player);
        startedAt.remove(player);
        playingTrack.remove(player);
        MusicSession session = playing.remove(player);
        if (session != null) {
            session.stop();
        }
    }

    public void stopAll() {
        for (UUID player : List.copyOf(runs.keySet())) {
            stop(player);
        }
        for (UUID player : List.copyOf(playing.keySet())) {
            stop(player);
        }
    }
}
