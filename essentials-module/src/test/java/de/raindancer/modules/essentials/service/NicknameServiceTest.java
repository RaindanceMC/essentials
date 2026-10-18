package de.raindancer.modules.essentials.service;

import de.raindancer.core.moderation.audit.Audit;
import de.raindancer.core.moderation.punishment.PunishmentKind;
import de.raindancer.core.moderation.punishment.Punishments;
import de.raindancer.core.ui.chat.Chat;
import de.raindancer.core.ui.identity.Identities;
import de.raindancer.core.ui.identity.Nicknames;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.essentials.EssentialsSettings;
import de.raindancer.modules.essentials.store.EssentialsStore;
import de.raindancer.modules.essentials.store.NicknameBlocklist;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import de.raindancer.modules.essentials.util.PermissionNodes;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The blocklist half of setting a nickname.
 *
 * <h2>Why the ban assertions do not mock {@code ModerationIntegration}</h2>
 * They do not need to: {@code ModerationIntegration.banOneDay} tries {@code Bukkit.getServicesManager()}
 * first and catches everything that lookup can throw, falling back to the {@link Punishments} passed
 * in here — and a unit test JVM has no Bukkit server at all, so that lookup always fails and the
 * fallback always runs. That is the real behaviour a server with no moderation plugin installed gets
 * too, which is exactly the case worth pinning.
 */
class NicknameServiceTest {

    private final EssentialsStore store = new EssentialsStore(Path.of("target", "test-nick-service", UUID.randomUUID().toString()));
    private final Identities identities = mock(Identities.class);
    private final Messages messages = mock(Messages.class);
    private final Chat chat = mock(Chat.class);
    private final Server server = mock(Server.class);
    private final Punishments punishments = mock(Punishments.class);
    private final Audit audit = mock(Audit.class);
    private final Nicknames directory = mock(Nicknames.class);

    private NicknameBlocklist blocklistOf(Path folder, String yaml) {
        try {
            Path file = folder.resolve("blocklist.yml");
            Files.writeString(file, yaml);
            NicknameBlocklist blocklist = new NicknameBlocklist(file, () -> null);
            blocklist.load();
            return blocklist;
        } catch (IOException failure) {
            throw new AssertionError("could not write a test blocklist", failure);
        }
    }

    private NicknameService serviceWith(NicknameBlocklist blocklist) {
        return serviceWith(blocklist, EssentialsSettings.DEFAULTS);
    }

    private NicknameService serviceWith(NicknameBlocklist blocklist, EssentialsSettings settings) {
        return new NicknameService(store, blocklist, identities, directory, messages, chat, server,
                punishments, audit, (who, task) -> task.run(), Runnable::run, settings);
    }

    private Player player(String name) {
        Player who = mock(Player.class);
        when(who.getUniqueId()).thenReturn(UUID.randomUUID());
        when(who.getName()).thenReturn(name);
        when(identities.nametag(any(), any())).thenReturn(Component.empty());
        when(identities.chatName(any(), any())).thenReturn(Component.empty());
        return who;
    }

    @Nested
    @DisplayName("a reported-only section match")
    class ReportedOnly {

        @Test
        @DisplayName("is refused, and never bans")
        void refusedButNotBanned(@TempDir Path folder) {
            NicknameBlocklist blocklist = blocklistOf(folder, """
                    politicians:
                      enabled: true
                      action: report
                      names:
                        - forbidden name
                    """);
            NicknameService service = serviceWith(blocklist);
            Player who = player("Tom");
            when(server.getOnlinePlayers()).thenReturn(List.of());

            boolean set = service.set(who, "Forbidden Name", false).join();

            assertThat(set).isFalse();
            assertThat(store.nicknameOf(who.getUniqueId())).isEmpty();
            verify(punishments, never()).punish(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("still writes down an audit entry")
        void stillAudited(@TempDir Path folder) {
            NicknameBlocklist blocklist = blocklistOf(folder, """
                    politicians:
                      enabled: true
                      action: report
                      names:
                        - forbidden name
                    """);
            NicknameService service = serviceWith(blocklist);
            Player who = player("Tom");
            when(server.getOnlinePlayers()).thenReturn(List.of());

            service.set(who, "Forbidden Name", false).join();

            verify(audit).record(org.mockito.ArgumentMatchers
                    .<de.raindancer.core.moderation.audit.AuditEntry.Builder>any());
        }

        @Test
        @DisplayName("a section switched off in the file blocks nothing")
        void disabledSectionBlocksNothing(@TempDir Path folder) {
            NicknameBlocklist blocklist = blocklistOf(folder, """
                    politicians:
                      enabled: false
                      action: report
                      names:
                        - forbidden name
                    """);
            NicknameService service = serviceWith(blocklist);
            Player who = player("Tom");

            boolean set = service.set(who, "Forbidden Name", false).join();

            assertThat(set).isTrue();
        }
    }

    @Nested
    @DisplayName("a report-and-ban section match")
    class ReportAndBan {

        @Test
        @DisplayName("is refused and bans for exactly one day")
        void refusedAndBanned(@TempDir Path folder) {
            NicknameBlocklist blocklist = blocklistOf(folder, """
                    hate-figures:
                      enabled: true
                      action: ban
                      names:
                        - severe name
                    """);
            NicknameService service = serviceWith(blocklist);
            Player who = player("Tom");
            when(server.getOnlinePlayers()).thenReturn(List.of());

            boolean set = service.set(who, "Severe Name", false).join();

            assertThat(set).isFalse();
            verify(punishments).punish(eq(who.getUniqueId()), eq(PunishmentKind.BAN), eq(null),
                    any(String.class), eq(Duration.ofDays(1)));
        }

        @Test
        @DisplayName("a ban is never issued quietly — it is audited and told to staff exactly like a plain report")
        void aBanIsAlwaysAlsoAReport(@TempDir Path folder) {
            NicknameBlocklist blocklist = blocklistOf(folder, """
                    hate-figures:
                      enabled: true
                      action: ban
                      names:
                        - severe name
                    """);
            NicknameService service = serviceWith(blocklist);
            Player who = player("Tom");
            Player staff = player("Staffer");
            when(staff.hasPermission(de.raindancer.modules.essentials.util.PermissionNodes.STAFF_NOTIFY))
                    .thenReturn(true);
            org.mockito.Mockito.doReturn(List.of(staff)).when(server).getOnlinePlayers();

            service.set(who, "Severe Name", false).join();

            verify(audit).record(org.mockito.ArgumentMatchers
                    .<de.raindancer.core.moderation.audit.AuditEntry.Builder>any());
            verify(chat).broadcast(org.mockito.ArgumentMatchers.eq(List.of(staff)),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.<net.kyori.adventure.text.minimessage.tag.resolver.TagResolver>any(),
                    org.mockito.ArgumentMatchers.<net.kyori.adventure.text.minimessage.tag.resolver.TagResolver>any());
        }

        @Test
        @DisplayName("matches case-insensitively and ignores colour markup")
        void matchesRegardlessOfCaseOrColour(@TempDir Path folder) {
            NicknameBlocklist blocklist = blocklistOf(folder, """
                    hate-figures:
                      enabled: true
                      action: ban
                      names:
                        - blocked
                    """);
            NicknameService service = serviceWith(blocklist);
            Player who = player("Tom");
            when(server.getOnlinePlayers()).thenReturn(List.of());

            boolean set = service.set(who, "<red>BLOCKED</red>", false).join();

            assertThat(set).isFalse();
            verify(punishments).punish(any(), eq(PunishmentKind.BAN), any(), any(), any());
        }

        @Test
        @DisplayName("beats a report match on a different section for the same name")
        void banBeatsReport(@TempDir Path folder) {
            NicknameBlocklist blocklist = blocklistOf(folder, """
                    politicians:
                      enabled: true
                      action: report
                      names:
                        - both
                    hate-figures:
                      enabled: true
                      action: ban
                      names:
                        - both
                    """);
            NicknameService service = serviceWith(blocklist);
            Player who = player("Tom");
            when(server.getOnlinePlayers()).thenReturn(List.of());

            service.set(who, "both", false).join();

            verify(punishments).punish(any(), eq(PunishmentKind.BAN), any(), any(), any());
        }
    }

    @Test
    @DisplayName("a name in no section is never reported or banned")
    void unblockedNameIsLeftAlone(@TempDir Path folder) {
        NicknameBlocklist blocklist = blocklistOf(folder, """
                politicians:
                  enabled: true
                  action: report
                  names:
                    - somebody else
                """);
        NicknameService service = serviceWith(blocklist);
        Player who = player("Tom");

        boolean set = service.set(who, "Foxy", false).join();

        assertThat(set).isTrue();
        assertThat(store.nicknameOf(who.getUniqueId())).contains("Foxy");
        verify(punishments, never()).punish(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("blocked takes priority over the length limit — still banned even though it is also too long")
    void blockedBeatsTooLong(@TempDir Path folder) {
        NicknameBlocklist blocklist = blocklistOf(folder, """
                hate-figures:
                  enabled: true
                  action: ban
                  names:
                    - waytoolongname
                """);
        EssentialsSettings shortLimit = EssentialsSettings.DEFAULTS.withNicknameMaxLength(4);
        NicknameService service = serviceWith(blocklist, shortLimit);
        Player who = player("Tom");
        when(server.getOnlinePlayers()).thenReturn(List.of());

        service.set(who, "waytoolongname", false).join();

        verify(punishments).punish(any(), eq(PunishmentKind.BAN), any(), any(), any());
    }

    @Nested
    @DisplayName("the tablist and the nametag")
    class EverywhereElse {

        @Test
        @DisplayName("are handed the nickname, as plain text, when that is switched on")
        void handedOver(@TempDir Path folder) {
            NicknameService service = serviceWith(blocklistOf(folder, "{}"));
            Player who = player("Tom");
            when(server.getOnlinePlayers()).thenReturn(List.of());

            service.set(who, "<red>Rain", false).join();

            verify(identities).setNickname(who.getUniqueId(), "Rain");
        }

        @Test
        @DisplayName("are not, when it is switched off")
        void notWhenOff(@TempDir Path folder) {
            NicknameService service = serviceWith(blocklistOf(folder, "{}"),
                    EssentialsSettings.DEFAULTS.withNicknameShownEverywhere(false));
            Player who = player("Tom");
            when(server.getOnlinePlayers()).thenReturn(List.of());

            service.set(who, "Rain", false).join();

            verify(identities, never()).setNickname(eq(who.getUniqueId()), eq("Rain"));
            verify(identities).setNickname(who.getUniqueId(), null);
        }
    }

    @Nested
    @DisplayName("the shared nickname directory")
    class Directory {

        @Test
        @DisplayName("a nickname somebody sets is written to Core's directory, with the colour stripped")
        void selfSetIsRemembered(@TempDir Path folder) {
            NicknameService service = serviceWith(blocklistOf(folder, "{}"));
            Player who = player("Tom");

            service.set(who, "<red>Foxy", false).join();

            verify(directory).remember(who.getUniqueId(), "Foxy");
        }

        @Test
        @DisplayName("clearing it takes it out of the directory too")
        void selfClearIsForgotten(@TempDir Path folder) {
            NicknameService service = serviceWith(blocklistOf(folder, "{}"));
            Player who = player("Tom");
            service.set(who, "Foxy", false).join();

            service.clear(who);

            verify(directory).clear(who.getUniqueId());
        }

        @Test
        @DisplayName("syncing hands over every stored nickname, so old ones become resolvable")
        void syncHandsOverEverything(@TempDir Path folder) {
            NicknameService service = serviceWith(blocklistOf(folder, "{}"));
            UUID first = UUID.randomUUID();
            UUID second = UUID.randomUUID();
            store.setNickname(first, "<gold>Goldie");
            store.setNickname(second, "Plain");

            int synced = service.syncDirectory();

            assertThat(synced).isEqualTo(2);
            verify(directory).remember(first, "Goldie");
            verify(directory).remember(second, "Plain");
        }
    }

    @Nested
    @DisplayName("an admin setting somebody else's")
    class Admin {

        private CommandSender admin(String... nodes) {
            CommandSender sender = mock(CommandSender.class);
            for (String node : nodes) {
                when(sender.hasPermission(node)).thenReturn(true);
            }
            return sender;
        }

        private OfflinePlayer offline(String name) {
            OfflinePlayer gone = mock(OfflinePlayer.class);
            when(gone.getUniqueId()).thenReturn(UUID.randomUUID());
            when(gone.getName()).thenReturn(name);
            return gone;
        }

        @Test
        @DisplayName("is refused without the others node, and nothing is stored")
        void needsTheNode(@TempDir Path folder) {
            NicknameService service = serviceWith(blocklistOf(folder, "{}"));
            OfflinePlayer target = offline("Tom");

            boolean set = service.set(admin(), target, "Foxy").join();

            assertThat(set).isFalse();
            assertThat(store.nicknameOf(target.getUniqueId())).isEmpty();
            verify(messages).send(any(CommandSender.class), eq("essentials.nick.not-others"), any(Object[].class));
        }

        @Test
        @DisplayName("works on somebody who is offline: stored, remembered, audited, nothing redrawn")
        void offlineTarget(@TempDir Path folder) {
            NicknameService service = serviceWith(blocklistOf(folder, "{}"));
            OfflinePlayer target = offline("Tom");
            when(target.getPlayer()).thenReturn(null);

            boolean set = service.set(admin(PermissionNodes.NICK_OTHERS), target, "Foxy").join();

            assertThat(set).isTrue();
            assertThat(store.nicknameOf(target.getUniqueId())).contains("Foxy");
            verify(directory).remember(target.getUniqueId(), "Foxy");
            verify(audit).record(org.mockito.ArgumentMatchers
                    .<de.raindancer.core.moderation.audit.AuditEntry.Builder>any());
            verify(identities, never()).setNickname(any(), any());
        }

        @Test
        @DisplayName("on somebody online redraws them and tells them who did it")
        void onlineTarget(@TempDir Path folder) {
            NicknameService service = serviceWith(blocklistOf(folder, "{}"));
            Player target = player("Tom");
            when(target.getPlayer()).thenReturn(target);

            service.set(admin(PermissionNodes.NICK_OTHERS), target, "Foxy").join();

            verify(identities).setNickname(target.getUniqueId(), "Foxy");
            verify(messages).send(eq(target), eq("essentials.nick.set-by-staff"), any(Object[].class));
        }

        @Test
        @DisplayName("a real player's name is refused, even for an admin, because that is impersonation")
        void impersonationRefused(@TempDir Path folder) {
            NicknameService service = serviceWith(blocklistOf(folder, "{}"));
            OfflinePlayer target = offline("Tom");
            OfflinePlayer notch = offline("Notch");
            when(server.getOfflinePlayerIfCached("Notch")).thenReturn(notch);

            boolean set = service.set(admin(PermissionNodes.NICK_OTHERS, PermissionNodes.NICK_BYPASS_BLOCKLIST),
                    target, "Notch").join();

            assertThat(set).isFalse();
            verify(messages).send(any(CommandSender.class), eq("essentials.nick.taken"), any(Object[].class));
        }

        @Test
        @DisplayName("the target's own real name is not impersonation")
        void ownNameIsFine(@TempDir Path folder) {
            NicknameService service = serviceWith(blocklistOf(folder, "{}"));
            OfflinePlayer target = offline("Tom");
            when(server.getOfflinePlayerIfCached("tom")).thenReturn(target);

            assertThat(service.set(admin(PermissionNodes.NICK_OTHERS), target, "tom").join()).isTrue();
        }

        @Test
        @DisplayName("a nickname somebody else already has is refused")
        void nicknameOwnedByAnother(@TempDir Path folder) {
            NicknameService service = serviceWith(blocklistOf(folder, "{}"));
            OfflinePlayer target = offline("Tom");
            when(directory.ownersOf("Foxy")).thenReturn(List.of(UUID.randomUUID()));

            boolean set = service.set(admin(PermissionNodes.NICK_OTHERS), target, "Foxy").join();

            assertThat(set).isFalse();
            verify(messages).send(any(CommandSender.class), eq("essentials.nick.nick-taken"), any(Object[].class));
        }

        @Test
        @DisplayName("a nickname that is in the shared store but not yet in Core's directory is refused too")
        void nicknameOwnedOnAnotherServer(@TempDir Path folder) {
            NicknameService service = serviceWith(blocklistOf(folder, "{}"));
            OfflinePlayer target = offline("Tom");
            store.setNickname(UUID.randomUUID(), "<gold>Foxy</gold>");
            store.flush();

            boolean set = service.set(admin(PermissionNodes.NICK_OTHERS), target, "foxy").join();

            assertThat(set).isFalse();
            assertThat(store.nicknameOf(target.getUniqueId())).isEmpty();
            verify(messages).send(any(CommandSender.class), eq("essentials.nick.nick-taken"), any(Object[].class));
        }

        @Test
        @DisplayName("their own nickname, typed again, is not 'taken'")
        void ownNicknameAgain(@TempDir Path folder) {
            NicknameService service = serviceWith(blocklistOf(folder, "{}"));
            OfflinePlayer target = offline("Tom");
            UUID owner = target.getUniqueId();
            when(directory.ownersOf("Foxy")).thenReturn(List.of(owner));

            assertThat(service.set(admin(PermissionNodes.NICK_OTHERS), target, "Foxy").join()).isTrue();
        }

        @Test
        @DisplayName("a blocklisted name is refused for an admin without bypass, and the target is never punished for it")
        void blocklistWithoutBypass(@TempDir Path folder) {
            NicknameBlocklist blocklist = blocklistOf(folder, """
                    hate-figures:
                      enabled: true
                      action: ban
                      names:
                        - severe name
                    """);
            NicknameService service = serviceWith(blocklist);
            OfflinePlayer target = offline("Tom");

            boolean set = service.set(admin(PermissionNodes.NICK_OTHERS), target, "Severe Name").join();

            assertThat(set).isFalse();
            verify(punishments, never()).punish(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("with the bypass node, it goes through")
        void blocklistWithBypass(@TempDir Path folder) {
            NicknameBlocklist blocklist = blocklistOf(folder, """
                    politicians:
                      enabled: true
                      action: report
                      names:
                        - forbidden name
                    """);
            NicknameService service = serviceWith(blocklist);
            OfflinePlayer target = offline("Tom");

            assertThat(service.set(admin(PermissionNodes.NICK_OTHERS, PermissionNodes.NICK_BYPASS_BLOCKLIST),
                    target, "Forbidden Name").join()).isTrue();
        }

        @Test
        @DisplayName("clearing somebody else's removes it from the store and the directory, and is audited")
        void clearOther(@TempDir Path folder) {
            NicknameService service = serviceWith(blocklistOf(folder, "{}"));
            OfflinePlayer target = offline("Tom");
            store.setNickname(target.getUniqueId(), "Foxy");

            boolean cleared = service.clear(admin(PermissionNodes.NICK_OTHERS), target);

            assertThat(cleared).isTrue();
            assertThat(store.nicknameOf(target.getUniqueId())).isEmpty();
            verify(directory).clear(target.getUniqueId());
            verify(audit).record(org.mockito.ArgumentMatchers
                    .<de.raindancer.core.moderation.audit.AuditEntry.Builder>any());
        }

        @Test
        @DisplayName("clearing somebody who has none says so instead of pretending")
        void clearNothing(@TempDir Path folder) {
            NicknameService service = serviceWith(blocklistOf(folder, "{}"));
            OfflinePlayer target = offline("Tom");

            assertThat(service.clear(admin(PermissionNodes.NICK_OTHERS), target)).isFalse();
            verify(messages).send(any(CommandSender.class), eq("essentials.nick.none-to-clear"),
                    any(Object[].class));
        }
    }

    @Nested
    @DisplayName("the shared store")
    class OffTheServerThread {

        private final List<Runnable> later = new ArrayList<>();

        private NicknameService deferring(Path folder) {
            return new NicknameService(store, blocklistOf(folder, "{}"), identities, directory, messages, chat,
                    server, punishments, audit, (who, task) -> task.run(), later::add, EssentialsSettings.DEFAULTS);
        }

        private void runLater() {
            List<Runnable> due = List.copyOf(later);
            later.clear();
            due.forEach(Runnable::run);
        }

        @Test
        @DisplayName("a nickname is claimed on the storage thread, and only then said to be set")
        void claimWaitsForStorage(@TempDir Path folder) {
            NicknameService service = deferring(folder);
            Player who = player("Tom");
            when(server.getOnlinePlayers()).thenReturn(List.of());

            CompletableFuture<Boolean> set = service.set(who, who, "Foxy");

            assertThat(set).isNotDone();
            assertThat(store.nicknameOf(who.getUniqueId())).isEmpty();
            verify(messages, never()).send(any(CommandSender.class), eq("essentials.nick.set"), any(Object[].class));

            runLater();

            assertThat(set).isCompletedWithValue(true);
            assertThat(store.nicknameOf(who.getUniqueId())).contains("Foxy");
            verify(messages).send(any(CommandSender.class), eq("essentials.nick.set"), any(Object[].class));
        }

        @Test
        @DisplayName("a refusal needs no storage and is answered at once")
        void refusalIsImmediate(@TempDir Path folder) {
            NicknameService service = deferring(folder);
            Player who = player("Tom");

            CompletableFuture<Boolean> set = service.set(who, who, "x".repeat(200));

            assertThat(set).isCompletedWithValue(false);
            assertThat(later).isEmpty();
        }

        @Test
        @DisplayName("clearing takes it off at once and writes it on the storage thread")
        void clearWritesLater(@TempDir Path folder) {
            NicknameService service = deferring(folder);
            Player who = player("Tom");
            service.set(who, who, "Foxy");
            runLater();

            boolean cleared = service.clear(who, who);

            assertThat(cleared).isTrue();
            assertThat(store.nicknameOf(who.getUniqueId())).isEmpty();
            assertThat(later).hasSize(1);
            assertThat(store.flush()).isTrue();
        }
    }
}
