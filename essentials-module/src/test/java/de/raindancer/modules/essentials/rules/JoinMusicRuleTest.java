package de.raindancer.modules.essentials.rules;

import de.raindancer.core.content.pack.PackStatus;
import de.raindancer.modules.essentials.rules.JoinMusicRule.Step;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("music when somebody logs on")
class JoinMusicRuleTest {

    @Test
    @DisplayName("plays only when the server has it on, there is a track, and the player has not switched it off")
    void plays() {
        assertThat(JoinMusicRule.plays(true, true, true)).isTrue();
        assertThat(JoinMusicRule.plays(false, true, true)).isFalse();
        assertThat(JoinMusicRule.plays(true, false, true)).isFalse();
        assertThat(JoinMusicRule.plays(true, true, false)).isFalse();
    }

    @Test
    @DisplayName("starts once the pack with the track is on, waits while it loads")
    void waitsForThePack() {
        assertThat(JoinMusicRule.step(PackStatus.LOADED, 2)).isEqualTo(Step.PLAY);
        assertThat(JoinMusicRule.step(PackStatus.ALREADY_HAD_IT, 0)).isEqualTo(Step.PLAY);
        assertThat(JoinMusicRule.step(PackStatus.SENT, 5)).isEqualTo(Step.WAIT);
        assertThat(JoinMusicRule.step(PackStatus.NOT_SENT, 1)).isEqualTo(Step.WAIT);
    }

    @Test
    @DisplayName("without the pack there is nothing to play: declined, failed or too long gives up")
    void givesUp() {
        assertThat(JoinMusicRule.step(PackStatus.DECLINED, 1)).isEqualTo(Step.GIVE_UP);
        assertThat(JoinMusicRule.step(PackStatus.FAILED, 1)).isEqualTo(Step.GIVE_UP);
        assertThat(JoinMusicRule.step(PackStatus.SENT, JoinMusicRule.PACK_PATIENCE_SECONDS)).isEqualTo(Step.GIVE_UP);
    }

    @Test
    @DisplayName("over once the track has run its length")
    void over() {
        assertThat(JoinMusicRule.over(1_000, 1_000 + 141_000, 141.5)).isFalse();
        assertThat(JoinMusicRule.over(1_000, 1_000 + 142_000, 141.5)).isTrue();
    }
}
