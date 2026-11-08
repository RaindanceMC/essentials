package de.raindancer.modules.essentials;

import de.raindancer.core.data.settings.Describe;
import de.raindancer.core.data.settings.In;
import de.raindancer.core.data.settings.Key;
import de.raindancer.core.data.settings.Range;
import de.raindancer.core.data.settings.Settings;
import de.raindancer.core.data.settings.Title;
import de.raindancer.core.data.settings.Topic;
import org.bukkit.Material;

/** /bugreport: reports and ideas from players, filed as GitHub issues. */
@Settings(id = "bugreports", topics = {
        @Topic(path = "bugreports", title = "Bug reports", icon = Material.WRITABLE_BOOK,
                description = "Players report bugs and ideas with /bugreport; they become GitHub issues."),
})
public record BugReportSettings(

        @In("bugreports") @Title("Bug reports")
        @Describe("Whether /bugreport takes reports at all.")
        @Key("enabled") boolean enabled,

        @In("bugreports") @Title("Repository")
        @Describe("Where the issues go, as owner/name. Keep it private: a report names the player and where "
                + "they stood.")
        @Key("repository") String repository,

        @In("bugreports") @Title("GitHub App id")
        @Describe("The id of the GitHub App installed on that repository. Its key goes into the network's "
                + "secrets: put it in plugins/RainsEssentials/github-app.pem once, and the file is taken in and "
                + "removed. 0: not set up.")
        @Range(min = 0, max = 2_000_000_000)
        @Key("app-id") int appId,

        @In("bugreports") @Title("Remind players every (minutes)")
        @Describe("A line in chat telling everybody that /bugreport exists. 0: never.")
        @Range(min = 0, max = 240)
        @Key("reminder-minutes") int reminderMinutes) {

    public static final BugReportSettings DEFAULTS = new BugReportSettings(true, "RaindanceMC/bugreports", 0, 30);
}
