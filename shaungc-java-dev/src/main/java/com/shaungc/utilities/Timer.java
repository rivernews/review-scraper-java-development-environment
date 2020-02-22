package com.shaungc.utilities;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Timer
 */
public class Timer {
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    public Timer() {
        this.start();
    }

    public void start() {
        this.startDateTime = LocalDateTime.now();
    }

    public String stop() {
        this.endDateTime = LocalDateTime.now();
        return Timer.getDurationString(this.startDateTime, this.endDateTime);
    }

    public String captureElapseDurationString() {
        final LocalDateTime nowDateTime = LocalDateTime.now();

        return Timer.getDurationString(startDateTime, nowDateTime);
    }

    private static String getDurationString(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Duration duration = Duration.between(startDateTime, endDateTime);

        return String.format(
            // "%02dh:%02dmin:%02ds.%d milliseconds",
            "*%02d:%02d:%02d.%d*",
            duration.toHoursPart(),
            duration.toMinutesPart(),
            duration.toSecondsPart(),
            duration.toMillisPart()
        );
    }
}
