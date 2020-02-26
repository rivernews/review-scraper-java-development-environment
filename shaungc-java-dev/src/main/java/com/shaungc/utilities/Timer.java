package com.shaungc.utilities;

import java.time.Duration;
import java.time.Instant;

/**
 * Timer
 */
public class Timer {
    private Instant startInstant;
    private Instant endInstant;

    private final Duration durationOffset;

    private final Duration countdownDuration;

    public Timer(final Duration countdownDuration) {
        this.countdownDuration = countdownDuration;
        this.durationOffset = null;

        this.start();
    }

    public Timer(String durationInMilliString, final Duration countDownDuration) {
        this.countdownDuration = countDownDuration;

        this.start();

        Long durationInMilli = Long.valueOf(durationInMilliString);
        this.durationOffset = Duration.ofMillis(durationInMilli);
    }

    public void start() {
        this.startInstant = Instant.now();
    }

    public String stop() {
        this.endInstant = Instant.now();
        return this.getDurationString(this.captureCurrentSessionDuration(this.endInstant));
    }

    public Boolean doesReachCountdownDuration() {
        return this.captureCurrentSessionDuration(Instant.now()).compareTo(this.countdownDuration) > 0;
    }

    private Duration captureCurrentSessionDuration(Instant endInstant) {
        return Duration.between(this.startInstant, endInstant);
    }

    private Duration captureOverallDuration(Instant endInstant) {
        Duration duration = Duration.between(this.startInstant, endInstant);

        if (this.durationOffset != null) {
            duration = duration.plus(this.durationOffset);
        }

        return duration;
    }

    public String captureOverallElapseDurationString() {
        final Duration duration = this.captureOverallDuration(Instant.now());
        return this.getDurationString(duration);
    }

    public String captureOverallElapseDurationInMilliAsString() {
        final Duration duration = this.captureOverallDuration(Instant.now());
        return String.format("%s", duration.toMillis());
    }

    private String getDurationString(final Duration duration) {
        return String.format(
            // "%02dh:%02dmin:%02ds.%d milliseconds",
            "*%02d:%02d:%02d.%d*",
            duration.toHoursPart(),
            duration.toMinutesPart(),
            duration.toSecondsPart(),
            duration.toMillisPart()
        );
    }

    public void restart() {
        this.startInstant = Instant.now();
        this.endInstant = null;
    }
}
