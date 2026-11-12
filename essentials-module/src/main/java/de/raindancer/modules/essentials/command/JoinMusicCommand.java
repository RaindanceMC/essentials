package de.raindancer.modules.essentials.command;

import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.essentials.service.JoinMusicService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** {@code /joinmusic [on|off]} — the music on logging on, for yourself; plain toggles it. */
public final class JoinMusicCommand implements IEssentialsCommand {

    private final Supplier<Messages> messages;
    private final Supplier<JoinMusicService> music;

    public JoinMusicCommand(Supplier<Messages> messages, Supplier<JoinMusicService> music) {
        this.messages = messages;
        this.music = music;
    }

    @Override
    public String describe() {
        return "switches the music on logging on off, or back on, for you";
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        Messages text = messages.get();
        if (!(source.getSender() instanceof Player player)) {
            text.send(source.getSender(), "essentials.only-a-player");
            return;
        }
        JoinMusicService live = music.get();
        String choice = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        boolean on;
        switch (choice) {
            case "" -> on = live.toggle(player);
            case "on" -> {
                live.set(player, true);
                on = true;
            }
            case "off" -> {
                live.set(player, false);
                on = false;
            }
            default -> {
                text.send(player, "essentials.joinmusic.usage");
                return;
            }
        }
        text.send(player, on ? "essentials.joinmusic.switched-on" : "essentials.joinmusic.switched-off");
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        return args.length <= 1 ? List.of("on", "off") : List.of();
    }
}
