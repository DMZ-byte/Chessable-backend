package dmz.chessable.repository;

import dmz.chessable.Model.Game;
import dmz.chessable.Model.GameStatus;
import dmz.chessable.Model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game,Long> {
    @Query("SELECT g FROM Game g WHERE (g.whitePlayer = :user OR g.blackPlayer = :user) AND g.gameStatus = :status")
    List<Game> findActiveGamesByUser(@Param("user")Users user, @Param("status")GameStatus status);
    List<Game> findByGameStatus(GameStatus status);

    // Find recent games
    @Query("SELECT g FROM Game g ORDER BY g.createdAt DESC")
    List<Game> findRecentGames();

    // Find games between two players
    @Query("SELECT g FROM Game g WHERE (g.whitePlayer = :player1 AND g.blackPlayer = :player2) OR (g.whitePlayer = :player2 AND g.blackPlayer = :player1)")
    List<Game> findGamesBetweenPlayers(@Param("player1") Users player1, @Param("player2") Users player2);
    List<Game> findByGameStatusIn(List<GameStatus> statuses);
    Optional<Game> findByWhitePlayerOrBlackPlayerAndGameStatusIn(
            Users whitePlayer, Users blackPlayer, List<GameStatus> statuses);}
