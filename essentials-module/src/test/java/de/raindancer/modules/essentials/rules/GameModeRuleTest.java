package de.raindancer.modules.essentials.rules;

import org.bukkit.GameMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GameModeRuleTest {

    private final GameModeRule rule = new GameModeRule();

    private boolean may(Set<String> held, GameMode wanted) {
        return rule.judge(new GameModeRule.Request(held::contains, wanted)).isAllowed();
    }

    @Test
    @DisplayName("without any mode node every mode stays open, as vanilla /gamemode has it")
    void noModeNodes() {
        for (GameMode mode : GameMode.values()) {
            assertThat(may(Set.of(), mode)).isTrue();
        }
    }

    @Test
    @DisplayName("one mode node limits somebody to the modes they hold")
    void limited() {
        Set<String> held = Set.of("essentials.gamemode.survival", "essentials.gamemode.spectator");

        assertThat(may(held, GameMode.SURVIVAL)).isTrue();
        assertThat(may(held, GameMode.SPECTATOR)).isTrue();
        assertThat(may(held, GameMode.CREATIVE)).isFalse();
        assertThat(may(held, GameMode.ADVENTURE)).isFalse();
        assertThat(rule.judge(new GameModeRule.Request(held::contains, GameMode.CREATIVE)).reason())
                .isEqualTo("essentials.gamemode.not-this-one");
    }

    @Test
    @DisplayName("the mode is read from /gamemode in every spelling vanilla takes, and only from it")
    void readsTheCommand() {
        assertThat(GameModeRule.wanted("/gamemode spectator")).contains(GameMode.SPECTATOR);
        assertThat(GameModeRule.wanted("/minecraft:gamemode Creative Steve")).contains(GameMode.CREATIVE);
        assertThat(GameModeRule.wanted("/GAMEMODE   survival @a")).contains(GameMode.SURVIVAL);

        assertThat(GameModeRule.wanted("/gamemode")).isEmpty();
        assertThat(GameModeRule.wanted("/gamemode 1")).isEmpty();
        assertThat(GameModeRule.wanted("/gamemodes creative")).isEmpty();
        assertThat(GameModeRule.wanted("/defaultgamemode creative")).isEmpty();
        assertThat(GameModeRule.wanted("/say gamemode creative")).isEmpty();
    }

    @Test
    @DisplayName("each mode has its own node")
    void nodes() {
        assertThat(GameModeRule.node(GameMode.ADVENTURE)).isEqualTo("essentials.gamemode.adventure");
    }
}
