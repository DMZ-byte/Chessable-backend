package dmz.chessable.Services;

import dmz.chessable.Model.*;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import dmz.chessable.repository.MoveRepository;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.transaction.Transactional;
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
    @Transactional
    public Game applyTimeoutIfExpired(Game game) {
        if (game.getGameStatus() != GameStatus.ACTIVE || game.getLastMoveAt() == null) return game;
        boolean whiteToMove = "WHITE".equals(game.getCurrentTurn());
        Long base = whiteToMove ? game.getWhiteTimeRemaining() : game.getBlackTimeRemaining();
        if (base == null) return game;
        if (base - (System.currentTimeMillis() - game.getLastMoveAt()) > 0) return game;

        if (whiteToMove) game.setWhiteTimeRemaining(0L); else game.setBlackTimeRemaining(0L);
        game.setWinner(whiteToMove ? game.getBlackPlayer() : game.getWhitePlayer());
        game.setGameStatus(GameStatus.TIMEOUT); // use whatever your "finished" enum value is
        return this.gameRepository.save(game);
    }
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

    public static long parseInitialTimeMillis(String timeControl) {
        if (timeControl == null || timeControl.trim().isEmpty()) {
            return 5 * 60 * 1000L;
        }
        String tc = timeControl.trim().toLowerCase();
        if ("bullet".equals(tc)) {
            return 1 * 60 * 1000L;
        }
        if ("blitz".equals(tc)) {
            return 5 * 60 * 1000L;
        }
        if ("rapid".equals(tc)) {
            return 10 * 60 * 1000L;
        }
        if ("classical".equals(tc)) {
            return 30 * 60 * 1000L;
        }
        if (tc.contains("+")) {
            String[] parts = tc.split("\\+");
            try {
                long minutes = Long.parseLong(parts[0].trim());
                return minutes * 60 * 1000L;
            } catch (NumberFormatException ignored) {}
        } else if (tc.contains("|")) {
            String[] parts = tc.split("\\|");
            try {
                long minutes = Long.parseLong(parts[0].trim());
                return minutes * 60 * 1000L;
            } catch (NumberFormatException ignored) {}
        } else {
            try {
                long val = Long.parseLong(tc);
                if (val <= 180) {
                    return val * 60 * 1000L;
                } else if (val >= 60_000) {
                    return val;
                } else {
                    return val * 1000L;
                }
            } catch (NumberFormatException ignored) {}
        }
        return 5 * 60 * 1000L;
    }

    public static Integer parseIncrement(String timeControl, Integer increment) {
        if (increment != null) {
            return increment;
        }
        if (timeControl != null) {
            String tc = timeControl.trim();
            if (tc.contains("+")) {
                String[] parts = tc.split("\\+");
                if (parts.length > 1) {
                    try {
                        return Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException ignored) {}
                }
            } else if (tc.contains("|")) {
                String[] parts = tc.split("\\|");
                if (parts.length > 1) {
                    try {
                        return Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return 0;
    }

    public static String getQueueKey(String timeControl, Integer increment) {
        long initialMillis = parseInitialTimeMillis(timeControl);
        long minutes = initialMillis / (60 * 1000L);
        int inc = parseIncrement(timeControl, increment);
        return minutes + "+" + inc;
    }

    public Game createGame(Long whitePlayerId, String timeControl, List<Moves> moves) {
        return createGame(whitePlayerId, timeControl, null, moves, "WHITE");
    }

    public Game createGame(Long whitePlayerId, String timeControl, List<Moves> moves, String color) {
        return createGame(whitePlayerId, timeControl, null, moves, color);
    }

    public Game createGame(Long whitePlayerId, String timeControl, Integer increment, List<Moves> moves) {
        return createGame(whitePlayerId, timeControl, increment, moves, "WHITE");
    }

    public Game createGame(Long whitePlayerId, String timeControl, Integer increment, List<Moves> moves, String color) {
        Game game = new Game();
        Users user1 = this.userRepository.findById(whitePlayerId).orElseThrow(
                () -> new RuntimeException("Cannot find user with specified user id: " + whitePlayerId)
        );
        if ("BLACK".equalsIgnoreCase(color)) {
            game.setBlackPlayer(user1);
        } else {
            game.setWhitePlayer(user1);
        }
        game.setGameStatus(GameStatus.WAITING_FOR_PLAYER);
        game.setBoard(new Board());
        game.setFenPosition("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        game.setPgnMoves("");
        long initialTime = parseInitialTimeMillis(timeControl);
        Integer inc = parseIncrement(timeControl, increment);
        game.setWhiteTimeRemaining(initialTime);
        game.setBlackTimeRemaining(initialTime);
        game.setTimeControl(timeControl != null ? timeControl : "5");
        game.setIncrement(inc);
        game.setCurrentTurn("WHITE");
        game.setMoves(moves != null ? moves : List.of());
        return this.gameRepository.save(game);
    }

    public List<Game> getAllAvailableGames() {
        return gameRepository.findByGameStatusIn(List.of(
                GameStatus.WAITING_FOR_PLAYER,
                GameStatus.ACTIVE
        ));
    }

    public Game joinGame(Long gameId, String player2Id) {
        Game game = this.gameRepository.findById(gameId).orElseThrow(() -> new RuntimeException("Cannot find game with gameid: " + gameId));

        Users player2 = userRepository.findById(Long.parseLong(player2Id)).orElseThrow(() ->
                new RuntimeException("Cannot find player with that player id: " + player2Id)
        );
        System.out.println("Game status is : " + game.getGameStatus());
        if (game.getGameStatus() != GameStatus.WAITING_FOR_PLAYER) {
            throw new RuntimeException("Game is not joinable.");
        }
        System.out.println("Players of the game are" + game.getBlackPlayerId() + " and " + game.getWhitePlayerId());
        if (game.getBlackPlayer() == null) {
            if (player2.getId().equals(game.getWhitePlayerId())) {
                throw new RuntimeException("Cannot match with yourself");
            }
            game.setBlackPlayer(player2);
        } else if (game.getWhitePlayer() == null) {
            if (player2.getId().equals(game.getBlackPlayerId())) {
                throw new RuntimeException("Cannot match with yourself");
            }
            game.setWhitePlayer(player2);
        } else {
            System.out.println("Cannot join game, both players are not null");
            throw new RuntimeException("Cannot join game, both players are not null");
        }
        game.setGameStatus(GameStatus.ACTIVE);

        long initial = parseInitialTimeMillis(game.getTimeControl());
        game.setWhiteTimeRemaining(initial);
        game.setBlackTimeRemaining(initial);
        if (game.getIncrement() == null) {
            game.setIncrement(parseIncrement(game.getTimeControl(), null));
        }
        game.setCurrentTurn("WHITE");
        game.setLastMoveAt(System.currentTimeMillis());
        return gameRepository.save(game);
    }

    public Game updateGame(Game game) {
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
                    playerToAdd.getUsername(),
                    "/queue/match-status",
                    "You are already in an active or waiting game."
            );
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(playerId),
                    "/queue/match-status",
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
                        playerToAdd.getUsername(),
                        "/queue/match-status",
                        "You are already in the queue."
                );
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
                            playerToAdd.getUsername(),
                            "/queue/match-status",
                            "Error finding opponent, please try again."
                    );
                    messagingTemplate.convertAndSendToUser(
                            String.valueOf(playerId),
                            "/queue/match-status",
                            "Error finding opponent, please try again."
                    );
                    return Optional.empty();
                }

                Users waitingUser = this.userRepository.findById(waitingPlayerId).orElseThrow(
                        () -> new RuntimeException("Waiting player not found: " + waitingPlayerId)
                );

                // Players found: waitingPlayerId (white) and playerId (black)
                Game newGame = createGame(waitingPlayerId, "10", 0, List.of()); // Time control "10" for 10 minutes
                newGame.setBlackPlayer(playerToAdd);
                newGame.setGameStatus(GameStatus.ACTIVE); // Game is now active
                newGame.setLastMoveAt(System.currentTimeMillis());
                this.gameRepository.save(newGame); // Save the game with both players

                MatchFoundMessage whiteMsg = new MatchFoundMessage(newGame.getId(), true, playerId);
                MatchFoundMessage blackMsg = new MatchFoundMessage(newGame.getId(), false, waitingPlayerId);

                // Notify waitingPlayerId (who is white)
                messagingTemplate.convertAndSendToUser(waitingUser.getUsername(), "/queue/match-found", whiteMsg);
                messagingTemplate.convertAndSendToUser(String.valueOf(waitingPlayerId), "/queue/match-found", whiteMsg);

                // Notify playerId (who is black)
                messagingTemplate.convertAndSendToUser(playerToAdd.getUsername(), "/queue/match-found", blackMsg);
                messagingTemplate.convertAndSendToUser(String.valueOf(playerId), "/queue/match-found", blackMsg);

                // Send game state to a general topic for this game (e.g., for spectating)
                messagingTemplate.convertAndSend("/topic/game/" + newGame.getId(), GameMapper.toGameDto(newGame));

                System.out.println("Match created: Game ID " + newGame.getId() + " - " + waitingPlayerId + " vs " + playerId);
                return Optional.of(newGame);

            } else {
                // No waiting player, add current player to queue
                matchMakingQueue.offer(playerId);
                System.out.println("Player " + playerId + " added to queue. Current queue size: " + matchMakingQueue.size());
                messagingTemplate.convertAndSendToUser(
                        playerToAdd.getUsername(),
                        "/queue/match-status",
                        "Waiting for an opponent..."
                );
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(playerId),
                        "/queue/match-status",
                        "Waiting for an opponent..."
                );
                return Optional.empty();
            }
        }
    }

    private Move findLegalUciMove(Board board, String uci){
        System.out.println("SAN received to validate: " + uci);
        System.out.println("Legal moves:");
        for(Move move : board.legalMoves()){
            System.out.println(move.toString());
            if(move.toString().equalsIgnoreCase(uci)){
                return move;
            }
        }
        throw new IllegalArgumentException("Illegal move:" + uci);
    }


    public Optional<Game> getGame(Long id){
        return this.gameRepository.findById(id);
    }
    public void removePlayerFromQueue(Long playerId){
        matchMakingQueue.remove(playerId);
        System.out.println("Player " + playerId + " removed from queue. Current queue size: " + matchMakingQueue.size());

    }
    public static class TimeExpiredException extends RuntimeException {
        public TimeExpiredException(String message) {
            super(message);
        }
    }

    private void chargeMoverClock(Game game, boolean whiteMoved) {
        Long base = whiteMoved ? game.getWhiteTimeRemaining() : game.getBlackTimeRemaining();
        if (base == null) return; // untimed game

        long now = System.currentTimeMillis();
        long lastMove = game.getLastMoveAt() != null ? game.getLastMoveAt() : now;
        long elapsed = Math.max(0L, now - lastMove);
        long left = Math.max(1L, base - elapsed);
        left += (game.getIncrement() == null ? 0 : game.getIncrement()) * 1000L;

        if (whiteMoved) game.setWhiteTimeRemaining(left); else game.setBlackTimeRemaining(left);
        game.setLastMoveAt(now);
    }

    @Transactional(dontRollbackOn = TimeExpiredException.class)
    public Moves makeMove(Long gameId, String uci, Long playerId) {
        System.out.printf("Processing move: Game %d, SAN %s, Player %d%n", gameId, uci, playerId);
        Game game = this.gameRepository.findById(gameId).orElseThrow(()-> new RuntimeException("Cannot find game with gameid: "+ gameId));

        Users user = this.userRepository.findById(playerId).orElseThrow(() -> new RuntimeException("Cannot find user with userId: " + playerId));

        if (game.getGameStatus() != GameStatus.ACTIVE) {
            throw new RuntimeException("Game is not active.");
        }

        // If the side to move already ran out of time, flag the game and reject the move.
        game = applyTimeoutIfExpired(game);
        if (game.getGameStatus() != GameStatus.ACTIVE) {
            throw new TimeExpiredException("Time expired.");
        }

        boolean isWhiteTurn = game.getFenPosition().contains(" w ");
        boolean isPlayerWhite = game.getWhitePlayerId() != null && game.getWhitePlayerId().equals(playerId);
        boolean isPlayerBlack = game.getBlackPlayerId() != null && game.getBlackPlayerId().equals(playerId);

        if((isWhiteTurn && !isPlayerWhite) || (!isWhiteTurn && !isPlayerBlack)){
            throw new RuntimeException("Its not " + user +"'s turn. ");
        }

        Board board = new Board();

        board.loadFromFen(game.getFenPosition());

        Move chessMove = findLegalUciMove(board, uci);

        chargeMoverClock(game, isWhiteTurn);

        board.doMove(chessMove);
        game.setFenPosition(board.getFen());
        game.setPgnMoves((game.getPgnMoves()+" "+uci).trim());
        System.out.println(board.getSideToMove().value());
        game.setCurrentTurn(board.getSideToMove().value().equals("WHITE") ? "WHITE" : "BLACK");

        if(board.isMated()){
            game.setGameStatus(GameStatus.CHECKMATE);
            game.setWinner(user);
            System.out.println("winner is: "+game.getWinner());
        } else if (board.isStaleMate()){
            game.setGameStatus(GameStatus.STALEMATE);
        }
        this.gameRepository.save(game);
        long moveNumber = this.moveRepository.countByGame(game) +1;
        Moves moveEntity = new Moves();
        moveEntity.setGame(game);
        moveEntity.setPlayer(user);
        moveEntity.setSan(uci);
        moveEntity.setMoveNumber((int) moveNumber);
        moveEntity.setTimestamp(LocalDateTime.now());
        PlayerColor playerColor;

        if (isPlayerWhite) {
            playerColor = PlayerColor.WHITE;
            moveEntity.setPlayerColor(playerColor);
        } else if (isPlayerBlack) {
            playerColor = PlayerColor.BLACK;
            moveEntity.setPlayerColor(playerColor);
        } else {
            throw new RuntimeException("Player is neither white nor black in this game.");
        }
        System.out.println("Time control is: "+game.getTimeControl());
        System.out.println("Black remaining time is: "+game.getBlackTimeRemaining());
        System.out.println("White remaining time is: "+game.getWhiteTimeRemaining());

        return this.moveRepository.save(moveEntity);
    }

}
