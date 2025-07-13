package dmz.chessable.dto;

public class MoveRequest {
    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getUci() {
        return uci;
    }
    public MoveRequest(){

    }
    public void setUci(String uci) {
        this.uci = uci;
    }

    private Long playerId;
    private String uci;
}
