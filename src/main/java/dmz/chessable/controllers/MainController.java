package dmz.chessable.controllers;


import com.github.bhlangonijr.chesslib.move.Move;
import java.security.Principal;
import dmz.chessable.Model.Game;
import dmz.chessable.Model.Moves;
import dmz.chessable.Model.Users;
import dmz.chessable.Services.ChessService;
import dmz.chessable.Services.GameMapper;
import dmz.chessable.dto.GameDto;
import dmz.chessable.dto.MoveRequest;
import dmz.chessable.dto.UserDto;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
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
    public ResponseEntity<GameDto> getGameById(@PathVariable Long id) {
        Game game = gameRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Cannot find game with gameId:"+ id)
        );

        if (game == null) {
            return ResponseEntity.notFound().build();
        }

        // Convert entity to DTO here
        GameDto dto = GameMapper.toGameDto(game);

        return ResponseEntity.ok(dto);
    }
    @GetMapping("/user/{userid}")
    public ResponseEntity<Users> getUserById(@PathVariable Long userid){
        Users user = this.userRepository.findById(userid).orElseThrow(
                () -> new RuntimeException("User not found")
        );
        return ResponseEntity.ok(user);
    }
    @PostMapping("/create")
    public ResponseEntity<Game> createGame(@RequestBody Map<String,String> payload){
        if(payload.get("player1Id") != null && payload.get("player2Id") != null){
            String player1Id = payload.get("player1Id");
            String player2Id = payload.get("player2Id");
            String timeControl = payload.get("timeControl");
            String timeIncrement = payload.get("timeIncrement");
            String color = payload.get("color");
            Game newGame = this.chessService.createGame(Long.parseLong(player1Id),timeControl,List.of(),color);
            this.gameRepository.save(newGame);
            messagingTemplate.convertAndSendToUser(player2Id,
                    "queue/game-created",
                    newGame.getId()
            );
            return ResponseEntity.ok(newGame);
        } else if (payload.get("player1Id") == null){
            throw new RuntimeException("player id is equal to null");
        }

        String player1Id = payload.get("player1Id");

        String timeControl = payload.get("timeControl");
        String timeIncrement = payload.get("timeIncrement");
        String color = payload.get("color");
        Game newGame = this.chessService.createGame(Long.parseLong(player1Id),timeControl,List.of(),color);
        newGame.setIncrement(Integer.parseInt(timeIncrement));
        this.gameRepository.save(newGame);
        System.out.println("Simply made a new game instance with player1");
        return ResponseEntity.ok(newGame);
    }
    @PostMapping("/{gameId}/join")
    public ResponseEntity<Game> joinGame(@PathVariable String gameId,@RequestBody Map<String,String> payload){
        System.out.println("Attempting to join game with player2: " + payload.get("playerId"));
        String player2Id = payload.get("playerId");
        try{
            Game joinedGame = this.chessService.joinGame(Long.parseLong(gameId),player2Id);
            messagingTemplate.convertAndSend("/topic/game/" + gameId,joinedGame);
            return ResponseEntity.ok(joinedGame);
        }catch (RuntimeException e){
            System.out.println(e);
            return ResponseEntity.badRequest().body(null);
        }
    }
    @PostMapping("/{gameId}/moves")
    public ResponseEntity<Moves> makeMove(
            @PathVariable Long gameId,
            @RequestBody MoveRequest moveRequest
            ){
        Moves moves = this.chessService.makeMove(gameId,moveRequest.getUci(),moveRequest.getPlayerId());
        return ResponseEntity.ok(moves);
    }
    @GetMapping("/{gameId}/moves")
    public ResponseEntity<List<Moves>> getMoves(
            @PathVariable Long gameId
    ){
        Game game = this.gameRepository.findById(gameId).orElseThrow(RuntimeException::new);
        List<Moves> moves = game.getMoves();
        return ResponseEntity.ok(moves);
    }
    @GetMapping("/protected-info")
    public ResponseEntity<String> getProtectedInfo(Principal principal){
        if(principal != null){
            return ResponseEntity.ok("Helllo, " + principal.getName() + "! This is protected info.");
        }
        return ResponseEntity.status(401).body("Not authenticated");
    }





}
