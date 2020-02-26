package com.shaungc.utilities;

import java.time.Duration;
import java.time.Instant;

/**
 * Timer
 */
public class Timer {
    private Instant startInstant;
    private Instant endInstant;

    private Duration durationOffset;

    private static Duration TRAVIS_BUILD_TIME_LIMIT = Duration.ofMinutes(1);

    public Timer() {
        this.start();
    }

    public Timer(String durationInMilliString) {
        this.start();
        Long durationInMilli = Long.valueOf(durationInMilliString);
        this.durationOffset = Duration.ofMillis(durationInMilli);
    }

    public void start() {
        this.startInstant = Instant.now();
    }

    public String stop() {
        this.endInstant = Instant.now();
        return this.getDurationString(this.captureCurrentSessionDuration());
    }

    public Boolean doesSessionApproachesTravisBuildLimit() {
        return this.captureCurrentSessionDuration(Instant.now()).compareTo(Timer.TRAVIS_BUILD_TIME_LIMIT) > 0;
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

    private Duration captureCurrentSessionDuration() {
        return this.captureCurrentSessionDuration(Instant.now());
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

    public void resetStart() {
        this.startInstant = Instant.now();
        this.endInstant = null;
    }
}
