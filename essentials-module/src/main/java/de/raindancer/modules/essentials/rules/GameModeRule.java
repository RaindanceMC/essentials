package de.raindancer.modules.essentials.rules;

import de.raindancer.core.platform.rule.AbstractRule;
import de.raindancer.core.platform.rule.Verdict;
import org.bukkit.GameMode;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Which game modes somebody may switch to with vanilla {@code /gamemode} or the F3+F4 switcher. Holding none of the
 * {@code essentials.gamemode.<mode>} nodes leaves every mode open, as before; holding one limits them to the ones
 * they hold.
 */
public final class GameModeRule extends AbstractRule<GameModeRule.Request> {

    public static final String PREFIX = "essentials.gamemode.";

    public GameModeRule() {
        super("no mode node: every mode; any mode node: only the modes held");
    }

    public record Request(Predicate<String> holds, GameMode wanted) {
    }

    public static String node(GameMode mode) {
        return PREFIX + mode.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public Verdict judge(Request request) {
        boolean limited = Arrays.stream(GameMode.values()).anyMatch(mode -> request.holds().test(node(mode)));
        if (limited && !request.holds().test(node(request.wanted()))) {
            return Verdict.refused("essentials.gamemode.not-this-one");
        }
        return Verdict.allowed();
    }

    /** The mode a typed {@code /gamemode} asks for; empty for any other command or a mode vanilla would refuse. */
    public static Optional<GameMode> wanted(String typed) {
        String[] words = typed.strip().split("\\s+");
        String label = words[0].toLowerCase(Locale.ROOT);
        if (!label.equals("/gamemode") && !label.equals("/minecraft:gamemode") || words.length < 2) {
            return Optional.empty();
        }
        String mode = words[1].toLowerCase(Locale.ROOT);
        return Arrays.stream(GameMode.values()).filter(each -> each.name().toLowerCase(Locale.ROOT).equals(mode))
                .findFirst();
    }
}
