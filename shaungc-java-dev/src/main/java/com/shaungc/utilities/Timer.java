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

    public void stop() {
        this.endDateTime = LocalDateTime.now();
    }

    public String getElapseDurationString() {
        this.endDateTime = LocalDateTime.now();

        Duration duration = Duration.between(this.startDateTime, this.endDateTime);

        return String.format(
            "%02d hours : %02d minutes : %02d seconds . %d milliseconds",
            duration.toHoursPart(),
            duration.toMinutesPart(),
            duration.toSecondsPart(),
            duration.toMillisPart()
        );
    }
}