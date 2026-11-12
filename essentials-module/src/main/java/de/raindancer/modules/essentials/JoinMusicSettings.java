package de.raindancer.modules.essentials;

import de.raindancer.core.data.settings.Describe;
import de.raindancer.core.data.settings.In;
import de.raindancer.core.data.settings.Key;
import de.raindancer.core.data.settings.Range;
import de.raindancer.core.data.settings.Settings;
import de.raindancer.core.data.settings.Title;
import de.raindancer.core.data.settings.Topic;
import org.bukkit.Material;

/** Music when somebody logs on — only they hear it, and /joinmusic switches it off for them. */
@Settings(id = "joinmusic", topics = {
        @Topic(path = "joinmusic", title = "Music on logging on", icon = Material.JUKEBOX,
                description = "A track from plugins/RainsEssentials/music plays to each player as they log on."),
})
public record JoinMusicSettings(

        @In("joinmusic") @Title("Music on logging on")
        @Describe("Whether a track plays to everybody as they log on. Every .ogg (Vorbis) in "
                + "plugins/RainsEssentials/music is one, picked at random; new tracks come with the next restart. "
                + "Each player can switch it off for themselves with /joinmusic.")
        @Key("join-music.enabled") boolean enabled,

        @In("joinmusic") @Title("Volume (%)")
        @Range(min = 1, max = 100)
        @Key("join-music.volume") int volume,

        @In("joinmusic") @Title("Credit")
        @Describe("A line under the music when it starts, e.g. the track and its artist when the licence asks "
                + "for it. Empty: none.")
        @Key("join-music.credit") String credit) {

    public static final JoinMusicSettings DEFAULTS = new JoinMusicSettings(false, 70, "");
}
