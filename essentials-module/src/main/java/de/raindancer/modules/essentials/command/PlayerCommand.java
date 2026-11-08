package de.raindancer.modules.essentials.command;

import de.raindancer.core.ui.profile.ProfileMenu;
import de.raindancer.modules.essentials.EssentialsServices;
import de.raindancer.modules.essentials.util.PermissionNodes;
import de.raindancer.modules.essentials.util.Players;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * {@code /player <name>} — straight to that one player's profile, for whoever already knows who
 * they want rather than browsing {@code /players}' list for them. Bare {@code /player} is the same
 * list {@code /players} opens; the two commands are two doors onto the one feature; picking a name
 * out of chat or typing it here both land on the same {@link ProfileMenu}.
 */
public final class PlayerCommand implements IEssentialsCommand {

    private final Supplier<EssentialsServices> services;

    /** Bare {@code /profile} is your own profile; bare {@code /player} the list to pick from. */
    private final boolean bareIsOwn;

    public PlayerCommand(Supplier<EssentialsServices> services) {
        this(services, false);
    }

    public PlayerCommand(Supplier<EssentialsServices> services, boolean bareIsOwn) {
        this.services = services;
        this.bareIsOwn = bareIsOwn;
    }

    @Override
    public String describe() {
        return "opens one player's profile directly, by name";
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        EssentialsServices live = services.get();
        CommandSender sender = source.getSender();
        if (args.length == 0) {
            if (!(sender instanceof Player viewer)) {
                live.messages().send(sender, "essentials.usage", "usage", bareIsOwn ? "/profile <name>" : "/player <name>");
                return;
            }
            if (bareIsOwn) {
                new ProfileMenu(viewer, live.brand(), null, viewer.getUniqueId(), viewer.getName()).open();
                return;
            }
            SeenCommand.openChooser(live, viewer, "Players");
            return;
        }
        Optional<OfflinePlayer> found = Players.one(live.messages(), live.server(), sender, args[0]);
        if (found.isEmpty()) {
            return;
        }
        OfflinePlayer them = found.get();
        if (sender instanceof Player viewer) {
            new ProfileMenu(viewer, live.brand(), null, them.getUniqueId(), Players.nameOf(them)).open();
            return;
        }
        // No inventory to open one in — the same report /seen <name> gives, which is the closest
        // thing to a profile the console can be shown at all.
        SeenCommand.report(live, sender, them);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length <= 1) {
            EssentialsServices live = services.get();
            String typed = args.length == 1 ? args[0] : "";
            return Players.suggest(live.server(), source.getSender(), typed, live.core().vanish());
        }
        return List.of();
    }

    @Override
    public String permission() {
        return PermissionNodes.PLAYERS;
    }
}
