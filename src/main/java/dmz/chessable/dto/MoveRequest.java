package dmz.chessable.dto;

public class MoveRequest {
    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getSan() {
        return san;
    }
    public MoveRequest(){

    }
    public void setSan(String san) {
        this.san = san;
    }

    private Long playerId;
    private String san;
}
