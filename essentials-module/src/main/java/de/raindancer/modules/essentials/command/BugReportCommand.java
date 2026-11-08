package de.raindancer.modules.essentials.command;

import de.raindancer.modules.essentials.EssentialsServices;
import de.raindancer.modules.essentials.screen.BugReportDialogs;
import de.raindancer.modules.essentials.service.BugReportService;
import de.raindancer.modules.essentials.util.PermissionNodes;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import java.util.function.Supplier;

/** {@code /bugreport} — report a bug, an idea or a problem to the team, or +1 one somebody already reported. */
public final class BugReportCommand implements IEssentialsCommand {

    private final Supplier<EssentialsServices> services;
    private final Supplier<BugReportService> reports;

    public BugReportCommand(Supplier<EssentialsServices> services, Supplier<BugReportService> reports) {
        this.services = services;
        this.reports = reports;
    }

    @Override
    public String describe() {
        return "reports a bug, an idea or a problem to the team";
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        EssentialsServices live = services.get();
        if (!(source.getSender() instanceof Player by)) {
            live.messages().send(source.getSender(), "essentials.only-a-player");
            return;
        }
        BugReportService bugs = reports.get();
        if (!bugs.enabled()) {
            live.messages().send(by, "essentials.bugreport.switched-off");
            return;
        }
        if (!bugs.ready()) {
            live.messages().send(by, "essentials.bugreport.not-set-up");
            return;
        }
        new BugReportDialogs(live.plugin(), bugs, by).home();
    }

    @Override
    public String permission() {
        return PermissionNodes.BUGREPORT;
    }
}
