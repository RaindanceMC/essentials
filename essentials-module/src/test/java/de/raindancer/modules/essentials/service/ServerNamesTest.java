package de.raindancer.modules.essentials.service;

import org.bukkit.Server;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** /list, /where and /seen name a server the way players know it, never by its id. */
class ServerNamesTest {

    private final NetworkWho who = new NetworkWho(mock(Server.class), null, null);

    @Test
    void theIdUntilSomebodySaysOtherwise() {
        assertThat(who.nameOf("beta")).isEqualTo("beta");
    }

    @Test
    void theNameTheServerGivesItself() {
        Map<String, String> names = Map.of("beta", "Hyades", "welcome", "Hyades - Welcome");
        who.named(id -> names.getOrDefault(id, id));
        assertThat(who.nameOf("beta")).isEqualTo("Hyades");
        assertThat(who.nameOf("welcome")).isEqualTo("Hyades - Welcome");
        assertThat(who.nameOf("")).isEmpty();
    }
}
