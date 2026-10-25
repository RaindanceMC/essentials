package de.raindancer.modules.essentials.rules;

import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Comparator;
import java.util.List;

/** What Essentials' screens count and say. */
public final class ScreenLineRule {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final int AWAY_SHOWN = 8;
    private static final int RULES_SHOWN = 9;
    private static final int TITLE_SHOWN = 32;

    public String away(List<String> names) {
        String head = "<gold><bold>Away right now</bold></gold>";
        if (names.isEmpty()) {
            return head + "\n<gray>Nobody — everybody is here";
        }
        StringBuilder text = new StringBuilder(head).append("\n<gray>").append(players(names.size()));
        names.stream().sorted(Comparator.comparing(String::toLowerCase)).limit(AWAY_SHOWN)
                .forEach(name -> text.append("\n<white>").append(MINI.escapeTags(name)));
        if (names.size() > AWAY_SHOWN) {
            text.append("\n<dark_gray>+ ").append(names.size() - AWAY_SHOWN).append(" more");
        }
        return text.toString();
    }

    /** @param titles what {@code /rules} shows, in its order */
    public String rules(List<String> titles) {
        String head = "<gold><bold>Server rules</bold></gold>";
        if (titles.isEmpty()) {
            return head + "\n<gray>None set";
        }
        StringBuilder text = new StringBuilder(head);
        for (int index = 0; index < Math.min(titles.size(), RULES_SHOWN); index++) {
            String title = titles.get(index);
            if (title.length() > TITLE_SHOWN) {
                title = title.substring(0, TITLE_SHOWN - 1) + "…";
            }
            text.append("\n<gold>").append(index + 1).append(". <white>").append(MINI.escapeTags(title));
        }
        if (titles.size() > RULES_SHOWN) {
            text.append("\n<dark_gray>+ ").append(titles.size() - RULES_SHOWN).append(" more — /rules");
        }
        return text.toString();
    }

    /** @param nickname null when none is set */
    public String you(String nickname, boolean away, int ignoring) {
        return "<gold><bold>You</bold></gold>"
                + "\n<gray>Nickname <white>" + (nickname == null ? "none — /nick" : MINI.escapeTags(nickname))
                + "\n<gray>Status " + (away ? "<yellow>away" : "<green>here")
                + "\n<gray>Ignoring <white>" + (ignoring == 0 ? "nobody" : players(ignoring));
    }

    private static String players(int count) {
        return count + (count == 1 ? " player" : " players");
    }
}
