package de.raindancer.modules.essentials.rules;

import de.raindancer.core.moderation.rules.RulePenalty;
import de.raindancer.core.ui.text.Text;
import de.raindancer.modules.essentials.model.HouseRule;

import java.util.ArrayList;
import java.util.List;

/**
 * The house rules as book pages, one rule a page: for the welcome book and for {@code /rules}. A book page cuts off
 * silently what does not fit, so a rule too long to keep its penalties on its page gets a second page for them.
 */
public final class RulesBookRule {

    /** Lines a book page shows. */
    public static final int LINES_A_PAGE = 14;
    /** Characters a line holds, a little under the average so word wrap is allowed for. */
    static final int CHARS_A_LINE = 17;

    private RulesBookRule() {
    }

    /** MiniMessage pages, numbered as players see the rules. */
    public static List<String> pages(List<HouseRule> rules) {
        List<String> pages = new ArrayList<>(rules.size());
        for (int index = 0; index < rules.size(); index++) {
            HouseRule rule = rules.get(index);
            String header = "<dark_aqua><bold>Rule " + (index + 1) + "</bold></dark_aqua>\n";
            String body = header + "<black><bold>" + Text.literal(rule.title()) + "</bold></black>\n\n"
                    + "<black>" + Text.literal(rule.text()) + "</black>";
            if (rule.penalties().isEmpty()) {
                pages.add(body);
                continue;
            }
            String penalties = "<dark_gray>Breaking it: " + Text.literal(RulePenalty.describe(rule.penalties()))
                    + "</dark_gray>";
            String together = body + "\n\n" + penalties;
            // A line short of the page: the count is an estimate, and a page cut off says nothing.
            if (lines(plain(together)) < LINES_A_PAGE) {
                pages.add(together);
            } else {
                pages.add(body);
                pages.add(header + penalties);
            }
        }
        return List.copyOf(pages);
    }

    /** About how many lines this text takes on a book page. */
    public static int lines(String plain) {
        int lines = 0;
        for (String paragraph : plain.split("\n", -1)) {
            lines += Math.max(1, (paragraph.length() + CHARS_A_LINE - 1) / CHARS_A_LINE);
        }
        return lines;
    }

    private static String plain(String mini) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(mini));
    }
}
