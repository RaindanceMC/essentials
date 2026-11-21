package de.raindancer.modules.essentials.rules;

import de.raindancer.core.content.pack.PackStatus;
import de.raindancer.core.content.pack.PackWait;

/** When the music on logging on plays, and when it is over. */
public final class JoinMusicRule {

    public enum Step {
        /** The pack with the track is on: start. */
        PLAY,
        /** The pack is still on its way. */
        WAIT,
        /** No pack, so no track to play: never mind this time. */
        GIVE_UP
    }

    /** How long a pack may take before the music is left out. */
    public static final int PACK_PATIENCE_SECONDS = PackWait.PATIENCE_SECONDS;

    private JoinMusicRule() {
    }

    public static boolean plays(boolean enabled, boolean playerWantsIt, boolean hasTracks) {
        return enabled && playerWantsIt && hasTracks;
    }

    /** @param somethingAbove whether the player already hears something that outranks a greeting: the radio */
    public static boolean plays(boolean enabled, boolean playerWantsIt, boolean hasTracks, boolean somethingAbove) {
        return plays(enabled, playerWantsIt, hasTracks) && !somethingAbove;
    }

    public static Step step(PackStatus status, double waitedSeconds) {
        return switch (PackWait.step(status, waitedSeconds)) {
            case GO -> Step.PLAY;
            case WAIT -> Step.WAIT;
            case GIVE_UP -> Step.GIVE_UP;
        };
    }

    public static boolean over(long startedAt, long now, double trackSeconds) {
        return now - startedAt >= trackSeconds * 1000;
    }
}
