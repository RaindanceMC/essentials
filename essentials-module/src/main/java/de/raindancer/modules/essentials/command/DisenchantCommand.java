package de.raindancer.modules.essentials.command;

import de.raindancer.modules.essentials.EssentialsServices;
import de.raindancer.modules.essentials.rules.DisenchantRule;
import de.raindancer.modules.essentials.service.EnchantService;
import de.raindancer.modules.essentials.util.Enchantments;
import de.raindancer.modules.essentials.util.PermissionNodes;
import de.raindancer.modules.essentials.util.Players;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * {@code /disenchant <enchantment|all> [player]} — takes enchantments off the item in the hand, one at a time or
 * all of them; curses only with {@link PermissionNodes#DISENCHANT_CURSES}. Tab completion offers what is on the item.
 */
public final class DisenchantCommand implements IEssentialsCommand {

    private static final String USAGE = "/disenchant <enchantment|all> [player]";

    private final Supplier<EssentialsServices> services;

    public DisenchantCommand(Supplier<EssentialsServices> services) {
        this.services = services;
    }

    @Override
    public String describe() {
        return "takes enchantments off the item in your hand";
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        EssentialsServices live = services.get();
        CommandSender sender = source.getSender();
        if (args.length == 0 || args.length > 2) {
            live.messages().send(sender, "essentials.usage", "usage", USAGE);
            return;
        }
        List<Player> targets;
        if (args.length > 1) {
            targets = Players.online(live.messages(), live.server(), sender, args[1], false, "essentials.enchant.nobody-there");
        } else if (sender instanceof Player self) {
            targets = List.of(self);
        } else {
            live.messages().send(sender, "essentials.only-a-player");
            return;
        }
        if (args[0].equalsIgnoreCase(DisenchantRule.ALL)) {
            boolean curses = sender.hasPermission(PermissionNodes.DISENCHANT_CURSES);
            targets.forEach(target -> live.enchanting().clear(sender, target, curses));
            return;
        }
        Optional<Enchantment> found = Enchantments.find(args[0]);
        if (found.isEmpty()) {
            live.messages().send(sender, "essentials.enchant.unknown", "enchantment", args[0]);
            return;
        }
        targets.forEach(target -> live.enchanting().disenchant(sender, target, found.get()));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length <= 1) {
            if (!(sender instanceof Player player)) {
                return List.of();
            }
            return DisenchantRule.suggestions(EnchantService.onItem(player.getInventory().getItemInMainHand()),
                    sender.hasPermission(PermissionNodes.DISENCHANT_CURSES), args.length == 0 ? "" : args[0]);
        }
        if (args.length == 2 && sender.hasPermission(PermissionNodes.ENCHANT_OTHERS)) {
            EssentialsServices live = services.get();
            String typed = args[1].toLowerCase(Locale.ROOT);
            return Players.suggest(live.server(), sender, args[1], live.core().vanish()).stream()
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
        }
        return List.of();
    }

    @Override
    public String permission() {
        return PermissionNodes.DISENCHANT;
    }
}
