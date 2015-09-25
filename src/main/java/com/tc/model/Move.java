package com.tc.model;

import java.util.UUID;

public class Move {

    /**
     * MoveID will contain playerID - UUID
     */
    private String moveId;

    private String playerId;

    private char direction;

    private static String generateNextMoveID(String playerId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(playerId).append('-');
        stringBuilder.append(UUID.randomUUID().toString());
        return stringBuilder.toString();
    }

}
