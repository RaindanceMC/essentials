package de.raindancer.modules.essentials.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("What Essentials' screens say")
class ScreenLineRuleTest {

    private final ScreenLineRule rule = new ScreenLineRule();

    @Test
    @DisplayName("nobody away says so")
    void nobodyAway() {
        assertThat(rule.away(List.of())).isEqualTo("<gold><bold>Away right now</bold></gold>\n<gray>Nobody — everybody is here");
    }

    @Test
    @DisplayName("the away are counted and named in order, ten at most, the rest counted")
    void away() {
        assertThat(rule.away(List.of("Bob", "Al"))).isEqualTo(
                "<gold><bold>Away right now</bold></gold>\n<gray>2 players\n<white>Al\n<white>Bob");
        assertThat(rule.away(List.of("Solo"))).contains("<gray>1 player\n");
        List<String> many = IntStream.rangeClosed(10, 25).mapToObj(n -> "P" + n).toList();
        String text = rule.away(many);
        assertThat(text.lines().count()).isEqualTo(2 + 8 + 1);
        assertThat(text).contains("<gray>16 players").contains("<white>P10").doesNotContain("P18")
                .endsWith("<dark_gray>+ 8 more");
    }

    @Test
    @DisplayName("names cannot inject tags")
    void escaped() {
        assertThat(rule.away(List.of("<red>x"))).endsWith("<white>\\<red>x");
    }

    @Test
    @DisplayName("rules are numbered as /rules numbers them, long titles cut, the rest counted")
    void rules() {
        assertThat(rule.rules(List.of())).isEqualTo("<gold><bold>Server rules</bold></gold>\n<gray>None set");
        assertThat(rule.rules(List.of("Be kind", "No griefing"))).isEqualTo(
                "<gold><bold>Server rules</bold></gold>\n<gold>1. <white>Be kind\n<gold>2. <white>No griefing");
        List<String> many = IntStream.rangeClosed(1, 12).mapToObj(n -> "R" + n).toList();
        assertThat(rule.rules(many)).contains("<gold>9. <white>R9").doesNotContain("R10")
                .endsWith("<dark_gray>+ 3 more — /rules");
        assertThat(rule.rules(List.of("x".repeat(60)))).contains("x".repeat(31) + "…").doesNotContain("x".repeat(32));
    }

    @Test
    @DisplayName("your own page shows nickname, status and ignores")
    void you() {
        assertThat(rule.you("Sky", false, 0)).isEqualTo(
                "<gold><bold>You</bold></gold>\n<gray>Nickname <white>Sky\n<gray>Status <green>here\n<gray>Ignoring <white>nobody");
        assertThat(rule.you(null, true, 2)).isEqualTo(
                "<gold><bold>You</bold></gold>\n<gray>Nickname <white>none — /nick\n<gray>Status <yellow>away\n<gray>Ignoring <white>2 players");
        assertThat(rule.you(null, true, 1)).endsWith("<white>1 player");
    }
}
