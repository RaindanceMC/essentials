package de.raindancer.modules.essentials.screen;

import de.raindancer.core.platform.util.Wrapping;
import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.menu.PaginatedMenu;
import de.raindancer.modules.essentials.EssentialsServices;
import de.raindancer.modules.essentials.model.HouseRule;
import de.raindancer.modules.essentials.rules.HouseRuleTextRule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The rule editor: every rule in order, switched-off ones greyed. Click opens one, shift-click switches it
 * on or off; the toolbar adds a rule and opens the presets.
 */
public final class RulesMenu extends PaginatedMenu<HouseRule> {


    private final EssentialsServices services;

    public RulesMenu(EssentialsServices services, Player viewer, Menu parent) {
        super(viewer, services.brand(), parent);
        this.services = services;
    }

    @Override
    protected Component title() {
        return Component.text("Rules");
    }

    @Override
    protected List<HouseRule> entries() {
        return services.rules().book().rules();
    }

    @Override
    protected ItemStack emptyIcon() {
        return Icons.of(Material.BARRIER, "<gray>No rules yet",
                "<dark_gray>Add one below, or apply a preset.");
    }

    @Override
    protected ItemStack icon(HouseRule rule) {
        int number = entries().indexOf(rule) + 1;
        List<String> lore = new ArrayList<>(lore(rule.text()));
        if (!rule.penalties().isEmpty()) {
            lore.add("<red>" + escaped(de.raindancer.modules.essentials.service.RulesService.ladderInWords(rule.penalties())));
        }
        lore.add("");
        lore.add(rule.enabled() ? "<green>shown to players" : "<red>switched off — kept, not shown");
        lore.add("");
        lore.add("<dark_gray>click to edit");
        lore.add("<dark_gray>shift-click to switch " + (rule.enabled() ? "off" : "on"));
        String name = (rule.enabled() ? "<white>" : "<gray><strikethrough>") + number + ". "
                + escaped(rule.title());
        return Icons.of(rule.enabled() ? rule.drawnAs() : Material.GRAY_DYE, name, lore);
    }

    @Override
    protected void onClick(HouseRule rule, InventoryClickEvent event) {
        if (event.isShiftClick()) {
            services.rules().book().update(rule.id(), current -> current.withEnabled(!current.enabled()));
            refresh();
            return;
        }
        new RuleMenu(services, viewer, rule.id(), this).open();
    }

    @Override
    protected void render() {
        super.render();
        toolbar(3, Icons.of(Material.WRITABLE_BOOK, "<green>Add a rule",
                        "<gray>Type its title, then its text, in chat."),
                click -> askTitle());
        toolbar(5, Icons.of(Material.BOOKSHELF, "<gold>Presets",
                        "<gray>Whole sets of rules: apply one,",
                        "<gray>or keep these as one of your own."),
                click -> new RulePresetsMenu(services, viewer, this).open());
    }

    @Override
    protected List<String> helpLines() {
        return List.of("<gray>Players read these with <white>/rules</white>.",
                "<gray>The number is the order they are shown in.",
                "<gray>Switched-off rules are kept, but nobody sees them.");
    }

    private void askTitle() {
        ask("essentials.rules.type-title", title -> {
            if (services.rules().check(viewer, HouseRuleTextRule.Part.TITLE, title).isRefused()) {
                open();
                return;
            }
            ask("essentials.rules.type-text", text -> {
                services.rules().add(viewer, title, text);
                open();
            });
        });
    }

    /** In the game's dialog, not in chat; a cancel brings this page back. */
    private void ask(String key, java.util.function.Consumer<String> answered) {
        de.raindancer.core.ui.prompt.AnvilInput.open(viewer, titleFor(key), "", de.raindancer.core.ui.prompt.Parsers.text(200), answered, this::open);
    }

    /** What the game's dialog asks for, per prompt key — the chat wording said "in chat" and how long one had. */
    static String titleFor(String key) {
        return switch (key) {
            case "essentials.rules.type-title" -> "The rule's title";
            case "essentials.rules.type-text" -> "What the rule says";
            case "essentials.rules.type-penalties" -> "What breaking it costs: warn, mute 1h, ban 3d";
            default -> "Type it";
        };
    }

    static String escaped(String text) {
        return MiniMessage.miniMessage().escapeTags(text);
    }

    static List<String> lore(String text) {
        return Wrapping.wrap(escaped(text), 36, "<gray>");
    }
}
