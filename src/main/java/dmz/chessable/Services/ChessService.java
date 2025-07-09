package dmz.chessable.Services;

import dmz.chessable.Model.*;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import dmz.chessable.repository.MoveRepository;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import dmz.chessable.repository.GameRepository;
import dmz.chessable.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class ChessService {
    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private UserRepository userRepository;
    @Enumerated(EnumType.STRING)
    private GameStatus gameStatus;

    private final ConcurrentLinkedQueue<Long> matchMakingQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String,String> playerToGameMap = new ConcurrentHashMap<>();

    private final MoveRepository moveRepository;

    public ChessService(MoveRepository moveRepository, GameRepository gameRepository,UserRepository userRepository) {
        this.moveRepository = moveRepository;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
    }

    public Game createGame(Long whitePlayerId, Long blackPlayerId, String timeControl,List<Moves> moves){

        Game game = new Game();
        game.setWhitePlayerId(whitePlayerId);
        game.setGameStatus(GameStatus.WAITING_FOR_PLAYER);
        game.setBoard(new Board());
        Users player1 = this.userRepository.findById(whitePlayerId).orElseThrow(RuntimeException::new);
        Users player2 = this.userRepository.findById(blackPlayerId).orElseThrow(RuntimeException::new);
        game.setWhitePlayer(player1);
        game.setBlackPlayer(player2);
        game.setBlackPlayerId(blackPlayerId);
        game.setFenPosition("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        game.setPgnMoves("");
        game.setWhiteTimeRemaining(Long.parseLong(timeControl));
        game.setBlackTimeRemaining(Long.parseLong(timeControl));
        game.setTimeControl(timeControl);
        game.setMoves(moves);
        return this.gameRepository.save(game);
    }
    public List<Game> getAllAvailableGames(){
        return gameRepository.findByGameStatusIn(List.of(
                GameStatus.WAITING_FOR_PLAYER,
                GameStatus.ACTIVE
        ));
    }

    public Game joinGame(Long gameId,String player2Id){
        Game game = gameRepository.findById(gameId).orElseThrow();
        if(Long.parseLong(player2Id) == game.getBlackPlayerId() || Long.parseLong(player2Id) == game.getWhitePlayerId()){
            throw new RuntimeException("Cannot match with yourself");
        }
        Users player2 = userRepository.findById(Long.parseLong(player2Id)).orElseThrow();
        if(game.getGameStatus() != GameStatus.WAITING_FOR_PLAYER){
            throw new RuntimeException("Game is not joinable.");
        }

        game.setBlackPlayer(player2);
        game.setGameStatus(GameStatus.ACTIVE);
        //playerToGameMap.put(player2Id,String.valueOf(gameId));
        return gameRepository.save(game);

    }
    public Game updateGame(Game game){
        return gameRepository.save(game);
    }
    public Optional<Game> addPlayertoQueue(Long playerId){
        // What a line eh? what it does is it takes the function from game repository and takes the string value of playerId and a list of game statuses
        //and it checks if the players are present or not in the game statuses, it would return optional if isPresent() wasnt there.
        Users player1 = this.userRepository.findById(playerId).orElseThrow();

        boolean palreadyinqueue = gameRepository.findByWhitePlayerOrBlackPlayerAndGameStatusIn(player1,player1,List.of(GameStatus.WAITING_FOR_PLAYER,GameStatus.ACTIVE)).isPresent();
        if(palreadyinqueue){
            System.out.print("Player " + playerId + " is already present in a game.");
            return Optional.empty();
        }
        Long waitingPlayerId = matchMakingQueue.poll();
        Users player2  = this.userRepository.findById(waitingPlayerId).orElseThrow();
        if(waitingPlayerId != null && !waitingPlayerId.equals(String.valueOf(playerId))){


            Game newGame = createGame(waitingPlayerId,playerId,"10+0",List.of()); // needs fixing

            newGame.setWhitePlayer(player1);
            newGame.setBlackPlayer(player2);
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(waitingPlayerId),
                    "/queue/match-found",
                    new MatchFoundMessage(newGame.getId(),true,playerId)
            );
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(playerId),
                    "/queue/match-found",
                    new MatchFoundMessage(newGame.getId(),false,waitingPlayerId)
            );
            messagingTemplate.convertAndSend("/topics/game"+newGame.getId(),newGame);

            this.gameRepository.save(newGame);
            return Optional.of(newGame);
            //playerToGameMap.put(newGame.getId(),newGame); // needs fixing
        }
        else {
            matchMakingQueue.offer(playerId);
            System.out.println("Player " + playerId + " added to queue. Current queue size: " + matchMakingQueue.size());
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(playerId),
                    "/queue/match-status",
                    "Waiting for an opponent..."
            );
            return Optional.empty();
        }

    }


    public Optional<Game> getGame(Long id){
        return this.gameRepository.findById(id);
    }
    public void removePlayerFromQueue(Long playerId){
        matchMakingQueue.remove(playerId);
        System.out.println("Player " + playerId + " removed from queue. Current queue size: " + matchMakingQueue.size());    }
    public Moves makeMove(Long gameId, String san, Long playerId) {
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new RuntimeException("Game not found"));
        Board board = new Board();
        Users user = this.userRepository.findById(playerId).orElseThrow(() -> new RuntimeException("User not found"));
        board.loadFromFen(game.getFenPosition());

        Move chessMove = new Move(san, board.getSideToMove());

        if (board.isMoveLegal(chessMove, true)) {
            board.doMove(chessMove);
            game.setFenPosition(board.getFen());
            game.setPgnMoves(game.getPgnMoves() + " " + san);

            if (board.isMated()) {
                game.setGameStatus(GameStatus.CHECKMATE);
                game.setWinner(user);
            } else if (board.isStaleMate()) {
                game.setGameStatus(GameStatus.STALEMATE);
            }
            gameRepository.save(game);

            Long moveNumber = moveRepository.countByGame(game) + 1;
            Moves moveEntity = new Moves();
            moveEntity.setGame(game);
            moveEntity.setPlayer(user);
            moveEntity.setSan(san);
            moveEntity.setMoveNumber(Math.toIntExact(moveNumber));
            moveEntity.setTimestamp(LocalDateTime.now());
            return moveRepository.save(moveEntity);
        }
        throw new RuntimeException("Illegal move");
    }

}
