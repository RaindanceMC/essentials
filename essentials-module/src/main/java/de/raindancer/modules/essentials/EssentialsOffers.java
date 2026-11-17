package de.raindancer.modules.essentials;

import de.raindancer.core.ui.preference.Preference;
import de.raindancer.core.ui.travel.TravelDoor;
import de.raindancer.modules.essentials.service.JoinMusicService;
import de.raindancer.modules.essentials.service.SpawnService;
import de.raindancer.modules.essentials.util.PermissionNodes;

/** What Essentials offers to Core's {@code /preferences} and {@code /travel} pages. */
final class EssentialsOffers {

    private EssentialsOffers() {
    }

    static Preference joinMusic() {
        return Preference.toggle("essentials:join-music", "Join music", "Join music",
                "A short tune when you log in.", JoinMusicService.WANTED);
    }

    static TravelDoor spawnDoor(SpawnService spawn) {
        return TravelDoor.of("essentials:spawn", 60, "Spawn", "Where everybody starts.",
                        (player, parent) -> spawn.go(player))
                .needing(PermissionNodes.SPAWN);
    }
}
