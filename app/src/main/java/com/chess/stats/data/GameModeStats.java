package com.chess.stats.data;

public final class GameModeStats {
    public final String rule;
    public final String speed;
    public final Integer currentRating;
    public final Long lastDate;
    public final Integer peakRating;
    public final Long peakDate;
    public final String peakGameUrl;
    public final Integer wins;
    public final Integer draws;
    public final Integer losses;
    public final Integer timePerMove;
    public final Double timeoutPercent;
    public final Integer tournamentCount;
    public final Integer tournamentPoints;
    public final Integer highestFinish;

    public GameModeStats(String rule, String speed, Integer currentRating, Long lastDate,
                         Integer peakRating, Long peakDate, String peakGameUrl, Integer wins,
                         Integer draws, Integer losses, Integer timePerMove, Double timeoutPercent,
                         Integer tournamentCount, Integer tournamentPoints, Integer highestFinish) {
        this.rule = rule;
        this.speed = speed;
        this.currentRating = currentRating;
        this.lastDate = lastDate;
        this.peakRating = peakRating;
        this.peakDate = peakDate;
        this.peakGameUrl = peakGameUrl;
        this.wins = wins;
        this.draws = draws;
        this.losses = losses;
        this.timePerMove = timePerMove;
        this.timeoutPercent = timeoutPercent;
        this.tournamentCount = tournamentCount;
        this.tournamentPoints = tournamentPoints;
        this.highestFinish = highestFinish;
    }

    public int getGameCount() {
        return (wins == null ? 0 : wins) + (draws == null ? 0 : draws) + (losses == null ? 0 : losses);
    }
}
