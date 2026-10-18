package de.raindancer.modules.essentials.rules;

import de.raindancer.core.platform.rule.AbstractRule;
import de.raindancer.core.platform.rule.Verdict;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Whether an enchantment may be taken off what somebody holds with {@code /disenchant}. Unlike {@code /enchant} this
 * is for everybody — only curses stick, or a Curse of Binding would be one command away from gone.
 */
public final class DisenchantRule extends AbstractRule<DisenchantRule.Request> {

    public static final String ALL = "all";

    public DisenchantRule() {
        super("anybody takes enchantments off their own item, staff off others'; curses only with their own node");
    }

    public record Request(boolean self, boolean mayOthers, boolean holding, boolean present, boolean cursed,
                         boolean mayCurses) {

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private boolean self;
            private boolean mayOthers;
            private boolean holding;
            private boolean present;
            private boolean cursed;
            private boolean mayCurses;

            public Builder self(boolean value) {
                self = value;
                return this;
            }

            public Builder mayOthers(boolean value) {
                mayOthers = value;
                return this;
            }

            public Builder holding(boolean value) {
                holding = value;
                return this;
            }

            public Builder present(boolean value) {
                present = value;
                return this;
            }

            public Builder cursed(boolean value) {
                cursed = value;
                return this;
            }

            public Builder mayCurses(boolean value) {
                mayCurses = value;
                return this;
            }

            public Request build() {
                return new Request(self, mayOthers, holding, present, cursed, mayCurses);
            }
        }
    }

    @Override
    public Verdict judge(Request request) {
        if (!request.self() && !request.mayOthers()) {
            return Verdict.refused("essentials.enchant.not-others");
        }
        if (!request.holding()) {
            return Verdict.refused("essentials.disenchant.nothing-held");
        }
        if (!request.present()) {
            return Verdict.refused("essentials.enchant.not-on-item");
        }
        if (request.cursed() && !request.mayCurses()) {
            return Verdict.refused("essentials.disenchant.cursed");
        }
        return Verdict.allowed();
    }

    /**
     * What to offer for the first word: the enchantments on the item, by the key one types (vanilla without its
     * namespace), and {@link #ALL} while there is anything to take — matched against the key or its name alone.
     *
     * @param onItem the item's enchantments, key to whether it is a curse
     */
    public static List<String> suggestions(Map<String, Boolean> onItem, boolean mayCurses, String typed) {
        String wanted = typed == null ? "" : typed.strip().toLowerCase(Locale.ROOT);
        List<String> removable = onItem.entrySet().stream()
                .filter(each -> mayCurses || !each.getValue())
                .map(Map.Entry::getKey)
                .toList();
        List<String> options = new ArrayList<>();
        if (!removable.isEmpty()) {
            options.add(ALL);
        }
        options.addAll(removable);
        return options.stream().filter(option -> option.startsWith(wanted)
                || option.substring(option.indexOf(':') + 1).startsWith(wanted)).toList();
    }
}
