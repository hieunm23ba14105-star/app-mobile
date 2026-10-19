package com.chess.stats.data;

import java.util.Collections;
import java.util.List;

public final class LookupResult {
    public enum Error { NONE, NOT_FOUND, RATE_LIMITED, HTTP, CONNECTION, INVALID_DATA }

    public final String username;
    public final PlayerProfile profile;
    public final List<GameModeStats> gameModes;
    public final Error error;
    public final int httpCode;

    public LookupResult(String username, PlayerProfile profile, List<GameModeStats> gameModes, Error error, int httpCode) {
        this.username = username;
        this.profile = profile;
        this.gameModes = gameModes == null ? Collections.emptyList() : Collections.unmodifiableList(gameModes);
        this.error = error;
        this.httpCode = httpCode;
    }

    public boolean isSuccess() {
        return error == Error.NONE && !gameModes.isEmpty();
    }
}
