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
    public Optional<Game> addPlayertoQueue(Long playerId) {
        Users playerToAdd = this.userRepository.findById(playerId).orElseThrow(() -> new RuntimeException("Player not found: " + playerId));

        // Check if player is already in an active/waiting game
        boolean pAlreadyInGame = gameRepository.findByWhitePlayerOrBlackPlayerAndGameStatusIn(
                playerToAdd, playerToAdd, List.of(GameStatus.WAITING_FOR_PLAYER, GameStatus.ACTIVE)
        ).isPresent();

        if (pAlreadyInGame) {
            System.out.println("Player " + playerId + " is already present in a game.");
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(playerId),
                    "/queue/match-status", // Using /queue as per your config
                    "You are already in an active or waiting game."
            );
            return Optional.empty();
        }

        // Synchronize access to the queue to prevent race conditions
        synchronized (matchMakingQueue) {
            // Check if player is already in the queue (e.g., from a quick double-click)
            if (matchMakingQueue.contains(playerId)) {
                System.out.println("Player " + playerId + " is already in the queue.");
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(playerId),
                        "/queue/match-status",
                        "You are already in the queue."
                );
                return Optional.empty();
            }

            Long waitingPlayerId = matchMakingQueue.poll(); // Try to get a waiting player

            if (waitingPlayerId != null) {
                // Found a waiting player, now create a match
                // Ensure waitingPlayerId is not the same as playerId (self-matching)
                if (waitingPlayerId.equals(playerId)) {
                    System.err.println("Attempted self-match, putting player back: " + playerId);
                    matchMakingQueue.offer(playerId); // Put player back in queue if it was self-match
                    messagingTemplate.convertAndSendToUser(
                            String.valueOf(playerId),
                            "/queue/match-status",
                            "Error finding opponent, please try again."
                    );
                    return Optional.empty();
                }

                // Players found: waitingPlayerId (white) and playerId (black)
                Game newGame = createGame(waitingPlayerId, playerId, "10", List.of()); // Time control "10" for 10 minutes
                newGame.setGameStatus(GameStatus.ACTIVE); // Game is now active
                this.gameRepository.save(newGame); // Save the game with both players

                // Notify waitingPlayerId (who is white)
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(waitingPlayerId),
                        "/queue/match-found", // Using /queue as per your config
                        new MatchFoundMessage(newGame.getId(), true, playerId) // true for white, opponent is playerId
                );
                // Notify playerId (who is black)
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(playerId),
                        "/queue/match-found", // Using /queue as per your config
                        new MatchFoundMessage(newGame.getId(), false, waitingPlayerId) // false for black, opponent is waitingPlayerId
                );

                // Send game state to a general topic for this game (e.g., for spectating)
                messagingTemplate.convertAndSend("/topic/game/" + newGame.getId(), newGame);

                System.out.println("Match created: Game ID " + newGame.getId() + " - " + waitingPlayerId + " vs " + playerId);
                return Optional.of(newGame);

            } else {
                // No waiting player, add current player to queue
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
