package de.raindancer.modules.essentials.service;

import de.raindancer.core.RainsCore;
import de.raindancer.core.platform.github.GitHubApp;
import de.raindancer.core.platform.github.GitHubIssues;
import de.raindancer.core.platform.log.LogChannel;
import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.essentials.BugReportSettings;
import de.raindancer.modules.essentials.EssentialsSettings;
import de.raindancer.modules.essentials.model.BugReports;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * /bugreport behind the menus: the areas, the open issues of one (kept a minute), a new report, "+1, me too". GitHub
 * is asked off the main thread; the answer comes back on the player's own.
 */
public final class BugReportService {

    /** The GitHub App's private key, in the network's secrets. */
    static final String KEY_SECRET = "bugreports-github-app-key";
    static final String KEY_FILE = "github-app.pem";
    private static final long CACHE_MS = 60_000;

    private record Cached(List<GitHubIssues.Issue> issues, long at) {
    }

    private final Plugin plugin;
    private final RainsCore core;
    private final Messages messages;
    private final Supplier<BugReportSettings> settings;
    private final LogChannel log;
    private final BugReports.Cooldown cooldown = new BugReports.Cooldown(180_000, System::currentTimeMillis);
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    private volatile GitHubIssues issues;
    private volatile ScheduledTask reminder;
    private volatile int remindingEvery = -1;

    public BugReportService(Plugin plugin, RainsCore core, Messages messages, Supplier<BugReportSettings> settings,
                            LogChannel log) {
        this.plugin = plugin;
        this.core = core;
        this.messages = messages;
        this.settings = settings;
        this.log = log;
    }

    public void start() {
        Scheduling.async(plugin, () -> {
            takeKeyFile();
            connect();
        });
        remind();
    }

    /** Its own settings are {@link BugReportSettings}, read fresh each time; a reload connects again all the same. */
    public void settings(EssentialsSettings fresh) {
        settingsChanged();
    }

    public void settingsChanged() {
        Scheduling.async(plugin, () -> {
            takeKeyFile();
            connect();
        });
        remind();
    }

    public void stop() {
        ScheduledTask task = reminder;
        if (task != null) {
            task.cancel();
        }
        reminder = null;
    }

    public boolean enabled() {
        return settings.get().enabled();
    }

    public boolean ready() {
        return enabled() && issues != null;
    }

    /** A key file dropped in the data folder goes into the network's secrets, and the file goes away. */
    private void takeKeyFile() {
        Path file = plugin.getDataFolder().toPath().resolve(KEY_FILE);
        if (!Files.exists(file)) {
            return;
        }
        try {
            String pem = Files.readString(file, StandardCharsets.US_ASCII);
            GitHubApp.readKey(pem);
            if (core.secrets().putText(KEY_SECRET, pem)) {
                Files.delete(file);
                log.info("The GitHub App's key for bug reports is in the network's secrets now; {} was removed.",
                        KEY_FILE);
            } else {
                log.warn("The GitHub App's key could not be stored (no database?); {} stays where it is.", KEY_FILE);
            }
        } catch (IOException | java.security.GeneralSecurityException | RuntimeException unreadable) {
            log.warn("{} is not a key that can be read ({}); bug reports stay off.", KEY_FILE,
                    unreadable.getMessage());
        }
    }

    private void connect() {
        BugReportSettings now = settings.get();
        if (!now.enabled() || now.appId() <= 0) {
            issues = null;
            return;
        }
        try {
            String pem = core.secrets().text(KEY_SECRET).orElse(null);
            if (pem == null) {
                issues = null;
                log.warn("Bug reports have an app id but no key: put the GitHub App's key in plugins/{}/{}.",
                        plugin.getName(), KEY_FILE);
                return;
            }
            issues = new GitHubIssues(GitHubApp.of(now.appId(), GitHubApp.readKey(pem)));
            log.info("Bug reports go to GitHub, {}.", now.repository());
        } catch (java.security.GeneralSecurityException | RuntimeException broken) {
            issues = null;
            log.warn("The GitHub App's key could not be read ({}); bug reports stay off.", broken.getMessage());
        }
    }

    private void remind() {
        int every = settings.get().reminderMinutes();
        if (every == remindingEvery && reminder != null) {
            return;
        }
        stop();
        remindingEvery = every;
        if (every <= 0) {
            return;
        }
        long ticks = every * 60L * 20L;
        reminder = Scheduling.globalTimer(plugin, ticks, ticks, task -> {
            if (ready() && !plugin.getServer().getOnlinePlayers().isEmpty()) {
                plugin.getServer().getOnlinePlayers().forEach(player -> messages.send(player,
                        "essentials.bugreport.reminder"));
            }
        });
    }

    /** Where a report can be about, on this server. */
    public List<BugReports.Area> areas() {
        return BugReports.areas(Arrays.stream(plugin.getServer().getPluginManager().getPlugins())
                .filter(Plugin::isEnabled).map(Plugin::getName).toList());
    }

    /** The open issues of an area, to {@code then} on the viewer's thread; empty when GitHub could not be asked. */
    public void open(Player viewer, BugReports.Area area, Consumer<List<GitHubIssues.Issue>> then) {
        Cached kept = cache.get(area.label());
        if (kept != null && System.currentTimeMillis() - kept.at() < CACHE_MS) {
            then.accept(kept.issues());
            return;
        }
        GitHubIssues github = issues;
        if (github == null) {
            then.accept(List.of());
            return;
        }
        String repository = settings.get().repository();
        Scheduling.async(plugin, () -> {
            List<GitHubIssues.Issue> found;
            try {
                found = github.open(repository, area.label());
                cache.put(area.label(), new Cached(found, System.currentTimeMillis()));
            } catch (IOException | RuntimeException failed) {
                log.warn("Open bug reports could not be read: {}", failed.getMessage());
                messages.send(viewer, "essentials.bugreport.loading-failed", "reason", failed.getMessage());
                found = List.of();
            }
            List<GitHubIssues.Issue> answer = found;
            Scheduling.entity(plugin, viewer, () -> then.accept(answer));
        });
    }

    /** Files a new report, unless they sent one a moment ago. */
    public void report(Player who, BugReports.Kind kind, BugReports.Area area, String title, String description) {
        GitHubIssues github = issues;
        if (github == null) {
            messages.send(who, "essentials.bugreport.not-set-up");
            return;
        }
        long wait = cooldown.tryNow(who.getUniqueId());
        if (wait > 0) {
            messages.send(who, "essentials.bugreport.wait", "seconds", String.valueOf(wait));
            return;
        }
        BugReports.Report report = new BugReports.Report(kind, area, title, description, context(who));
        // Prime's feature requests are looked at first: marked in the title and labelled.
        boolean prime = who.hasPermission("rains.prime");
        String repository = settings.get().repository();
        messages.send(who, "essentials.bugreport.sending");
        Scheduling.async(plugin, () -> {
            try {
                int number = github.create(repository, BugReports.title(kind, report.title(), prime),
                        BugReports.body(report), BugReports.labels(kind, area, prime));
                cache.remove(area.label());
                log.info("{} filed bug report #{} ({}, {}).", who.getName(), number, kind.title(), area.name());
                messages.send(who, "essentials.bugreport.sent", "number", String.valueOf(number));
            } catch (IOException | RuntimeException failed) {
                log.warn("A bug report by {} could not be filed: {}", who.getName(), failed.getMessage());
                messages.send(who, "essentials.bugreport.failed", "reason", failed.getMessage());
            }
        });
    }

    /** The moment, read on the player's own thread: where they are, how they and the server are doing. */
    BugReports.Context context(Player who) {
        Location at = who.getLocation();
        org.bukkit.Server server = plugin.getServer();
        String client = who.getClientBrandName();
        return new BugReports.Context(who.getName(), who.getUniqueId(), java.time.Instant.now(), core.serverName(),
                at.getWorld() == null ? "?" : at.getWorld().getName(), at.getBlockX(), at.getBlockY(), at.getBlockZ(),
                de.raindancer.core.ui.menu.ClickHints.isBedrock(who.getUniqueId()) ? "Bedrock" : "Java",
                client == null ? "unknown" : client, who.getPing(), who.getGameMode().name(),
                core.playtime().minutes(who.getUniqueId()), server.getOnlinePlayers().size(), server.getTPS()[0],
                server.getAverageTickTime(), server.getName() + " " + server.getVersion(),
                Arrays.stream(server.getPluginManager().getPlugins()).filter(Plugin::isEnabled)
                        .map(p -> p.getName() + " " + p.getPluginMeta().getVersion()).sorted().toList());
    }

    /** "This happened to me too", once per player per issue. */
    public void plusOne(Player who, BugReports.Area area, GitHubIssues.Issue issue, Runnable then) {
        GitHubIssues github = issues;
        if (github == null) {
            messages.send(who, "essentials.bugreport.not-set-up");
            return;
        }
        String repository = settings.get().repository();
        String comment = BugReports.plusOneComment(issue.number(), issue.title(), context(who));
        Scheduling.async(plugin, () -> {
            try {
                GitHubIssues.PlusOne answer = github.plusOne(repository, issue.number(),
                        BugReports.reporterKey(who.getUniqueId()), comment);
                cache.remove(area.label());
                messages.send(who, answer == GitHubIssues.PlusOne.ADDED ? "essentials.bugreport.plus-one"
                        : "essentials.bugreport.plus-one-already", "number", String.valueOf(issue.number()));
            } catch (IOException | RuntimeException failed) {
                messages.send(who, "essentials.bugreport.failed", "reason", failed.getMessage());
            }
            Scheduling.entity(plugin, who, then);
        });
    }
}
