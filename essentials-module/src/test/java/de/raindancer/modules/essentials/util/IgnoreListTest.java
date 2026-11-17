package de.raindancer.modules.essentials.util;

import de.raindancer.core.moderation.vanish.Vanish;
import de.raindancer.core.moderation.vanish.VanishSink;
import de.raindancer.core.ui.chat.Chat;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.essentials.EssentialsSettings;
import de.raindancer.modules.essentials.service.MessagingService;
import de.raindancer.modules.essentials.store.EssentialsStore;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IgnoreListTest {

    private final EssentialsStore store = new EssentialsStore(Path.of("target", "test-ignore-list"));
    private final Messages messages = mock(Messages.class);
    private final MessagingService messaging = new MessagingService(store, messages, mock(Chat.class),
            new Vanish(mock(VanishSink.class)), EssentialsSettings.DEFAULTS);
    private final UUID zed = UUID.randomUUID();
    private final UUID amy = UUID.randomUUID();
    private final Map<UUID, String> names = Map.of(zed, "zed", amy, "Amy");
    private final IgnoreList list = new IgnoreList(store, messaging, messages, names::get);
    private final Player me = player();

    private static Player player() {
        Player who = mock(Player.class);
        when(who.getUniqueId()).thenReturn(UUID.randomUUID());
        return who;
    }

    @Test
    @DisplayName("lists whom you ignore A to Z, ignoring case")
    void sorted() {
        messaging.ignore(me, zed);
        messaging.ignore(me, amy);

        assertThat(list.of(me)).containsExactly(amy, zed);
    }

    @Test
    @DisplayName("an empty list is empty")
    void empty() {
        assertThat(list.of(me)).isEmpty();
    }

    @Test
    @DisplayName("stopping lets them message you and says so, like /unignore")
    void stop() {
        messaging.ignore(me, amy);

        assertThat(list.stop(me, amy)).isTrue();

        assertThat(messaging.isIgnoring(me.getUniqueId(), amy)).isFalse();
        verify(messages).send(me, "essentials.ignore.stopped", "player", "Amy");
    }

    @Test
    @DisplayName("stopping somebody you did not ignore says so")
    void stopNobody() {
        assertThat(list.stop(me, amy)).isFalse();

        verify(messages).send(me, "essentials.ignore.not-ignoring", "player", "Amy");
    }

    @Test
    @DisplayName("starting blocks them and says so, like /ignore")
    void start() {
        assertThat(list.start(me, zed)).isTrue();

        assertThat(messaging.isIgnoring(me.getUniqueId(), zed)).isTrue();
        verify(messages).send(me, "essentials.ignore.started", "player", "zed");
    }

    @Test
    @DisplayName("you cannot ignore yourself")
    void notYourself() {
        assertThat(list.start(me, me.getUniqueId())).isFalse();

        verify(messages, never()).send(me, "essentials.ignore.started", "player", null);
        assertThat(list.of(me)).isEmpty();
    }

    @Test
    @DisplayName("the chooser skips you and whoever is ignored already")
    void notToOffer() {
        messaging.ignore(me, amy);

        assertThat(list.notToOffer(me)).containsExactlyInAnyOrder(amy, me.getUniqueId());
    }
}
