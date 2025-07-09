package dmz.chessable.controllers;


import com.github.bhlangonijr.chesslib.move.Move;
import dmz.chessable.Model.Game;
import dmz.chessable.Model.Moves;
import dmz.chessable.Services.ChessService;
import dmz.chessable.dto.MoveRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import dmz.chessable.repository.GameRepository;
import dmz.chessable.repository.MoveRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import dmz.chessable.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/games")
public class MainController {
    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final UserRepository userRepository;
    private final ChessService chessService;
    private final SimpMessagingTemplate messagingTemplate;

    public MainController(GameRepository gameRepository, MoveRepository moveRepository, UserRepository userRepository, ChessService chessService, SimpMessagingTemplate messagingTemplate) {
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.userRepository = userRepository;
        this.chessService = chessService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping
    public ResponseEntity<List<Game>> home(){
        List<Game> games = this.gameRepository.findAll();
        return ResponseEntity.ok(games);
    }
    @GetMapping("/{id}")
    public Game findGame(@PathVariable Long id){
        return this.gameRepository.findById(id).orElseThrow(RuntimeException::new);
    }
    @PostMapping("/create")
    public ResponseEntity<Game> createGame(@RequestBody Map<String,String> payload){
        String player1Id = payload.get("player1Id");
        String player2Id = payload.get("player2Id");
        String timeControl = payload.get("timeControl");
        String timeIncrement = payload.get("timeIncrement");
        Game newGame = this.chessService.createGame(Long.parseLong(player1Id),Long.parseLong(player2Id),timeControl,List.of());
        newGame.setIncrement(Integer.parseInt(timeIncrement));
        this.gameRepository.save(newGame);
        messagingTemplate.convertAndSendToUser(player2Id,
                "queue/game-created",
                newGame.getId()
        );
        return ResponseEntity.ok(newGame);
    }
    @PostMapping("/{gameId}/join")
    public ResponseEntity<Game> joinGame(@PathVariable String gameId,@RequestBody Map<String,String> payload){
        String player2Id = payload.get("playerId");
        try{
            Game joinedGame = chessService.joinGame(Long.parseLong(gameId),player2Id);
            messagingTemplate.convertAndSend("/topic/game/" + gameId,joinedGame);
            return ResponseEntity.ok(joinedGame);
        }catch (RuntimeException e){
            return ResponseEntity.badRequest().body(null);
        }
    }
    @PostMapping("/{gameId}/moves")
    public ResponseEntity<Moves> makeMove(
            @PathVariable Long gameId,
            @RequestBody MoveRequest moveRequest
            ){
        Moves moves = this.chessService.makeMove(gameId,moveRequest.getSan(),moveRequest.getPlayerId());
        return ResponseEntity.ok(moves);
    }



}
