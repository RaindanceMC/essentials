package de.raindancer.modules.essentials.rules;

import de.raindancer.core.moderation.rules.RulePenalty;
import de.raindancer.core.ui.text.Text;
import de.raindancer.modules.essentials.model.HouseRule;

import java.util.ArrayList;
import java.util.List;

/** The house rules as book pages, one rule a page: for the welcome book and for {@code /rules}. */
public final class RulesBookRule {

    private RulesBookRule() {
    }

    /** MiniMessage pages, numbered as players see the rules. */
    public static List<String> pages(List<HouseRule> rules) {
        List<String> pages = new ArrayList<>(rules.size());
        for (int index = 0; index < rules.size(); index++) {
            HouseRule rule = rules.get(index);
            StringBuilder page = new StringBuilder("<dark_aqua><bold>Rule ").append(index + 1).append("</bold></dark_aqua>\n")
                    .append("<black><bold>").append(Text.literal(rule.title())).append("</bold></black>\n\n")
                    .append("<black>").append(Text.literal(rule.text())).append("</black>");
            if (!rule.penalties().isEmpty()) {
                page.append("\n\n<dark_gray>Breaking it: ").append(Text.literal(RulePenalty.describe(rule.penalties())))
                        .append("</dark_gray>");
            }
            pages.add(page.toString());
        }
        return List.copyOf(pages);
    }
}
