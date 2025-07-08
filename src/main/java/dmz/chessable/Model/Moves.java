package dmz.chessable.Model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "moves")
public class Moves {
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private Boolean isStalemate = false;
    private Boolean isCapture = false;
    private Boolean isCastling = false;
    @Getter
    private String moveNotation;
    private String promotedPiece;

    private String san;


    @Enumerated(EnumType.STRING)
    @Column(name = "player_color", nullable = false)
    private PlayerColor playerColor;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "move_number", nullable = false)
    private Integer moveNumber;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Users player;

    private String fenAfterMove;

    private Long timeTaken;

    private Long timeRemaining;

    @CreationTimestamp
    private LocalDateTime timestamp;

    private Boolean isCheck = false;
    private Boolean isCheckmate = false;

    public Game getGame() {
        return game;
    }

    public Integer getMoveNumber() {
        return moveNumber;
    }

    public void setMoveNumber(Integer moveNumber) {
        this.moveNumber = moveNumber;
    }




    public String getFenAfterMove() {
        return fenAfterMove;
    }

    public void setFenAfterMove(String fenAfterMove) {
        this.fenAfterMove = fenAfterMove;
    }

    public Long getTimeTaken() {
        return timeTaken;
    }

    public void setTimeTaken(Long timeTaken) {
        this.timeTaken = timeTaken;
    }

    public Long getTimeRemaining() {
        return timeRemaining;
    }

    public void setTimeRemaining(Long timeRemaining) {
        this.timeRemaining = timeRemaining;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getCheck() {
        return isCheck;
    }

    public void setCheck(Boolean check) {
        isCheck = check;
    }

    public Boolean getCheckmate() {
        return isCheckmate;
    }

    public void setCheckmate(Boolean checkmate) {
        isCheckmate = checkmate;
    }

    public Boolean getStalemate() {
        return isStalemate;
    }

    public void setStalemate(Boolean stalemate) {
        isStalemate = stalemate;
    }

    public Boolean getCapture() {
        return isCapture;
    }

    public void setCapture(Boolean capture) {
        isCapture = capture;
    }

    public Boolean getCastling() {
        return isCastling;
    }

    public void setCastling(Boolean castling) {
        isCastling = castling;
    }

    public void setMoveNotation(String moveNotation) {
        this.moveNotation = moveNotation;
    }

    public String getPromotedPiece() {
        return promotedPiece;
    }

    public void setPromotedPiece(String promotedPiece) {
        this.promotedPiece = promotedPiece;
    }



    public Moves(){

    }
    public Moves(Game game,Integer moveNumber,Users player,String moveNotation){
        this.game = game;
        this.moveNumber = moveNumber;
        this.player = player;
        this.moveNotation = moveNotation;

    }
    @Override
    public String toString() {
        return "Move{" +
                "id=" + id +
                ", moveNumber=" + moveNumber +
                ", player=" + player +
                ", moveNotation='" + moveNotation + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
    public PlayerColor getPlayerColor() {
        return playerColor;
    }

    public void setPlayerColor(PlayerColor playerColor) {
        this.playerColor = playerColor;
    }
    public String getSan() {
        return san;
    }

    public void setSan(String san) {
        this.san = san;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public Users getPlayer() {
        return player;
    }

    public void setPlayer(Users player) {
        this.player = player;
    }

    public String getMoveNotation() {
        return moveNotation;
    }

}
