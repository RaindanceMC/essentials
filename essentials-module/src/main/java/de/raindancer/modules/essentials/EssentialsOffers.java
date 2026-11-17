package de.raindancer.modules.essentials;

import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.preference.Preference;
import de.raindancer.core.ui.travel.TravelDoor;
import de.raindancer.modules.essentials.service.JoinMusicService;
import de.raindancer.modules.essentials.service.SpawnService;
import de.raindancer.modules.essentials.util.PermissionNodes;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;

/** What Essentials offers to Core's {@code /preferences} and {@code /travel} pages. */
final class EssentialsOffers {

    private EssentialsOffers() {
    }

    static Preference joinMusic() {
        return Preference.toggle("essentials:join-music", "Join music", "Join music",
                "A short tune when you log in.", JoinMusicService.WANTED);
    }

    static Preference ignored(BiConsumer<Player, Menu> open) {
        return Preference.page("essentials:ignored", "Private messages", "People you ignore",
                "See who cannot message you, and ignore somebody new.", open)
                .needing(PermissionNodes.IGNORE);
    }

    static Preference nickname(BiConsumer<Player, Menu> open) {
        return Preference.page("essentials:nickname", "Nickname", "Your nickname",
                "Set the name others see you by, or take it off.", open)
                .needing(PermissionNodes.NICK);
    }

    static TravelDoor spawnDoor(SpawnService spawn) {
        return TravelDoor.of("essentials:spawn", 60, "Spawn", "Where everybody starts.",
                        (player, parent) -> spawn.go(player))
                .needing(PermissionNodes.SPAWN);
    }
}
