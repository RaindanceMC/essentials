package de.raindancer.modules.essentials.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BugReportsTest {

    private static final UUID ADA = UUID.fromString("c9e2c821-7b5e-44e9-a5f7-7554a4da0120");

    @Test
    @DisplayName("the areas: the game itself first, then every Rain's plugin by its readable name, Other last")
    void areas() {
        List<BugReports.Area> areas = BugReports.areas(List.of("RainsExtendedClaims", "RainsEconomy", "LuckPerms",
                "RainsCore", "ViaVersion", "RainsTPA"));
        assertThat(areas).extracting(BugReports.Area::name)
                .containsExactly("World & building", "Chat", "Economy", "Extended Claims", "TPA", "Other");
        assertThat(areas).extracting(BugReports.Area::label)
                .contains("area: Economy", "area: Extended Claims", "area: Other");
    }

    @Test
    @DisplayName("an issue's labels: what kind it is, and where")
    void labels() {
        BugReports.Area economy = new BugReports.Area("Economy", "area: Economy", "GOLD_INGOT");
        assertThat(BugReports.labels(BugReports.Kind.BUG, economy)).containsExactly("bug", "area: Economy");
        assertThat(BugReports.labels(BugReports.Kind.IDEA, economy)).containsExactly("feature request", "area: Economy");
    }

    private static BugReports.Context context() {
        return new BugReports.Context("Ada", ADA, java.time.Instant.parse("2026-11-08T15:04:05Z"), "smp", "world",
                12, 64, -30, "Java", "fabric", 42, "SURVIVAL", 1234, 37, 19.8, 41.5, "Paper 26.3-227",
                List.of("RainsCore 2.66.0", "RainsEconomy 0.70.0"));
    }

    @Test
    @DisplayName("the text: what they wrote, then every detail of the moment for debugging, and a +1 count of none")
    void body() {
        BugReports.Report report = new BugReports.Report(BugReports.Kind.BUG,
                new BugReports.Area("Economy", "area: Economy", "GOLD_INGOT"), "Shop eats coins",
                "I bought a sword and lost 300 coins.", context());
        String body = BugReports.body(report);
        assertThat(body).startsWith("I bought a sword and lost 300 coins.");
        assertThat(body).contains("Ada").contains(ADA.toString()).contains("2026-11-08 15:04:05 UTC")
                .contains("smp").contains("world 12 64 -30").contains("Java").contains("fabric").contains("42 ms")
                .contains("SURVIVAL").contains("20 h 34 min").contains("37").contains("19.8").contains("41.5 ms")
                .contains("RainsEconomy 0.70.0").contains("Paper 26.3-227").contains("Economy").contains("Bug");
        assertThat(body).contains("<!-- plus-ones: 0 -->");
        assertThat(BugReports.title(report)).isEqualTo("Shop eats coins");
    }

    @Test
    @DisplayName("a +1 says who, when and for which issue, with the same details as a report")
    void plusOneComment() {
        String comment = BugReports.plusOneComment(7, "Shop eats coins", context());
        assertThat(comment).startsWith("+1").contains("Ada").contains("#7").contains("Shop eats coins")
                .contains("2026-11-08 15:04:05 UTC").contains("20 h 34 min").contains("world 12 64 -30")
                .contains("RainsEconomy 0.70.0");
    }

    @Test
    @DisplayName("what is sent is said before sending, in the player's words")
    void disclosure() {
        assertThat(String.join(" ", BugReports.WHAT_IS_SENT)).contains("name").contains("server")
                .contains("position").contains("playtime").contains("ping").contains("versions");
    }

    @Test
    @DisplayName("one report every few minutes per player; a +1 key that does not say who it is")
    void limits() {
        long[] now = {1_000_000};
        BugReports.Cooldown cooldown = new BugReports.Cooldown(180_000, () -> now[0]);
        assertThat(cooldown.tryNow(ADA)).isZero();
        now[0] += 60_000;
        assertThat(cooldown.tryNow(ADA)).as("seconds left").isEqualTo(120);
        now[0] += 120_000;
        assertThat(cooldown.tryNow(ADA)).isZero();
        assertThat(cooldown.tryNow(UUID.randomUUID())).as("somebody else").isZero();

        String key = BugReports.reporterKey(ADA);
        assertThat(key).hasSize(16).doesNotContain(ADA.toString().substring(0, 8));
        assertThat(BugReports.reporterKey(ADA)).isEqualTo(key);
    }
}
