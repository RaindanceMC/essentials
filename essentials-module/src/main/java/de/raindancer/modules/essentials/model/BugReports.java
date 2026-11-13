package de.raindancer.modules.essentials.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** What a bug report is made of and how it reads on GitHub. No server in here. */
public final class BugReports {

    /** What a player is told goes along with what they write. */
    public static final List<String> WHAT_IS_SENT = List.of(
            "<gray>Sent with it, to help find the bug: your name",
            "<gray>and id, the time, this server and your position,",
            "<gray>your playtime, ping, game mode and client, how",
            "<gray>the server is doing, and the versions of the",
            "<gray>server and its plugins.");

    public enum Kind {
        BUG("Bug", "bug", "Something does not work as it should."),
        IDEA("Feature request", "feature request", "Something you would like to have."),
        OTHER("Other problem", "issue", "Anything else that is in the way.");

        private final String title;
        private final String label;
        private final String hint;

        Kind(String title, String label, String hint) {
            this.title = title;
            this.label = label;
            this.hint = hint;
        }

        public String title() {
            return title;
        }

        public String label() {
            return label;
        }

        public String hint() {
            return hint;
        }
    }

    /** Where in the game it is: a plugin, or the game itself. {@code icon} names a Material. */
    public record Area(String name, String label, String icon) {
    }

    /** Everything about the moment that could help find the bug. */
    public record Context(String name, UUID id, java.time.Instant at, String server, String world, int x, int y, int z,
                          String edition, String client, int pingMs, String gameMode, long playtimeMinutes,
                          int online, double tps, double msptMs, String serverVersion, List<String> plugins) {

        public Context {
            plugins = List.copyOf(plugins);
        }
    }

    public record Report(Kind kind, Area area, String title, String description, Context context) {
    }

    private static final Map<String, String> ICONS = Map.ofEntries(
            Map.entry("Economy", "GOLD_INGOT"), Map.entry("Extended Claims", "GOLDEN_SHOVEL"),
            Map.entry("Guilds", "WHITE_BANNER"), Map.entry("Pets", "BONE"), Map.entry("Fishing", "FISHING_ROD"),
            Map.entry("Homes", "RED_BED"), Map.entry("Warps", "ENDER_PEARL"), Map.entry("Chat", "PAPER"),
            Map.entry("Cosmetics", "ELYTRA"), Map.entry("Shops", "CHEST"), Map.entry("Jobs", "IRON_PICKAXE"),
            Map.entry("Moderation", "SHIELD"), Map.entry("Enchants", "ENCHANTED_BOOK"),
            Map.entry("Skills", "EXPERIENCE_BOTTLE"), Map.entry("TPA", "COMPASS"), Map.entry("Ranks", "NAME_TAG"),
            Map.entry("Mannequins", "ARMOR_STAND"), Map.entry("One Block", "GRASS_BLOCK"),
            Map.entry("Essentials", "CRAFTING_TABLE"), Map.entry("Random Teleport", "MAP"));

    private BugReports() {
    }

    /** The game itself first, then each of Rain's plugins (Core is everywhere, so it is no place), Other last. */
    public static List<Area> areas(List<String> pluginNames) {
        List<Area> areas = new ArrayList<>();
        areas.add(area("World & building", "GRASS_BLOCK"));
        areas.add(area("Chat", "PAPER"));
        pluginNames.stream()
                .filter(name -> name.startsWith("Rains") && !name.equals("RainsCore") && name.length() > 5)
                .map(name -> readable(name.substring(5)))
                .filter(name -> !name.equals("Chat"))
                .sorted()
                .forEach(name -> areas.add(area(name, ICONS.getOrDefault(name, "BOOK"))));
        areas.add(area("Other", "BOOKSHELF"));
        return areas;
    }

    private static Area area(String name, String icon) {
        return new Area(name, "area: " + name, icon);
    }

    /** "ExtendedClaims" as "Extended Claims"; a run of capitals stays together: "TPA". */
    static String readable(String camel) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            boolean startsWord = i > 0 && Character.isUpperCase(c)
                    && (Character.isLowerCase(camel.charAt(i - 1))
                    || i + 1 < camel.length() && Character.isLowerCase(camel.charAt(i + 1)));
            if (startsWord) {
                out.append(' ');
            }
            out.append(c);
        }
        return out.toString();
    }

    /** On a Prime member's feature request: Prime's ideas are looked at first. */
    public static final String PRIME_LABEL = "prime";

    public static List<String> labels(Kind kind, Area area) {
        return List.of(kind.label(), area.label());
    }

    public static List<String> labels(Kind kind, Area area, boolean prime) {
        return prime && kind == Kind.IDEA ? List.of(kind.label(), area.label(), PRIME_LABEL) : labels(kind, area);
    }

    public static String title(Report report) {
        return report.title().strip();
    }

    public static String title(Kind kind, String title, boolean prime) {
        String plain = title.strip();
        return prime && kind == Kind.IDEA ? "💎 " + plain : plain;
    }

    public static String body(Report report) {
        StringBuilder body = new StringBuilder(report.description().strip()).append("\n\n---\n\n");
        body.append("| | |\n|---|---|\n");
        row(body, "Kind", report.kind().title());
        row(body, "Where", report.area().name());
        table(body, report.context());
        body.append("\n<sub>Filed in game with /bugreport.</sub>\n\n<!-- plus-ones: 0 -->\n");
        return body.toString();
    }

    /** "+1, this happened to me too": who, when, for which issue, and the same details as a report. */
    public static String plusOneComment(int number, String title, Context context) {
        StringBuilder comment = new StringBuilder("+1 — **").append(context.name()).append("** had #")
                .append(number).append(" (").append(title.replace("\n", " ")).append(") too.\n\n");
        comment.append("| | |\n|---|---|\n");
        table(comment, context);
        return comment.toString();
    }

    private static final java.time.format.DateTimeFormatter TIME = java.time.format.DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(java.time.ZoneOffset.UTC);

    private static void table(StringBuilder out, Context c) {
        row(out, "Player", c.name() + " (`" + c.id() + "`)");
        row(out, "When", TIME.format(c.at()));
        row(out, "Server", c.server());
        row(out, "Position", c.world() + " " + c.x() + " " + c.y() + " " + c.z());
        row(out, "Edition / client", c.edition() + " / " + c.client());
        row(out, "Ping", c.pingMs() + " ms");
        row(out, "Game mode", c.gameMode());
        row(out, "Playtime", (c.playtimeMinutes() / 60) + " h " + (c.playtimeMinutes() % 60) + " min");
        row(out, "Players online", String.valueOf(c.online()));
        row(out, "Server health", String.format(java.util.Locale.ROOT, "%.1f TPS, %.1f ms per tick", c.tps(),
                c.msptMs()));
        row(out, "Server version", c.serverVersion());
        row(out, "Plugins", String.join(", ", c.plugins()));
    }

    private static void row(StringBuilder body, String what, String value) {
        body.append("| ").append(what).append(" | ").append(value.replace("|", "\\|").replace("\n", " ")).append(" |\n");
    }

    /** For "+1, me too": the same for one player every time, and not their id. */
    public static String reporterKey(UUID player) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(("bugreport-plus-one:" + player).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /** One report per player per window. */
    public static final class Cooldown {

        private final long windowMs;
        private final LongSupplier clock;
        private final Map<UUID, Long> last = new ConcurrentHashMap<>();

        public Cooldown(long windowMs, LongSupplier clock) {
            this.windowMs = windowMs;
            this.clock = clock;
        }

        /** 0 and counted now when they may; otherwise how many seconds are left. */
        public long tryNow(UUID player) {
            long now = clock.getAsLong();
            Long before = last.get(player);
            if (before != null && now - before < windowMs) {
                return (windowMs - (now - before) + 999) / 1000;
            }
            last.put(player, now);
            return 0;
        }
    }
}
