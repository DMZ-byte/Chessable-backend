package dmz.chessable.dto;

import dmz.chessable.Model.Game;
import dmz.chessable.Model.GameStatus;
import dmz.chessable.Model.Users;

public class GameDto {
    private Long id;
    private Users whitePlayer;
    private Users blackPlayer;
    private String currentTurn;
    private String fenPosition;
    private String pgnMoves;
    private GameStatus gameStatus;
    private Long whiteTimeRemaining;
    private Long blackTimeRemaining;
    private Integer increment;
    private String timeControl;

    public GameDto(Game game) {
        this.id = game.getId();
        this.whitePlayer = game.getWhitePlayer();
        this.blackPlayer = game.getBlackPlayer();
        this.currentTurn = game.getCurrentTurn();
        this.fenPosition = game.getFenPosition();
        this.pgnMoves = game.getPgnMoves();
        this.gameStatus = game.getGameStatus();
        this.whiteTimeRemaining = game.getWhiteTimeRemaining();
        this.blackTimeRemaining = game.getBlackTimeRemaining();
        this.increment = game.getIncrement();
        this.timeControl = game.getTimeControl();
    }
    public GameDto(){

    }

    // Getters and setters below

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Users getWhitePlayer() {
        return whitePlayer;
    }

    public void setWhitePlayer(Users whitePlayer) {
        this.whitePlayer = whitePlayer;
    }

    public Users getBlackPlayer() {
        return blackPlayer;
    }

    public void setBlackPlayer(Users blackPlayer) {
        this.blackPlayer = blackPlayer;
    }

    public String getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(String currentTurn) {
        this.currentTurn = currentTurn;
    }

    public String getFenPosition() {
        return fenPosition;
    }

    public void setFenPosition(String fenPosition) {
        this.fenPosition = fenPosition;
    }

    public String getPgnMoves() {
        return pgnMoves;
    }

    public void setPgnMoves(String pgnMoves) {
        this.pgnMoves = pgnMoves;
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(GameStatus gameStatus) {
        this.gameStatus = gameStatus;
    }

    public Long getWhiteTimeRemaining() {
        return whiteTimeRemaining;
    }

    public void setWhiteTimeRemaining(Long whiteTimeRemaining) {
        this.whiteTimeRemaining = whiteTimeRemaining;
    }

    public Long getBlackTimeRemaining() {
        return blackTimeRemaining;
    }

    public void setBlackTimeRemaining(Long blackTimeRemaining) {
        this.blackTimeRemaining = blackTimeRemaining;
    }

    public Integer getIncrement() {
        return increment;
    }

    public void setIncrement(Integer increment) {
        this.increment = increment;
    }

    public String getTimeControl() {
        return timeControl;
    }

    public void setTimeControl(String timeControl) {
        this.timeControl = timeControl;
    }
}
