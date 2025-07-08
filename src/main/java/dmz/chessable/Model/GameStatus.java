package dmz.chessable.Model;

public enum GameStatus {
    ACTIVE("Active"), // Currently 2 players are playing
    CHECKMATE("Checkmate"), // The game has finished and a player has won
    STALEMATE("Stalemate"), // The Game has finished and nobody has won
    DRAW("Draw"),// The Game has finished and nobody has won
    RESIGNATION("Resignation"),// The Game has finished and a player has won
    TIMEOUT("Timeout"), // The Game has finished and a player has won
    WAITING_FOR_PLAYER("Waiting_for_player"), // Game is created and a player is waiting for player 2
    ABANDONED("Abandoned"); // The Game has finished and a player has won

    private final String displayName;

    public String getDisplayName() {
        return displayName;
    }

    GameStatus(String displayName) {
        this.displayName = displayName;
    }


}
