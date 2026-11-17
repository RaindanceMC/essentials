package de.raindancer.modules.essentials.rules;

import de.raindancer.core.moderation.rules.RulePenalty;
import de.raindancer.modules.essentials.model.HouseRule;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RulesBookRuleTest {

    private static String shown(String page) {
        return PlainTextComponentSerializer.plainText().serialize(MiniMessage.miniMessage().deserialize(page));
    }

    @Test
    @DisplayName("one page per rule: its number, title and text, and what breaking it costs")
    void onePagePerRule() {
        List<HouseRule> rules = List.of(
                new HouseRule("kind", "Be kind", "No insults.", HouseRule.DEFAULT_ICON, true,
                        RulePenalty.ladder("warn, mute 1h").orElseThrow()),
                new HouseRule("grief", "No griefing", "Leave other builds alone.", HouseRule.DEFAULT_ICON, true,
                        List.of()));

        List<String> pages = RulesBookRule.pages(rules);

        assertThat(pages).hasSize(2);
        assertThat(shown(pages.get(0))).contains("Rule 1").contains("Be kind").contains("No insults.")
                .contains("warning");
        assertThat(shown(pages.get(1))).contains("Rule 2").contains("No griefing").doesNotContain("Breaking it");
    }

    @Test
    @DisplayName("an owner's rule text stays text in the book")
    void textStaysText() {
        List<String> pages = RulesBookRule.pages(List.of(new HouseRule("x", "<red>Loud", "<bold>no",
                HouseRule.DEFAULT_ICON, true, List.of())));
        assertThat(shown(pages.getFirst())).contains("<red>Loud").contains("<bold>no");
    }

    @Test
    @DisplayName("no rules, no pages")
    void none() {
        assertThat(RulesBookRule.pages(List.of())).isEmpty();
    }

    @Test
    @DisplayName("a rule too long for one page with its penalties takes the next page for them; nothing is cut off")
    void longRuleSpills() {
        HouseRule cheating = new HouseRule("cheat", "No cheating",
                "No hacked clients, x-ray, duping or exploits. Found a bug? Tell staff instead of using it.",
                HouseRule.DEFAULT_ICON, true, RulePenalty.ladder("ban 14d, ban").orElseThrow());

        List<String> pages = RulesBookRule.pages(List.of(cheating));

        assertThat(pages).hasSize(2);
        assertThat(shown(pages.get(0))).contains("No cheating").contains("Tell staff").doesNotContain("Breaking it");
        assertThat(shown(pages.get(1))).contains("Rule 1").contains("Breaking it");
        for (String page : pages) {
            assertThat(RulesBookRule.lines(shown(page))).as(page).isLessThanOrEqualTo(RulesBookRule.LINES_A_PAGE);
        }
    }

    @Test
    @DisplayName("a short rule keeps its penalties on its own page")
    void shortRuleStaysTogether() {
        HouseRule kind = new HouseRule("kind", "Be kind", "No insults.", HouseRule.DEFAULT_ICON, true,
                RulePenalty.ladder("warn, mute 1h").orElseThrow());
        assertThat(RulesBookRule.pages(List.of(kind))).hasSize(1);
    }
}
