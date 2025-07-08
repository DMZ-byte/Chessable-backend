package dmz.chessable.repository;

import dmz.chessable.Model.Game;
import dmz.chessable.Model.Moves;
import com.github.bhlangonijr.chesslib.move.Move;
import dmz.chessable.Model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MoveRepository extends JpaRepository<Moves,Long> {
    List<Moves> findByGameOrderByMoveNumberAsc(Game game);

    List<Moves> findByGameAndPlayerOrderByMoveNumberAsc(Game game, Users player);

    // Get the last move for a game
    @Query("SELECT m FROM Moves m WHERE m.game = :game ORDER BY m.moveNumber DESC, m.timestamp DESC")
    List<Moves> findLastMoveByGame(@Param("game") Game game);

    // Count moves for a game
    long countByGame(Game game);
}
