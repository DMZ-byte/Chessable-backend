package dmz.chessable.Model;

public class MatchFoundMessage {
    private Long gameId;
    private boolean isWhitePlayer;
    private Long opponentId;


    public MatchFoundMessage(Long gameId, boolean isWhitePlayer, Long opponentId){
        this.gameId = gameId;
        this.isWhitePlayer = isWhitePlayer;
        this.opponentId = opponentId;
    }
    public Long getGameId() {
        return gameId;
    }

    public boolean isWhitePlayer() {
        return isWhitePlayer;
    }

    public Long getOpponentId() {
        return opponentId;
    }


}
