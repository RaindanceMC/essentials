package de.raindancer.modules.essentials.rules;

import de.raindancer.core.platform.rule.Verdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DisenchantRuleTest {

    private final DisenchantRule rule = new DisenchantRule();

    private static DisenchantRule.Request.Builder own() {
        return DisenchantRule.Request.builder().self(true).holding(true).present(true);
    }

    @Test
    @DisplayName("anybody may take an enchantment off what they hold")
    void ownItem() {
        assertThat(rule.judge(own().build()).isAllowed()).isTrue();
    }

    @Test
    @DisplayName("somebody else's item needs the others node")
    void others() {
        Verdict refused = rule.judge(own().self(false).build());
        assertThat(refused.reason()).isEqualTo("essentials.enchant.not-others");
        assertThat(rule.judge(own().self(false).mayOthers(true).build()).isAllowed()).isTrue();
    }

    @Test
    @DisplayName("empty hands, or an enchantment that is not on it, are said rather than pretended")
    void nothingThere() {
        assertThat(rule.judge(own().holding(false).build()).reason()).isEqualTo("essentials.disenchant.nothing-held");
        assertThat(rule.judge(own().present(false).build()).reason()).isEqualTo("essentials.enchant.not-on-item");
    }

    @Test
    @DisplayName("curses stick unless one may lift them")
    void curses() {
        assertThat(rule.judge(own().cursed(true).build()).reason()).isEqualTo("essentials.disenchant.cursed");
        assertThat(rule.judge(own().cursed(true).mayCurses(true).build()).isAllowed()).isTrue();
    }

    @Test
    @DisplayName("tab completion offers what is on the item — curses only to who may lift them — and all")
    void suggestions() {
        Map<String, Boolean> onItem = new LinkedHashMap<>();
        onItem.put("sharpness", false);
        onItem.put("rainsenchants:lifesteal", false);
        onItem.put("vanishing_curse", true);

        assertThat(DisenchantRule.suggestions(onItem, false, ""))
                .containsExactly("all", "sharpness", "rainsenchants:lifesteal");
        assertThat(DisenchantRule.suggestions(onItem, true, ""))
                .containsExactly("all", "sharpness", "rainsenchants:lifesteal", "vanishing_curse");
        assertThat(DisenchantRule.suggestions(onItem, false, "SHA")).containsExactly("sharpness");
        assertThat(DisenchantRule.suggestions(onItem, false, "life"))
                .as("a plugin enchantment is found by its own name too").containsExactly("rainsenchants:lifesteal");
        assertThat(DisenchantRule.suggestions(Map.of("vanishing_curse", true), false, "")).isEmpty();
        assertThat(DisenchantRule.suggestions(Map.of(), true, "")).isEmpty();
    }
}
