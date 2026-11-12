package de.raindancer.modules.essentials.rules;

import de.raindancer.core.content.pack.PackStatus;

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
    public static final int PACK_PATIENCE_SECONDS = 30;

    private JoinMusicRule() {
    }

    public static boolean plays(boolean enabled, boolean playerWantsIt, boolean hasTracks) {
        return enabled && playerWantsIt && hasTracks;
    }

    public static Step step(PackStatus status, double waitedSeconds) {
        if (status.isWearing()) {
            return Step.PLAY;
        }
        if (status == PackStatus.DECLINED || status == PackStatus.FAILED || waitedSeconds >= PACK_PATIENCE_SECONDS) {
            return Step.GIVE_UP;
        }
        return Step.WAIT;
    }

    public static boolean over(long startedAt, long now, double trackSeconds) {
        return now - startedAt >= trackSeconds * 1000;
    }
}
