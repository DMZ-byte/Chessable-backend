// src/main/java/dmz/chessable/controllers/GameWebSocketController.java
package dmz.chessable.controllers;
import java.security.Principal;
import dmz.chessable.Model.Game;
import dmz.chessable.Model.Users;
import dmz.chessable.Services.ChessService;
import dmz.chessable.dto.MoveRequest;
import dmz.chessable.repository.UserRepository;
import org.apache.catalina.User;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    private final ChessService chessService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketController(ChessService chessService, UserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.chessService = chessService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/game/{gameId}/move")
    public void processMove(@DestinationVariable Long gameId, @Payload MoveRequest moveMessage,Principal principal) {
        if(principal == null){
            System.err.println("Unauthenticated WebSocket message received");
            return;
        }
        String authenricatedUsername = principal.getName();
        Users user = userRepository.findByUsername(authenricatedUsername).orElseThrow(RuntimeException::new);
        Long authenticatedUserId = user.getId();
        try {
            Game updatedGame = chessService.makeMove(gameId, moveMessage.getSan(), authenticatedUserId).getGame();
            messagingTemplate.convertAndSend("/topic/game/" + gameId, updatedGame);

        } catch (Exception e) {
            System.err.println("Error processing move for game " + gameId + ": " + e.getMessage());
        }
    }
}