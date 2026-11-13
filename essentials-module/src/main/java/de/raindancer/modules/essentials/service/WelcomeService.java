package de.raindancer.modules.essentials.service;

import de.raindancer.core.ui.identity.JoinLines;
import de.raindancer.core.ui.chat.Chat;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.essentials.EssentialsSettings;
import org.bukkit.entity.Player;



import de.raindancer.core.ui.chat.Style;
import de.raindancer.core.ui.text.Markup;
import net.kyori.adventure.title.Title;

import java.time.Duration;

/**
 * The lines around joining and leaving.
 *
 * <h2>Why the wording is not a setting</h2>
 * Every other server-facing sentence in this ecosystem lives in {@code messages.yml}, editable by
 * the owner without a restart's worth of settings plumbing — a join line is not special enough to be
 * the one exception. What {@link de.raindancer.modules.essentials.EssentialsSettings} decides is only
 * whether these are said at all, and whether a first join gets its own line.
 */
public final class WelcomeService implements IEssentialsService {

    private static final Title.Times GREETING = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500),
            Duration.ofMillis(1000));

    private final Messages messages;
    private final Chat chat;
    private final ReactionService reactions;
    private final java.util.function.Supplier<String> serverName;

    private volatile EssentialsSettings settings;

    public WelcomeService(Messages messages, Chat chat, ReactionService reactions, EssentialsSettings settings) {
        this(messages, chat, reactions, settings, () -> "the server");
    }

    /** @param serverName what {@code <server>} says in the welcome — Core's server name */
    public WelcomeService(Messages messages, Chat chat, ReactionService reactions, EssentialsSettings settings,
                          java.util.function.Supplier<String> serverName) {
        this.messages = messages;
        this.chat = chat;
        this.reactions = reactions;
        this.serverName = serverName;
        settings(settings);
    }

    @Override
    public void settings(EssentialsSettings fresh) {
        this.settings = fresh;
    }

    /** Whether this module's own join/quit lines replace vanilla's — the listener asks this first. */
    public boolean ownsJoinQuitLines() {
        return settings.joinQuitEnabled();
    }

    public void joined(Player who, boolean firstJoin) {
        String key;
        java.util.Optional<String> own = java.util.Optional.empty();
        if (firstJoin && settings.welcomeFirstJoin()) {
            key = "essentials.welcome.first-join";
        } else if (settings.joinQuitEnabled()) {
            key = "essentials.welcome.joined";
            own = JoinLines.custom(who.getUniqueId(), JoinLines.Kind.JOIN);
        } else {
            return;
        }
        String server = serverName.get();
        String template = own.orElseGet(() -> messages.raw(key));
        if (!settings.sayHiButton() || !reactions.isClickable()) {
            chat.broadcast(template, Chat.arg("player", who.getName()), Chat.arg("server", server));
            return;
        }
        reactions.broadcast(own.isPresent()
                        ? chat.prefixed(template, Chat.arg("player", who.getName()), Chat.arg("server", server))
                        : messages.prefixed(key, "player", who.getName(), "server", server),
                who, ReactionService.HI);
    }

    /**
     * A title of their own for everybody logging on: their name in the server's gradient, then the server. Said
     * whether or not anybody else is told they joined; an owner who blanks the title line has none.
     */
    public void greet(Player who) {
        if (messages.raw("essentials.welcome.title").isBlank()) {
            return;
        }
        Markup name = Markup.of("<gradient:" + Style.brandFrom() + ":" + Style.brandTo() + ">" + who.getName()
                + "</gradient>");
        String server = serverName.get();
        who.showTitle(Title.title(messages.get("essentials.welcome.title", "player", name, "server", server),
                messages.get("essentials.welcome.subtitle", "player", name, "server", server), GREETING));
    }

    /** Their own line when they have one (Cosmetics' flair, through Core's JoinLines), else the server's. */
    private String line(Player who, JoinLines.Kind kind, String key) {
        return JoinLines.template(who.getUniqueId(), kind, messages.raw(key));
    }

    /**
     * The quit line for somebody vanishing — the same line a real quit gets, to exactly these players.
     *
     * @return false when this module does not own the lines, so vanilla's is said instead
     */
    public boolean seemsToLeave(Player who, java.util.Collection<? extends Player> to) {
        if (!settings.joinQuitEnabled()) {
            return false;
        }
        chat.broadcast(to, line(who, JoinLines.Kind.QUIT, "essentials.welcome.quit"), Chat.arg("player", who.getName()));
        return true;
    }

    /** The join line for somebody coming out of vanish; see {@link #seemsToLeave}. */
    public boolean seemsToArrive(Player who, java.util.Collection<? extends Player> to) {
        if (!settings.joinQuitEnabled()) {
            return false;
        }
        chat.broadcast(to, line(who, JoinLines.Kind.JOIN, "essentials.welcome.joined"), Chat.arg("player", who.getName()));
        return true;
    }

    public void quit(Player who) {
        if (settings.joinQuitEnabled()) {
            chat.broadcast(line(who, JoinLines.Kind.QUIT, "essentials.welcome.quit"),
                    Chat.arg("player", who.getName()));
        }
    }

    @Override
    public String describe() {
        return "the lines around joining and leaving";
    }
}
