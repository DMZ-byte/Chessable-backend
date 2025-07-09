package dmz.chessable.Services;
import dmz.chessable.Model.Game;
import dmz.chessable.Model.Users;
import dmz.chessable.repository.GameRepository;
import dmz.chessable.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChessGameRegistry {
    private final ConcurrentHashMap<String, Game> activeGames = new ConcurrentHashMap<>();
    private final UserRepository userRepository;
    private final GameRepository gameRepository;

    public ChessGameRegistry(UserRepository userRepository, GameRepository gameRepository) {
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
    }

    public Game createNewGame(String player1Id){

        Game game = new Game();
        Users player1 = this.userRepository.findById(Long.parseLong(player1Id)).orElseThrow();
        game.setWhitePlayer(player1);
        System.out.print("Game created with id"+game.getId());
        return game;
    }
    public Game getGameById(String gameId){
        return activeGames.get(gameId);
    }
    public void removeGame(String gameId){
        activeGames.remove(gameId);
        System.out.print("Game "+gameId+" removed.");
    }
}
