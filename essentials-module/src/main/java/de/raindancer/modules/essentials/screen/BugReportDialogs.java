package de.raindancer.modules.essentials.screen;

import de.raindancer.core.platform.github.GitHubIssues;
import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.core.ui.text.Text;
import de.raindancer.modules.essentials.model.BugReports;
import de.raindancer.modules.essentials.service.BugReportService;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * /bugreport in the game's own windows, like the /help guide: what kind of thing it is, where, what others already
 * reported there (+1 it), and the form for a new one.
 */
public final class BugReportDialogs {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final ClickCallback.Options CALLBACK = ClickCallback.Options.builder()
            .uses(ClickCallback.UNLIMITED_USES).lifetime(Duration.ofMinutes(30)).build();
    private static final int TEXT_WIDTH = 320;
    private static final int WIDE_BUTTON = 300;
    private static final int BUTTON = 150;
    private static final int LONGEST_TITLE = 80;
    private static final int LONGEST_TEXT = 1500;
    private static final String TITLE = "title";
    private static final String TEXT = "text";

    private final Plugin plugin;
    private final BugReportService reports;
    private final Player viewer;

    public BugReportDialogs(Plugin plugin, BugReportService reports, Player viewer) {
        this.plugin = plugin;
        this.reports = reports;
        this.viewer = viewer;
    }

    /** What kind of thing it is. */
    public void home() {
        List<DialogBody> body = List.of(DialogBody.item(new ItemStack(Material.WRITABLE_BOOK)).showTooltip(false)
                .description(DialogBody.plainMessage(MINI.deserialize("<white>Found something broken, or wish "
                        + "something worked differently? Tell the team — it lands right where they work on the "
                        + "server."), TEXT_WIDTH - 40)).build());
        List<ActionButton> buttons = new ArrayList<>();
        kind(buttons, BugReports.Kind.BUG, "<red>");
        kind(buttons, BugReports.Kind.IDEA, "<green>");
        kind(buttons, BugReports.Kind.OTHER, "<yellow>");
        show(null, body, List.of(), buttons, button("<gray>Close", null, BUTTON, (response, player) -> { }), 1);
    }

    private void kind(List<ActionButton> buttons, BugReports.Kind kind, String colour) {
        buttons.add(button(colour + kind.title(), "<gray>" + kind.hint(), WIDE_BUTTON,
                (response, player) -> then(() -> area(kind))));
    }

    /** Where in the game it is. */
    void area(BugReports.Kind kind) {
        List<DialogBody> body = List.of(DialogBody.plainMessage(MINI.deserialize("<white>Where does it happen? "
                + "<gray>Pick the part of the game it is about."), TEXT_WIDTH));
        List<ActionButton> buttons = new ArrayList<>();
        for (BugReports.Area area : reports.areas()) {
            buttons.add(button("<white>" + Text.literal(area.name()), "<gray>See what others reported here, or "
                    + "report something new.", BUTTON, (response, player) -> then(() -> load(kind, area))));
        }
        show(kind.title(), body, List.of(), buttons, back(this::home), 2);
    }

    private void load(BugReports.Kind kind, BugReports.Area area) {
        reports.open(viewer, area, found -> reported(kind, area, found));
    }

    /** What is already reported there: +1 one, or report something new. */
    void reported(BugReports.Kind kind, BugReports.Area area, List<GitHubIssues.Issue> found) {
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(MINI.deserialize(found.isEmpty()
                ? "<white>Nothing is reported in " + Text.literal(area.name()) + " yet. <gray>Be the first."
                : "<white>Already reported in " + Text.literal(area.name()) + ". <gray>Did one happen to you too? "
                + "Press it — that counts you in (+1), so the team sees how many it hits."), TEXT_WIDTH));
        List<ActionButton> buttons = new ArrayList<>();
        for (GitHubIssues.Issue issue : found) {
            String others = issue.plusOnes() == 0 ? "Nobody else has said they had it yet."
                    : issue.plusOnes() + (issue.plusOnes() == 1 ? " other player had it too." : " other players had it too.");
            buttons.add(button("<white>+1 <dark_gray>·</dark_gray> " + Text.literal(shortened(issue.title()))
                            + " <gray>(" + issue.plusOnes() + ")",
                    "<white>" + Text.literal(issue.title()) + "\n<dark_gray>#" + issue.number() + "</dark_gray> <gray>"
                            + others + " Press if it happened to you "
                            + "too.", WIDE_BUTTON, (response, player) -> then(() -> reports.plusOne(viewer, area, issue,
                            () -> load(kind, area)))));
        }
        buttons.add(button("<green>Report something new", "<gray>" + kind.title() + " in " + Text.literal(area.name())
                + ".", WIDE_BUTTON, (response, player) -> then(() -> form(kind, area, "", "", null))));
        show(area.name(), body, List.of(), buttons, back(() -> area(kind)), 1);
    }

    /** The report itself: a title and what happened, and what goes along with it. */
    void form(BugReports.Kind kind, BugReports.Area area, String title, String text, String problem) {
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(MINI.deserialize("<white>" + kind.title() + " in "
                + Text.literal(area.name()) + ". <gray>Say what happened and how the team can see it again."),
                TEXT_WIDTH));
        if (problem != null) {
            body.add(DialogBody.plainMessage(MINI.deserialize("<red>" + Text.literal(problem)), TEXT_WIDTH));
        }
        body.add(DialogBody.plainMessage(MINI.deserialize(String.join(" ", BugReports.WHAT_IS_SENT)
                + " <gray>Only the team can read it."), TEXT_WIDTH));
        List<DialogInput> inputs = List.of(
                DialogInput.text(TITLE, MINI.deserialize("<white>A short title")).width(WIDE_BUTTON)
                        .maxLength(LONGEST_TITLE).initial(title).labelVisible(true).build(),
                DialogInput.text(TEXT, MINI.deserialize("<white>What happened?")).width(WIDE_BUTTON)
                        .maxLength(LONGEST_TEXT).initial(text).labelVisible(true)
                        .multiline(TextDialogInput.MultilineOptions.create(8, 100)).build());
        List<ActionButton> buttons = List.of(button("<green>Send", "<gray>Off to the team.", BUTTON,
                (response, player) -> {
                    String typedTitle = response.getText(TITLE) == null ? "" : response.getText(TITLE).strip();
                    String typedText = response.getText(TEXT) == null ? "" : response.getText(TEXT).strip();
                    if (typedTitle.isEmpty() || typedText.isEmpty()) {
                        then(() -> form(kind, area, typedTitle, typedText,
                                typedTitle.isEmpty() ? "It needs a title." : "Say what happened, please."));
                        return;
                    }
                    then(() -> reports.report(viewer, kind, area, typedTitle, typedText));
                }));
        show("New report", body, inputs, buttons, back(() -> load(kind, area)), 2);
    }

    private void show(String page, List<DialogBody> body, List<DialogInput> inputs, List<ActionButton> buttons,
                      ActionButton exit, int columns) {
        Component title = MINI.deserialize("<gradient:#F87171:#FBBF24>Bug report</gradient>" + (page == null ? ""
                : " <dark_gray>›</dark_gray> <white>" + Text.literal(page)));
        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(title).canCloseWithEscape(true).pause(false)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE).body(body).inputs(inputs).build())
                .type(DialogType.multiAction(buttons).exitAction(exit).columns(columns).build()));
        viewer.showDialog(dialog);
    }

    /** A button holds about fifty characters; the whole title is in the tooltip's report anyway. */
    static String shortened(String title) {
        String one = title.replace('\n', ' ').strip();
        return one.length() <= 40 ? one : one.substring(0, 39).strip() + "…";
    }

    private ActionButton back(Runnable where) {
        return button("<gray>Back", null, BUTTON, (response, player) -> then(where));
    }

    private static ActionButton button(String label, String tooltip, int width, DialogActionCallback callback) {
        return ActionButton.builder(MINI.deserialize(label)).tooltip(tooltip == null ? null : MINI.deserialize(tooltip))
                .width(width).action(DialogAction.customClick(callback, CALLBACK)).build();
    }

    /** The next window, on the viewer's own thread. */
    private void then(Runnable next) {
        Scheduling.entity(plugin, viewer, next);
    }

}
