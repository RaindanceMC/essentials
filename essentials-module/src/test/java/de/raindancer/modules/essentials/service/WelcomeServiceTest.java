package de.raindancer.modules.essentials.service;

import de.raindancer.core.ui.chat.Chat;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.essentials.EssentialsSettings;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Vanishing reads as this module's own quit line, to exactly the players Core says should see one. */
class WelcomeServiceTest {

    private final Messages messages = mock(Messages.class);
    private final Chat chat = mock(Chat.class);
    private final Player ada = mock(Player.class);

    @Test
    @DisplayName("with its own join and quit lines on, a vanish says its quit line and vanilla says nothing")
    void ownLines() {
        when(messages.raw("essentials.welcome.quit")).thenReturn("<gray>- <white><player></white> left.");
        when(ada.getName()).thenReturn("Ada");
        WelcomeService welcome = new WelcomeService(messages, chat, mock(ReactionService.class), EssentialsSettings.DEFAULTS);
        List<Player> to = List.of(mock(Player.class));

        assertThat(welcome.seemsToLeave(ada, to)).isTrue();
        verify(chat).broadcast(eq(to), eq("<gray>- <white><player></white> left."), any());
    }

    @Test
    @DisplayName("with them off, it leaves the line to vanilla")
    void vanillaLines() {
        EssentialsSettings off = new EssentialsSettings(3, true, 300, true, false, true, true, 16, true, true,
                List.of(), true, List.of(), true, true, 20, true, true, org.bukkit.GameMode.SURVIVAL, true, false,
                true, true, true, true);
        WelcomeService welcome = new WelcomeService(messages, chat, mock(ReactionService.class), off);

        assertThat(welcome.seemsToLeave(ada, List.of())).isFalse();
        assertThat(welcome.seemsToArrive(ada, List.of())).isFalse();
        verify(chat, never()).broadcast(any(java.util.Collection.class), anyString(), any());
    }

    @Test
    @DisplayName("a first join is welcomed to the server by the name Core gives it")
    void firstJoinNamesTheServer() {
        when(ada.getName()).thenReturn("Ada");
        ReactionService reactions = mock(ReactionService.class);
        when(reactions.isClickable()).thenReturn(true);
        WelcomeService welcome = new WelcomeService(messages, chat, reactions, EssentialsSettings.DEFAULTS,
                () -> "Lilly's SMP");

        welcome.joined(ada, true);

        verify(messages).prefixed("essentials.welcome.first-join", "player", "Ada", "server", "Lilly's SMP");
    }

    @org.junit.jupiter.api.AfterEach
    void noProvider() {
        de.raindancer.core.ui.identity.JoinLines.provider(null);
    }

    private static void ownLinesFor(Player who) {
        java.util.UUID id = java.util.UUID.randomUUID();
        when(who.getUniqueId()).thenReturn(id);
        de.raindancer.core.ui.identity.JoinLines.provider((player, kind) -> !player.equals(id) ? java.util.Optional.empty()
                : java.util.Optional.of(kind == de.raindancer.core.ui.identity.JoinLines.Kind.JOIN
                        ? "<gold>✦ <player> arrives in a shower of sparks" : "<gray><player> drifts off into the stars"));
    }

    @Test
    @DisplayName("somebody with their own lines (Cosmetics' flair) is greeted and seen off with them")
    void theirOwnLines() {
        when(messages.raw("essentials.welcome.joined")).thenReturn("<gray>+ <player>");
        when(messages.raw("essentials.welcome.quit")).thenReturn("<gray>- <player>");
        when(ada.getName()).thenReturn("Ada");
        ownLinesFor(ada);
        WelcomeService welcome = new WelcomeService(messages, chat, mock(ReactionService.class), EssentialsSettings.DEFAULTS);

        welcome.joined(ada, false);
        welcome.quit(ada);

        verify(chat).broadcast(eq("<gold>✦ <player> arrives in a shower of sparks"), any(net.kyori.adventure.text.minimessage.tag.resolver.TagResolver[].class));
        verify(chat).broadcast(eq("<gray><player> drifts off into the stars"), any(net.kyori.adventure.text.minimessage.tag.resolver.TagResolver[].class));
    }

    @Test
    @DisplayName("their own line goes out with the say-hi button too, and vanishing uses it as well")
    void theirOwnLinesClickableAndVanish() {
        when(ada.getName()).thenReturn("Ada");
        ownLinesFor(ada);
        ReactionService reactions = mock(ReactionService.class);
        when(reactions.isClickable()).thenReturn(true);
        WelcomeService welcome = new WelcomeService(messages, chat, reactions, EssentialsSettings.DEFAULTS);
        List<Player> to = List.of(mock(Player.class));

        welcome.joined(ada, false);
        welcome.seemsToLeave(ada, to);

        verify(chat).prefixed(eq("<gold>✦ <player> arrives in a shower of sparks"), any(net.kyori.adventure.text.minimessage.tag.resolver.TagResolver[].class));
        verify(chat).broadcast(eq(to), eq("<gray><player> drifts off into the stars"), any(net.kyori.adventure.text.minimessage.tag.resolver.TagResolver[].class));
    }

    @Test
    @DisplayName("a first join keeps the server's own welcome")
    void firstJoinStaysTheServers() {
        when(ada.getName()).thenReturn("Ada");
        when(messages.raw("essentials.welcome.first-join")).thenReturn("<gold>Welcome <player>!");
        ownLinesFor(ada);
        WelcomeService welcome = new WelcomeService(messages, chat, mock(ReactionService.class), EssentialsSettings.DEFAULTS);

        welcome.joined(ada, true);

        verify(chat).broadcast(eq("<gold>Welcome <player>!"), any(net.kyori.adventure.text.minimessage.tag.resolver.TagResolver[].class));
    }

    @Test
    @DisplayName("everybody logging on is greeted by name with a title of their own, in the server's words")
    void greetedWithATitle() {
        when(ada.getName()).thenReturn("Ada");
        when(messages.raw("essentials.welcome.title")).thenReturn("<white>Welcome home, <player>");
        when(messages.get(eq("essentials.welcome.title"), any(Object[].class)))
                .thenReturn(net.kyori.adventure.text.Component.text("Welcome home, Ada"));
        when(messages.get(eq("essentials.welcome.subtitle"), any(Object[].class)))
                .thenReturn(net.kyori.adventure.text.Component.text("Glad to see you on Hyades!"));
        WelcomeService welcome = new WelcomeService(messages, chat, mock(ReactionService.class),
                EssentialsSettings.DEFAULTS, () -> "Hyades");

        welcome.greet(ada);

        org.mockito.ArgumentCaptor<net.kyori.adventure.title.Title> shown =
                org.mockito.ArgumentCaptor.forClass(net.kyori.adventure.title.Title.class);
        verify(ada).showTitle(shown.capture());
        assertThat(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(shown.getValue().title())).isEqualTo("Welcome home, Ada");
        assertThat(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(shown.getValue().subtitle())).isEqualTo("Glad to see you on Hyades!");
        verify(messages).get(eq("essentials.welcome.subtitle"), eq("player"), any(), eq("server"), eq("Hyades"));
    }

    @Test
    @DisplayName("an owner who blanks the title line has no title at all")
    void blankTitleIsNone() {
        when(messages.raw("essentials.welcome.title")).thenReturn(" ");
        new WelcomeService(messages, chat, mock(ReactionService.class), EssentialsSettings.DEFAULTS).greet(ada);
        verify(ada, never()).showTitle(any(net.kyori.adventure.title.Title.class));
    }
}
