package dmz.chessable.controllers;

import dmz.chessable.Model.Game;
import dmz.chessable.Services.ChessService;
import dmz.chessable.repository.GameRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

@Controller
public class MatchMakingController {
    private final SimpMessagingTemplate messagingTemplate;
    private final GameRepository gameRepository;
    private final ChessService chessService;

    private static final Logger log = LoggerFactory.getLogger(MatchMakingController.class);
    private final Queue<String> waitingPlayers = new LinkedList<>();

    @Autowired
    public MatchMakingController(SimpMessagingTemplate messagingTemplate,
                                 GameRepository gameRepository,
                                 ChessService chessService) {
        this.messagingTemplate = messagingTemplate;
        this.gameRepository = gameRepository;
        this.chessService = chessService;
    }

    @MessageMapping("/queue/join")
    public void joinQueue(@RequestBody Map<String,String> payload) {
        // You might want to deserialize the payload properly if it's more complex
        String userId = payload.get("userId");// crude parsing, improve as needed
        log.info("User {} joined the queue", userId);

        if (!waitingPlayers.isEmpty()) {
            String player1Id = waitingPlayers.poll();
            String player2Id = userId;


            Game game = chessService.createGame(Long.parseLong(player1Id), "5", List.of());
            game.setBlackPlayerId(Long.parseLong(player2Id)); // set the second player
            gameRepository.save(game);
            Map<String,Object> matchPayload = Map.of(
                    "gameId",game.getId(),
                    "whiteId",userId,
                    "blackId",player2Id
            );
            log.info("Match found: {} vs {}", player1Id, player2Id);
            messagingTemplate.convertAndSend("/user/"+player1Id+"/queue/match-found", matchPayload);
            messagingTemplate.convertAndSend("/user/"+player2Id+"/queue/match-found", matchPayload);

            // Notify both users

        } else {
            waitingPlayers.offer(userId);
            messagingTemplate.convertAndSendToUser(userId, "/queue/match-status", "Waiting for an opponent...");
        }
    }

    @MessageMapping("/queue/leave")
    public void leaveQueue(String payloadJson) {

        String userId = payloadJson.replaceAll("[^\\w-]", "");
        waitingPlayers.remove(userId);
        log.info("User {} left the queue", userId);
    }
}
