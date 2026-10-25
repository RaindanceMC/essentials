package de.raindancer.modules.essentials.service;

import de.raindancer.core.moderation.vanish.Vanish;
import de.raindancer.core.ui.hologram.ScreenKind;
import de.raindancer.core.ui.hologram.Screens;
import de.raindancer.modules.essentials.EssentialsSettings;
import de.raindancer.modules.essentials.model.HouseRule;
import de.raindancer.modules.essentials.rules.ScreenLineRule;
import de.raindancer.modules.essentials.store.EssentialsStore;
import de.raindancer.modules.essentials.store.RuleBook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/** Essentials' screens for Core's {@code /screen}: who is away, the server's rules, and everybody's own page. */
public final class EssentialsScreens implements IEssentialsService {

    public static final List<String> KINDS = List.of("essentials-afk", "essentials-rules", "essentials-you");
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Server server;
    private final Vanish vanish;
    private final AfkService afk;
    private final RuleBook rules;
    private final EssentialsStore store;
    private final ScreenLineRule rule = new ScreenLineRule();

    public EssentialsScreens(Server server, Vanish vanish, AfkService afk, RuleBook rules, EssentialsStore store) {
        this.server = server;
        this.vanish = vanish;
        this.afk = afk;
        this.rules = rules;
        this.store = store;
    }

    @Override
    public void settings(EssentialsSettings fresh) {
        // The screens read live state and no setting.
    }

    public void register(Screens screens, Plugin owner) {
        screens.register(owner, ScreenKind.everyone("essentials-afk", "Away right now",
                "Who is away from the keyboard on this server.", Material.CLOCK, this::away));
        screens.register(owner, ScreenKind.everyone("essentials-rules", "Server rules",
                "The rules players read with /rules, numbered the same.", Material.WRITABLE_BOOK, this::rules));
        screens.register(owner, ScreenKind.personal("essentials-you", "You",
                "Everybody's own nickname, away status and ignore count.", Material.PLAYER_HEAD, this::you));
    }

    private Component away() {
        List<String> names = server.getOnlinePlayers().stream()
                .filter(player -> afk.isAfk(player.getUniqueId()) && !vanish.isVanished(player.getUniqueId()))
                .map(Player::getName).toList();
        return MINI.deserialize(rule.away(names));
    }

    private Component rules() {
        return MINI.deserialize(rule.rules(rules.shown().stream().map(HouseRule::title).toList()));
    }

    private Component you(ScreenKind.Viewer viewer) {
        return MINI.deserialize(rule.you(store.nicknameOf(viewer.id()).orElse(null), afk.isAfk(viewer.id()),
                store.ignoredBy(viewer.id()).size()));
    }
}
