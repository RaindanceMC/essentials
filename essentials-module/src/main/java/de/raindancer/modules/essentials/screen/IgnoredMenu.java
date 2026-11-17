package de.raindancer.modules.essentials.screen;

import de.raindancer.core.ui.choose.PlayerChooser;
import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.menu.PaginatedMenu;
import de.raindancer.modules.essentials.EssentialsServices;
import de.raindancer.modules.essentials.util.IgnoreList;
import de.raindancer.modules.essentials.util.Players;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * The people this player ignores. Clicking one lets them message again, the button below picks somebody new
 * from Core's player list. Both go through {@link IgnoreList}, so the words match {@code /ignore}.
 */
public final class IgnoredMenu extends PaginatedMenu<UUID> {

    private final EssentialsServices services;
    private final IgnoreList ignored;

    public IgnoredMenu(EssentialsServices services, Player viewer, Menu parent) {
        super(viewer, services.brand(), parent);
        this.services = services;
        this.ignored = IgnoreList.forServices(services);
    }

    @Override
    protected Component title() {
        return Component.text("People you ignore");
    }

    @Override
    public String breadcrumb() {
        return "Ignored";
    }

    @Override
    protected List<UUID> entries() {
        return ignored.of(viewer);
    }

    @Override
    protected ItemStack emptyIcon() {
        return Icons.of(Material.COBWEB, "<gray>You ignore nobody",
                "<gray>Click here to pick somebody",
                "<gray>whose private messages you do not want.");
    }

    @Override
    protected void emptyAction(InventoryClickEvent event) {
        askWho();
    }

    @Override
    protected ItemStack icon(UUID who) {
        return Icons.head(who, "<white>" + name(who),
                "<gray>Cannot send you private messages.",
                "",
                "<gray>Click to let them message you again.");
    }

    @Override
    protected void onClick(UUID who, InventoryClickEvent event) {
        ignored.stop(viewer, who);
        refresh();
    }

    @Override
    protected void decorate() {
        super.decorate();
        toolbar(4, Icons.of(Material.IRON_DOOR, "<white>Ignore somebody",
                        "<gray>Pick a player from the list.",
                        "<gray>They cannot send you private messages."),
                click -> askWho());
    }

    private void askWho() {
        new PlayerChooser(viewer, services.brand(), this, "Ignore whom?", ignored.notToOffer(viewer),
                person -> {
                    ignored.start(viewer, person.id());
                    open();
                }).open();
    }

    private String name(UUID who) {
        return Players.nameOf(services.server().getOfflinePlayer(who));
    }
}
