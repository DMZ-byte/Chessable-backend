package dmz.chessable;

import dmz.chessable.Model.Game;
import dmz.chessable.Model.GameStatus;
import dmz.chessable.Model.Users;
import dmz.chessable.Services.ChessService;
import dmz.chessable.controllers.MatchMakingController;
import dmz.chessable.repository.GameRepository;
import dmz.chessable.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class MatchMakingTest {

    private SimpMessagingTemplate messagingTemplate;
    private GameRepository gameRepository;
    private UserRepository userRepository;
    private ChessService chessService;
    private MatchMakingController matchMakingController;

    private Users user1;
    private Users user2;
    private Users user3;

    @BeforeEach
    public void setup() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        gameRepository = mock(GameRepository.class);
        userRepository = mock(UserRepository.class);

        user1 = new Users("alice", "alice@example.com", "pass");
        user1.setId(1L);
        user2 = new Users("bob", "bob@example.com", "pass");
        user2.setId(2L);
        user3 = new Users("charlie", "charlie@example.com", "pass");
        user3.setId(3L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user1));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user2));
        when(userRepository.findByUsername("charlie")).thenReturn(Optional.of(user3));

        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
            Game g = invocation.getArgument(0);
            if (g.getId() == null) {
                g.setId(100L);
            }
            return g;
        });

        chessService = new ChessService(mock(dmz.chessable.repository.MoveRepository.class), gameRepository, userRepository);
        matchMakingController = new MatchMakingController(messagingTemplate, gameRepository, userRepository, chessService);
    }

    @Test
    public void testTimeControlParsing() {
        assertEquals(300_000L, ChessService.parseInitialTimeMillis("5"));
        assertEquals(60_000L, ChessService.parseInitialTimeMillis("1"));
        assertEquals(120_000L, ChessService.parseInitialTimeMillis("2"));
        assertEquals(180_000L, ChessService.parseInitialTimeMillis("3"));
        assertEquals(600_000L, ChessService.parseInitialTimeMillis("10"));
        assertEquals(300_000L, ChessService.parseInitialTimeMillis("5+3"));
        assertEquals(180_000L, ChessService.parseInitialTimeMillis("3+2"));
        assertEquals(60_000L, ChessService.parseInitialTimeMillis("bullet"));
        assertEquals(300_000L, ChessService.parseInitialTimeMillis("blitz"));
        assertEquals(600_000L, ChessService.parseInitialTimeMillis("rapid"));

        assertEquals(3, ChessService.parseIncrement("5+3", null));
        assertEquals(2, ChessService.parseIncrement("3+2", null));
        assertEquals(5, ChessService.parseIncrement("5", 5));
        assertEquals(0, ChessService.parseIncrement("5", null));

        assertEquals("5+0", ChessService.getQueueKey("5", 0));
        assertEquals("5+3", ChessService.getQueueKey("5+3", null));
        assertEquals("1+0", ChessService.getQueueKey("1", 0));
        assertEquals("3+2", ChessService.getQueueKey("3+2", null));
    }

    @Test
    public void testCreateGameInitializesCorrectTimeAndIncrement() {
        Game game = chessService.createGame(1L, "5+3", List.of());
        assertEquals(300_000L, game.getWhiteTimeRemaining());
        assertEquals(300_000L, game.getBlackTimeRemaining());
        assertEquals(3, game.getIncrement());
        assertEquals("5+3", game.getTimeControl());
        assertEquals(GameStatus.WAITING_FOR_PLAYER, game.getGameStatus());
    }

    @Test
    public void testMatchMakingPairsPlayersWithSameTimeControl() {
        Principal p1 = () -> "alice";
        Principal p2 = () -> "bob";

        // Alice queues for 5 min
        matchMakingController.joinQueue(Map.of("userId", "1", "timeControl", "5", "increment", "0"), p1);

        // Verify waiting status sent
        verify(messagingTemplate, atLeastOnce()).convertAndSendToUser(eq("alice"), eq("/queue/match-status"), eq("Waiting for an opponent..."));

        // Bob queues for 5 min
        matchMakingController.joinQueue(Map.of("userId", "2", "timeControl", "5", "increment", "0"), p2);

        // Verify match created
        ArgumentCaptor<Game> gameCaptor = ArgumentCaptor.forClass(Game.class);
        verify(gameRepository, atLeastOnce()).save(gameCaptor.capture());

        Game matchedGame = gameCaptor.getAllValues().get(gameCaptor.getAllValues().size() - 1);
        assertEquals(GameStatus.ACTIVE, matchedGame.getGameStatus());
        assertEquals(user1, matchedGame.getWhitePlayer());
        assertEquals(user2, matchedGame.getBlackPlayer());
        assertEquals("5", matchedGame.getTimeControl());
        assertEquals(0, matchedGame.getIncrement());
        assertEquals(300_000L, matchedGame.getWhiteTimeRemaining());
        assertEquals(300_000L, matchedGame.getBlackTimeRemaining());
        assertNotNull(matchedGame.getLastMoveAt());

        // Verify match-found messages sent to both players (both username and ID destinations)
        verify(messagingTemplate, atLeastOnce()).convertAndSendToUser(eq("alice"), eq("/queue/match-found"), any(Map.class));
        verify(messagingTemplate, atLeastOnce()).convertAndSendToUser(eq("1"), eq("/queue/match-found"), any(Map.class));
        verify(messagingTemplate, atLeastOnce()).convertAndSendToUser(eq("bob"), eq("/queue/match-found"), any(Map.class));
        verify(messagingTemplate, atLeastOnce()).convertAndSendToUser(eq("2"), eq("/queue/match-found"), any(Map.class));
    }

    @Test
    public void testMatchMakingDoesNotMatchDifferentTimeControls() {
        Principal p1 = () -> "alice";
        Principal p2 = () -> "bob";
        Principal p3 = () -> "charlie";

        // Alice queues for 1 min
        matchMakingController.joinQueue(Map.of("userId", "1", "timeControl", "1", "increment", "0"), p1);

        // Bob queues for 5 min
        matchMakingController.joinQueue(Map.of("userId", "2", "timeControl", "5", "increment", "0"), p2);

        // No match should be found yet between Alice and Bob
        verify(messagingTemplate, never()).convertAndSendToUser(eq("alice"), eq("/queue/match-found"), any());
        verify(messagingTemplate, never()).convertAndSendToUser(eq("bob"), eq("/queue/match-found"), any());

        // Charlie queues for 1 min (matches with Alice)
        matchMakingController.joinQueue(Map.of("userId", "3", "timeControl", "1", "increment", "0"), p3);

        // Verify Alice and Charlie matched for 1 min game (60,000 ms)
        ArgumentCaptor<Game> gameCaptor = ArgumentCaptor.forClass(Game.class);
        verify(gameRepository, atLeastOnce()).save(gameCaptor.capture());

        Game matchedGame = gameCaptor.getAllValues().get(gameCaptor.getAllValues().size() - 1);
        assertEquals(user1, matchedGame.getWhitePlayer());
        assertEquals(user3, matchedGame.getBlackPlayer());
        assertEquals(60_000L, matchedGame.getWhiteTimeRemaining());
        assertEquals(60_000L, matchedGame.getBlackTimeRemaining());

        verify(messagingTemplate, atLeastOnce()).convertAndSendToUser(eq("alice"), eq("/queue/match-found"), any(Map.class));
        verify(messagingTemplate, atLeastOnce()).convertAndSendToUser(eq("charlie"), eq("/queue/match-found"), any(Map.class));
        verify(messagingTemplate, never()).convertAndSendToUser(eq("bob"), eq("/queue/match-found"), any());
    }

    @Test
    public void testLeaveQueueRemovesPlayer() {
        Principal p1 = () -> "alice";

        matchMakingController.joinQueue(Map.of("userId", "1", "timeControl", "5", "increment", "0"), p1);
        matchMakingController.leaveQueue(Map.of("userId", "1"), p1);

        verify(messagingTemplate, atLeastOnce()).convertAndSendToUser(eq("alice"), eq("/queue/match-status"), eq("Left the queue"));

        // Bob joins 5 min queue now, should not match with Alice
        Principal p2 = () -> "bob";
        matchMakingController.joinQueue(Map.of("userId", "2", "timeControl", "5", "increment", "0"), p2);

        verify(messagingTemplate, never()).convertAndSendToUser(eq("alice"), eq("/queue/match-found"), any());
        verify(messagingTemplate, never()).convertAndSendToUser(eq("bob"), eq("/queue/match-found"), any());
    }
}
