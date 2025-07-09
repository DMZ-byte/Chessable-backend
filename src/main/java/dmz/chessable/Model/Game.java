package dmz.chessable.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.github.bhlangonijr.chesslib.Board;
import jakarta.persistence.*;

import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.Reference;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "games")
public class Game {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Setter
    @Transient
    private Board board;

    @Setter
    private String currentTurn;

    private Integer increment;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "white_player_id",nullable = false)
    @JsonManagedReference
    private Users whitePlayer;
    public void setFenPosition(String fenPosition) {
        this.fenPosition = fenPosition;
    }

    @Setter
    private String fenPosition = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public void setPgnMoves(String pgnMoves) {
        this.pgnMoves = pgnMoves;
    }

    @Setter
    @Column(name = "pgn_moves", columnDefinition = "TEXT")
    private String pgnMoves = "";
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "black_player_id",nullable = false)
    @JsonManagedReference
    private Users blackPlayer;

    public void setWhitePlayerId(Long whitePlayerId) {
        this.whitePlayerId = whitePlayerId;
    }

    @Setter
    @Column(name = "white_player_id", insertable = false, updatable = false)
    private Long whitePlayerId;
    @Column(name = "black_player_id", insertable = false, updatable = false)
    private Long blackPlayerId;
    @Enumerated(EnumType.STRING)
    @Column(name = "game_status", length = 20)
    private GameStatus gameStatus = GameStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Users winner;

    @Column(name = "time_control", length = 10)
    private String timeControl;

    @Column(name = "white_time_remaining")
    private Long whiteTimeRemaining;
    @Column(name = "black_time_remaining")
    private Long blackTimeRemaining;

    @Setter
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Setter
    @OneToMany(mappedBy = "game",cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    private List<Moves> moves;

    public Game(){
        this.currentTurn = "WHITE";
    }
    public Game(Users whitePlayer, Users blackPlayer, String timeControl) {
        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        // Parse time control and set initial times
        parseAndSetTimeControl(timeControl);
    }
    public Game(Users player1){
        this.whitePlayer = player1;
    }

    private void parseAndSetTimeControl(String timeControl) {
        if (timeControl != null && timeControl.contains("+")) {
            String[] parts = timeControl.split("\\+");
            int minutes = Integer.parseInt(parts[0]);
            long timeInMillis = minutes * 60 * 1000L;
            this.whiteTimeRemaining = timeInMillis;
            this.blackTimeRemaining = timeInMillis;
        }
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

    public Long getWhitePlayerId() {
        return whitePlayerId;
    }

    public Long getBlackPlayerId() {
        return blackPlayerId;
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(GameStatus gameStatus) {
        this.gameStatus = gameStatus;
    }

    public Users getWinner() {
        return winner;
    }

    public void setWinner(Users winner) {
        this.winner = winner;
    }

    public String getTimeControl() {
        return timeControl;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Moves> getMoves() {
        return moves;
    }

    public Board getBoard(){
        return this.board;
    }
    public Long getId(){
        return this.id;
    }
    public String getCurrentTurn(){
        return this.currentTurn;
    }

    public void setBlackPlayerId(Long blackPlayerId) {
        this.blackPlayerId = blackPlayerId;
    }

    public void setTimeControl(String timeControl) {
        this.timeControl = timeControl;
    }
    public String getFenPosition() { return fenPosition; }

    public String getPgnMoves() { return pgnMoves; }

    public Integer getIncrement() {
        return increment;
    }

    public void setIncrement(Integer increment) {
        this.increment = increment;
    }
    public void setBoard(Board board) {
        this.board = board;
    }

    public void setCurrentTurn(String currentTurn) {
        this.currentTurn = currentTurn;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setMoves(List<Moves> moves) {
        this.moves = moves;
    }

}
