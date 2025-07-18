package dmz.chessable.controllers;

import dmz.chessable.Model.Game;
import dmz.chessable.Model.Users;
import dmz.chessable.Services.ChessService;
import dmz.chessable.repository.GameRepository;
import dmz.chessable.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;

import java.security.Principal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

@Controller
public class MatchMakingController {
    private final SimpMessagingTemplate messagingTemplate;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final ChessService chessService;

    private static final Logger log = LoggerFactory.getLogger(MatchMakingController.class);
    private final Queue<String> waitingPlayers = new LinkedList<>();

    @Autowired
    public MatchMakingController(SimpMessagingTemplate messagingTemplate,
                                 GameRepository gameRepository, UserRepository userRepository,
                                 ChessService chessService) {
        this.messagingTemplate = messagingTemplate;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
        this.chessService = chessService;
    }

    @MessageMapping("/queue/join")
    public void joinQueue(@RequestBody Map<String,String> payload, Principal principal) {
        String userId = payload.get("userId");
        log.info("User {} joined the queue", userId);
        String principalId = principal != null ? principal.getName() : "null";
        log.info("joinQueue: userId from payload = {}, principal = {}", userId, principalId);

        if (userId == null || userId.trim().isEmpty()) {
            log.error("Invalid userId received: {}", userId);
            messagingTemplate.convertAndSendToUser(principalId, "/queue/errors", "Invalid user ID");
            return;
        }

        // Check if user is already in queue
        if (waitingPlayers.contains(userId)) {
            log.warn("User {} is already in queue", userId);
            messagingTemplate.convertAndSendToUser(userId, "/queue/match-status", "You are already in the queue...");
            return;
        }

        if (!waitingPlayers.isEmpty()) {
            String player1Id = waitingPlayers.poll();
            String player2Id = userId;

            log.info("Attempting to create match between {} and {}", player1Id, player2Id);

            try {
                Users user1 = this.userRepository.findById(Long.parseLong(player1Id)).orElseThrow(
                        () -> new RuntimeException("Cannot find user with specified user id: " + player1Id)
                );
                Users user2 = this.userRepository.findById(Long.parseLong(player2Id)).orElseThrow(
                        () -> new RuntimeException("Cannot find user with specified user id: " + player2Id)
                );

                Game game = chessService.createGame(Long.parseLong(player1Id), "5", List.of());
                game.setBlackPlayer(user2);
                game.setWhitePlayer(user1);
                game = gameRepository.save(game);

                // Create the match payload with proper structure
                Map<String, Object> matchPayload = new HashMap<>();
                matchPayload.put("gameId", game.getId());
                matchPayload.put("whiteId", player1Id);
                matchPayload.put("blackId", player2Id);
                matchPayload.put("message", "Match found!");

                log.info("Match created successfully: {} vs {}, Game ID: {}", player1Id, player2Id, game.getId());
                log.info("Sending match payload: {}", matchPayload);

                // Send match found messages to both players
                messagingTemplate.convertAndSendToUser(user1.getUsername(), "/queue/match-found", matchPayload);
                messagingTemplate.convertAndSendToUser(user2.getUsername(), "/queue/match-found", matchPayload);

                log.info("Match found messages sent to both players");

            } catch (Exception e) {
                log.error("Error creating match between {} and {}: {}", player1Id, player2Id, e.getMessage());

                // Put player1 back in queue if match creation failed
                waitingPlayers.offer(player1Id);

                // Send error messages
                messagingTemplate.convertAndSendToUser(player1Id, "/queue/errors", "Match creation failed");
                messagingTemplate.convertAndSendToUser(player2Id, "/queue/errors", "Match creation failed");
            }

        } else {
            // Add to waiting queue
            waitingPlayers.offer(userId);
            log.info("User {} added to waiting queue. Current queue size: {}", userId, waitingPlayers.size());
            messagingTemplate.convertAndSendToUser(userId, "/queue/match-status", "Waiting for an opponent...");
        }
    }

    @MessageMapping("/queue/leave")
    public void leaveQueue(@RequestBody Map<String,String> payload, Principal principal) {
        String userId = payload.get("userId");
        if (userId == null && principal != null) {
            userId = principal.getName();
        }

        if (userId != null) {
            boolean removed = waitingPlayers.remove(userId);
            if (removed) {
                log.info("User {} left the queue", userId);
                messagingTemplate.convertAndSendToUser(userId, "/queue/match-status", "Left the queue");
            } else {
                log.warn("User {} was not in the queue", userId);
            }
        } else {
            log.error("No userId provided for leave queue request");
        }
    }
}