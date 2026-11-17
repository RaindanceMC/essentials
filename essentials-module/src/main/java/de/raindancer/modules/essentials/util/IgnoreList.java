package de.raindancer.modules.essentials.util;

import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.essentials.service.MessagingService;
import de.raindancer.modules.essentials.store.EssentialsStore;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Whom a player ignores, as the ignore page needs it: the list by name, and the two changes with the same
 * words {@code /ignore} and {@code /unignore} use.
 */
public final class IgnoreList {

    private final EssentialsStore store;
    private final MessagingService messaging;
    private final Messages messages;
    private final Function<UUID, String> names;

    public IgnoreList(EssentialsStore store, MessagingService messaging, Messages messages,
                      Function<UUID, String> names) {
        this.store = store;
        this.messaging = messaging;
        this.messages = messages;
        this.names = names;
    }

    public static IgnoreList forServices(de.raindancer.modules.essentials.EssentialsServices services) {
        return new IgnoreList(services.store(), services.messaging(), services.messages(),
                id -> Players.nameOf(services.server().getOfflinePlayer(id)));
    }

    /** Everybody this player ignores, A to Z. */
    public List<UUID> of(Player who) {
        List<UUID> ignored = new ArrayList<>(store.ignoredBy(who.getUniqueId()));
        ignored.sort(Comparator.comparing(names, String.CASE_INSENSITIVE_ORDER));
        return ignored;
    }

    /** Who a chooser must not offer: yourself and whoever is on the list already. */
    public List<UUID> notToOffer(Player who) {
        List<UUID> skip = new ArrayList<>(store.ignoredBy(who.getUniqueId()));
        skip.add(who.getUniqueId());
        return skip;
    }

    public boolean stop(Player who, UUID target) {
        boolean changed = messaging.stopIgnoring(who, target);
        messages.send(who, changed ? "essentials.ignore.stopped" : "essentials.ignore.not-ignoring",
                "player", names.apply(target));
        return changed;
    }

    public boolean start(Player who, UUID target) {
        if (who.getUniqueId().equals(target)) {
            return false;
        }
        boolean changed = messaging.ignore(who, target);
        if (changed) {
            messages.send(who, "essentials.ignore.started", "player", names.apply(target));
        }
        return changed;
    }
}
