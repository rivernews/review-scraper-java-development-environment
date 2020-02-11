package com.shaungc.utilities;


// defines strategies to handle when 
// a review collision occurs, how to deal with it
public enum ReviewCollisionStrategy {
    // only use this in debug mode, but this is not likely to affect data integrity
    // write to collided version of file, then keep going to next review
    ALWAYS_WRITE(0),

    // only use this in debug mode or unstable production version, but this is not likely to affect data integrity
    // skip and keep going to next review
    SKIP(1),

    // when scraper is stable, use this with most-recent ordering 
    // to avoid re-accessing same reviews & pages
    ABORT(2);

    final private int value;

    ReviewCollisionStrategy(Integer value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}
