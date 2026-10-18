package dmz.chessable.controllers;

import dmz.chessable.Model.Game;
import dmz.chessable.Model.GameStatus;
import dmz.chessable.Model.Users;
import dmz.chessable.Services.ChessService;
import dmz.chessable.Services.GameMapper;
import dmz.chessable.repository.GameRepository;
import dmz.chessable.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class MatchMakingController {
    private final SimpMessagingTemplate messagingTemplate;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final ChessService chessService;

    private static final Logger log = LoggerFactory.getLogger(MatchMakingController.class);

    public static class WaitingPlayer {
        private final String userId;
        private final String username;
        private final String principalName;
        private final String timeControl;
        private final Integer increment;

        public WaitingPlayer(String userId, String username, String principalName, String timeControl, Integer increment) {
            this.userId = userId;
            this.username = username;
            this.principalName = principalName;
            this.timeControl = timeControl;
            this.increment = increment;
        }

        public String getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getPrincipalName() { return principalName; }
        public String getTimeControl() { return timeControl; }
        public Integer getIncrement() { return increment; }
    }

    private final ConcurrentHashMap<String, Queue<WaitingPlayer>> waitingQueues = new ConcurrentHashMap<>();

    @Autowired
    public MatchMakingController(SimpMessagingTemplate messagingTemplate,
                                 GameRepository gameRepository, UserRepository userRepository,
                                 ChessService chessService) {
        this.messagingTemplate = messagingTemplate;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
        this.chessService = chessService;
    }

    private void notifyUser(Users user, String rawUserId, String principalName, String destination, Object payload) {
        Set<String> notifiedTargets = new HashSet<>();
        if (user != null && user.getUsername() != null) {
            notifiedTargets.add(user.getUsername());
        }
        if (rawUserId != null && !rawUserId.trim().isEmpty()) {
            notifiedTargets.add(rawUserId.trim());
        }
        if (user != null && user.getId() != null) {
            notifiedTargets.add(String.valueOf(user.getId()));
        }
        if (principalName != null && !principalName.trim().isEmpty() && !"null".equalsIgnoreCase(principalName)) {
            notifiedTargets.add(principalName.trim());
        }

        for (String target : notifiedTargets) {
            try {
                messagingTemplate.convertAndSendToUser(target, destination, payload);
            } catch (Exception e) {
                log.warn("Failed to notify user target {}: {}", target, e.getMessage());
            }
        }
    }

    @MessageMapping("/queue/join")
    public void joinQueue(@RequestBody Map<String,String> payload, Principal principal) {
        String userId = payload != null ? payload.get("userId") : null;
        if (userId == null && payload != null) {
            userId = payload.get("playerId");
        }
        String principalName = principal != null ? principal.getName() : null;
        log.info("joinQueue: userId = {}, principal = {}", userId, principalName);

        Users user = null;
        if (userId != null && !userId.trim().isEmpty()) {
            try {
                user = this.userRepository.findById(Long.parseLong(userId.trim())).orElse(null);
            } catch (NumberFormatException ignored) {}
        }
        if (user == null && principalName != null && !principalName.trim().isEmpty()) {
            user = this.userRepository.findByUsername(principalName.trim()).orElse(null);
        }

        if (user == null) {
            log.error("Invalid user for queue join: userId = {}, principal = {}", userId, principalName);
            String target = principalName != null ? principalName : userId;
            if (target != null) {
                messagingTemplate.convertAndSendToUser(target, "/queue/errors", "Invalid user ID");
            }
            return;
        }

        final Long resolvedUserId = user.getId();
        final String resolvedUsername = user.getUsername();

        // Extract time control and increment
        String timeControl = payload != null ? payload.get("timeControl") : null;
        if (timeControl == null && payload != null) {
            timeControl = payload.get("time");
        }
        if (timeControl == null && payload != null) {
            timeControl = payload.get("duration");
        }
        if (timeControl == null || timeControl.trim().isEmpty()) {
            timeControl = "5";
        }
        timeControl = timeControl.trim();

        Integer increment = null;
        if (payload != null) {
            String incStr = payload.get("increment");
            if (incStr == null) {
                incStr = payload.get("timeIncrement");
            }
            if (incStr != null && !incStr.trim().isEmpty()) {
                try {
                    increment = Integer.parseInt(incStr.trim());
                } catch (NumberFormatException ignored) {}
            }
        }
        increment = ChessService.parseIncrement(timeControl, increment);

        String queueKey = ChessService.getQueueKey(timeControl, increment);
        log.info("User {} ({}) joining queue with key {}", resolvedUsername, resolvedUserId, queueKey);

        synchronized (waitingQueues) {
            // Check if user is already in ANY queue
            for (Queue<WaitingPlayer> q : waitingQueues.values()) {
                for (WaitingPlayer wp : q) {
                    if (wp.getUserId().equals(String.valueOf(resolvedUserId)) || wp.getUsername().equals(resolvedUsername)) {
                        log.warn("User {} is already in queue", resolvedUsername);
                        notifyUser(user, String.valueOf(resolvedUserId), principalName, "/queue/match-status", "You are already in the queue...");
                        return;
                    }
                }
            }

            Queue<WaitingPlayer> queue = waitingQueues.computeIfAbsent(queueKey, k -> new LinkedList<>());

            if (!queue.isEmpty()) {
                WaitingPlayer player1 = queue.poll();
                WaitingPlayer player2 = new WaitingPlayer(String.valueOf(resolvedUserId), resolvedUsername, principalName, timeControl, increment);

                log.info("Attempting to create match between {} and {} for queue {}", player1.getUsername(), player2.getUsername(), queueKey);

                try {
                    Users user1 = this.userRepository.findById(Long.parseLong(player1.getUserId())).orElseThrow(
                            () -> new RuntimeException("Cannot find user with user id: " + player1.getUserId())
                    );
                    Users user2 = this.userRepository.findById(Long.parseLong(player2.getUserId())).orElseThrow(
                            () -> new RuntimeException("Cannot find user with user id: " + player2.getUserId())
                    );

                    Game game = chessService.createGame(user1.getId(), timeControl, increment, List.of());
                    game.setWhitePlayer(user1);
                    game.setBlackPlayer(user2);
                    game.setGameStatus(GameStatus.ACTIVE);
                    game.setTimeControl(timeControl);
                    game.setIncrement(increment);
                    long initialTime = ChessService.parseInitialTimeMillis(timeControl);
                    game.setWhiteTimeRemaining(initialTime);
                    game.setBlackTimeRemaining(initialTime);
                    game.setCurrentTurn("WHITE");
                    game.setLastMoveAt(System.currentTimeMillis());

                    game = gameRepository.save(game);

                    Map<String, Object> basePayload = new HashMap<>();
                    basePayload.put("gameId", game.getId());
                    basePayload.put("whiteId", String.valueOf(user1.getId()));
                    basePayload.put("blackId", String.valueOf(user2.getId()));
                    basePayload.put("whiteUsername", user1.getUsername());
                    basePayload.put("blackUsername", user2.getUsername());
                    basePayload.put("timeControl", game.getTimeControl());
                    basePayload.put("increment", game.getIncrement());
                    basePayload.put("whiteTimeRemaining", game.getWhiteTimeRemaining());
                    basePayload.put("blackTimeRemaining", game.getBlackTimeRemaining());
                    basePayload.put("message", "Match found!");

                    Map<String, Object> player1Payload = new HashMap<>(basePayload);
                    player1Payload.put("isWhitePlayer", true);
                    player1Payload.put("opponentId", user2.getId());
                    player1Payload.put("opponentUsername", user2.getUsername());

                    Map<String, Object> player2Payload = new HashMap<>(basePayload);
                    player2Payload.put("isWhitePlayer", false);
                    player2Payload.put("opponentId", user1.getId());
                    player2Payload.put("opponentUsername", user1.getUsername());

                    log.info("Match created successfully: {} vs {}, Game ID: {}", user1.getUsername(), user2.getUsername(), game.getId());

                    // Send notifications to both players
                    notifyUser(user1, player1.getUserId(), player1.getPrincipalName(), "/queue/match-found", player1Payload);
                    notifyUser(user2, player2.getUserId(), player2.getPrincipalName(), "/queue/match-found", player2Payload);

                    // Broadcast game state to game topic
                    messagingTemplate.convertAndSend("/topic/game/" + game.getId(), GameMapper.toGameDto(game));

                } catch (Exception e) {
                    log.error("Error creating match between {} and {}: {}", player1.getUserId(), player2.getUserId(), e.getMessage());

                    // Put player1 back in queue
                    queue.offer(player1);

                    // Send error messages
                    notifyUser(null, player1.getUserId(), player1.getPrincipalName(), "/queue/errors", "Match creation failed");
                    notifyUser(user, String.valueOf(resolvedUserId), principalName, "/queue/errors", "Match creation failed");
                }
            } else {
                WaitingPlayer newPlayer = new WaitingPlayer(String.valueOf(resolvedUserId), resolvedUsername, principalName, timeControl, increment);
                queue.offer(newPlayer);
                log.info("User {} added to waiting queue for {}. Current queue size: {}", resolvedUsername, queueKey, queue.size());
                notifyUser(user, String.valueOf(resolvedUserId), principalName, "/queue/match-status", "Waiting for an opponent...");
            }
        }
    }

    @MessageMapping("/queue/leave")
    public void leaveQueue(@RequestBody Map<String,String> payload, Principal principal) {
        String userId = payload != null ? payload.get("userId") : null;
        if (userId == null && payload != null) {
            userId = payload.get("playerId");
        }
        String principalName = principal != null ? principal.getName() : null;

        Users user = null;
        if (userId != null && !userId.trim().isEmpty()) {
            try {
                user = this.userRepository.findById(Long.parseLong(userId.trim())).orElse(null);
            } catch (NumberFormatException ignored) {}
        }
        if (user == null && principalName != null && !principalName.trim().isEmpty()) {
            user = this.userRepository.findByUsername(principalName.trim()).orElse(null);
        }

        String searchId = user != null ? String.valueOf(user.getId()) : userId;
        String searchUsername = user != null ? user.getUsername() : principalName;

        boolean removed = false;
        synchronized (waitingQueues) {
            for (Queue<WaitingPlayer> q : waitingQueues.values()) {
                Iterator<WaitingPlayer> it = q.iterator();
                while (it.hasNext()) {
                    WaitingPlayer wp = it.next();
                    if ((searchId != null && searchId.equals(wp.getUserId())) ||
                        (searchUsername != null && searchUsername.equals(wp.getUsername())) ||
                        (principalName != null && principalName.equals(wp.getPrincipalName()))) {
                        it.remove();
                        removed = true;
                    }
                }
            }
        }

        if (removed) {
            log.info("User {}/{} left the queue", searchId, searchUsername);
            notifyUser(user, searchId, principalName, "/queue/match-status", "Left the queue");
        } else {
            log.warn("User {}/{} was not in any queue", searchId, searchUsername);
        }
    }
}