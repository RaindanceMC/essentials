package de.raindancer.modules.essentials.service;

import de.raindancer.core.moderation.vanish.Vanish;
import de.raindancer.core.moderation.vanish.VanishSink;
import de.raindancer.core.ui.chat.Chat;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.essentials.EssentialsSettings;
import de.raindancer.modules.essentials.store.EssentialsStore;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessagingServiceTest {

    private final EssentialsStore store = new EssentialsStore(Path.of("target", "test-essentials"));
    private final Messages messages = mock(Messages.class);
    private final Chat chat = mock(Chat.class);
    private final Vanish vanish = new Vanish(mock(VanishSink.class));
    private final MessagingService service =
            new MessagingService(store, messages, chat, vanish, EssentialsSettings.DEFAULTS);

    private Player player(String name) {
        Player who = mock(Player.class);
        when(who.getUniqueId()).thenReturn(UUID.randomUUID());
        when(who.getName()).thenReturn(name);
        return who;
    }

    @Nested
    @DisplayName("sending")
    class Sending {

        @Test
        @DisplayName("delivers to both, and remembers each as the other's reply target")
        void delivers() {
            Player from = player("Tom");
            Player to = player("Alex");

            boolean sent = service.send(from, to, "hi");

            assertThat(sent).isTrue();
            assertThat(service.replyTarget(to.getUniqueId())).contains(from.getUniqueId());
            assertThat(service.replyTarget(from.getUniqueId())).contains(to.getUniqueId());
        }

        @Test
        @DisplayName("refuses a message to yourself")
        void refusesYourself() {
            Player who = player("Tom");

            boolean sent = service.send(who, who, "hi");

            assertThat(sent).isFalse();
        }

        @Test
        @DisplayName("an ignored sender's message never arrives")
        void ignoredNeverArrives() {
            Player from = player("Tom");
            Player to = player("Alex");
            service.ignore(to, from.getUniqueId());

            boolean sent = service.send(from, to, "hi");

            assertThat(sent).isFalse();
        }

        @Test
        @DisplayName("a vanished target never receives it, and never gives away that they are hidden")
        void vanishedNeverArrives() {
            Player from = player("Tom");
            Player to = player("Alex");
            vanish.vanish(to.getUniqueId());

            boolean sent = service.send(from, to, "hi");

            assertThat(sent).isFalse();
        }

        @Test
        @DisplayName("staff who may see vanished players can still message one")
        void staffCanReachAVanishedTarget() {
            Player from = player("Mod");
            Player to = player("Alex");
            vanish.vanish(to.getUniqueId());
            vanish.maySeeVanished(from.getUniqueId(), true);

            boolean sent = service.send(from, to, "hi");

            assertThat(sent).isTrue();
        }

        @Test
        @DisplayName("a vanished sender may still message somebody who cannot see them")
        void vanishedSenderCanStillWrite() {
            Player from = player("Mod");
            Player to = player("Alex");
            vanish.vanish(from.getUniqueId());

            boolean sent = service.send(from, to, "hi");

            assertThat(sent).isTrue();
        }
    }

    @Nested
    @DisplayName("ignoring")
    class Ignoring {

        @Test
        @DisplayName("blocking, then unblocking, leaves nobody ignored")
        void blockThenUnblock() {
            Player who = player("Tom");
            UUID target = UUID.randomUUID();

            assertThat(service.ignore(who, target)).isTrue();
            assertThat(service.isIgnoring(who.getUniqueId(), target)).isTrue();

            assertThat(service.stopIgnoring(who, target)).isTrue();
            assertThat(service.isIgnoring(who.getUniqueId(), target)).isFalse();
        }

        @Test
        @DisplayName("blocking twice changes nothing the second time")
        void blockingTwiceIsNotAChange() {
            Player who = player("Tom");
            UUID target = UUID.randomUUID();

            assertThat(service.ignore(who, target)).isTrue();
            assertThat(service.ignore(who, target)).isFalse();
        }
    }

    @Test
    @DisplayName("forgetting a player drops reply-target state without touching who they ignore")
    void forgetDropsSessionStateOnly() {
        Player from = player("Tom");
        Player to = player("Alex");
        service.send(from, to, "hi");
        service.ignore(to, from.getUniqueId());

        service.forget(from.getUniqueId());

        assertThat(service.replyTarget(from.getUniqueId())).isEmpty();
        assertThat(service.isIgnoring(to.getUniqueId(), from.getUniqueId())).isTrue();
    }

    @Nested
    @DisplayName("the shared store")
    class OffTheServerThread {

        private final Path folder = Path.of("target", "test-essentials-deferred", UUID.randomUUID().toString());
        private final EssentialsStore deferredStore = new EssentialsStore(folder);
        private final List<Runnable> later = new ArrayList<>();
        private final MessagingService deferring = new MessagingService(deferredStore, messages, chat, vanish,
                later::add, EssentialsSettings.DEFAULTS);

        private boolean storedIgnoring(UUID who, UUID target) {
            EssentialsStore fresh = new EssentialsStore(folder);
            fresh.load();
            return fresh.isIgnoring(who, target);
        }

        @Test
        @DisplayName("blocking answers at once and is written on the storage thread")
        void ignoreWritesLater() {
            Player who = player("Tom");
            UUID target = UUID.randomUUID();

            assertThat(deferring.ignore(who, target)).isTrue();

            assertThat(deferring.isIgnoring(who.getUniqueId(), target)).isTrue();
            assertThat(storedIgnoring(who.getUniqueId(), target)).isFalse();
            later.forEach(Runnable::run);
            assertThat(storedIgnoring(who.getUniqueId(), target)).isTrue();
        }

        @Test
        @DisplayName("unblocking too")
        void stopIgnoringWritesLater() {
            Player who = player("Tom");
            UUID target = UUID.randomUUID();
            deferring.ignore(who, target);
            later.forEach(Runnable::run);
            later.clear();

            assertThat(deferring.stopIgnoring(who, target)).isTrue();

            assertThat(storedIgnoring(who.getUniqueId(), target)).isTrue();
            later.forEach(Runnable::run);
            assertThat(storedIgnoring(who.getUniqueId(), target)).isFalse();
        }
    }
}
