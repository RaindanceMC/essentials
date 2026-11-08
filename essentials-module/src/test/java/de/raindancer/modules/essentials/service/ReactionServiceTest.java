package de.raindancer.modules.essentials.service;

import de.raindancer.core.ui.chat.ChatButtons;
import de.raindancer.core.ui.chat.ClickActions;
import de.raindancer.core.ui.messages.Messages;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReactionServiceTest {

    private Player player(UUID id, String name) {
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(id);
        when(p.getName()).thenReturn(name);
        return p;
    }

    @Test
    @DisplayName("one event costs one click token however many players are online, so Core's table is not flooded")
    void oneTokenPerEvent() {
        ClickActions actions = new ClickActions(System::currentTimeMillis);
        ChatButtons buttons = new ChatButtons(actions, "rcclick");
        Messages messages = mock(Messages.class);
        when(messages.raw(org.mockito.ArgumentMatchers.anyString())).thenReturn("<green>x");
        Server server = mock(Server.class);
        when(server.getConsoleSender()).thenReturn(mock(ConsoleCommandSender.class));
        List<Player> online = new ArrayList<>();
        for (int i = 0; i < 99; i++) {
            online.add(player(UUID.randomUUID(), "P" + i));
        }
        when(server.getOnlinePlayers()).thenAnswer(inv -> online);
        ReactionService service = new ReactionService(messages, buttons, server, mock(Plugin.class),
                de.raindancer.modules.essentials.EssentialsSettings.DEFAULTS);

        service.broadcast(Component.text("Ada joined"), online.get(0), ReactionService.HI);

        assertThat(actions.size()).isEqualTo(1);
    }
}
