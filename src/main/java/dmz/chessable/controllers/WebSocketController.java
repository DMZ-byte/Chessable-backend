// src/main/java/dmz/chessable/controllers/GameWebSocketController.java
package dmz.chessable.controllers;
import java.security.Principal;
import dmz.chessable.Model.Game;
import dmz.chessable.Model.Users;
import dmz.chessable.Services.ChessService;
import dmz.chessable.dto.GameDto;
import dmz.chessable.dto.MoveRequest;
import dmz.chessable.repository.GameRepository;
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
    private final GameRepository gameRepository;

    public WebSocketController(ChessService chessService, UserRepository userRepository, SimpMessagingTemplate messagingTemplate, GameRepository gameRepository) {
        this.chessService = chessService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.gameRepository = gameRepository;
    }

    @MessageMapping("/game/{gameId}/move")
    public void processMove(@DestinationVariable Long gameId, @Payload MoveRequest moveMessage,Principal principal) {
        System.out.println("Received move for gameId " + gameId + ": " + moveMessage.getUci() + ", made with principal: " + principal.getName() + " and message total is: " + moveMessage);
        if(principal == null){
            System.err.println("Unauthenticated WebSocket message received");
            messagingTemplate.convertAndSend("/topic/game/"+gameId+"/errors","Unauthenticated move attempt.");
            return;
        }
        String authenticatedUsername = principal.getName();
        System.out.println("Principal username: " + authenticatedUsername);
        Users user = userRepository.findById(Long.parseLong(authenticatedUsername)).orElseThrow(RuntimeException::new);
        Long authenticatedUserId = user.getId();
        try {
            Game updatedGame = chessService.makeMove(gameId, moveMessage.getUci(), authenticatedUserId).getGame();
            this.gameRepository.save(updatedGame);
            GameDto gameDto = new GameDto(updatedGame);
            messagingTemplate.convertAndSend("/topic/game/" + gameId, gameDto);

        } catch (Exception e) {
            System.err.println("Error processing move for game " + gameId + ": " + e.getMessage());
        }
    }
}