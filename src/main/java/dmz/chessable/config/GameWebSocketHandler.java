package dmz.chessable.config;

/* @Component
public class GameWebSocketHandler<GameOverMessage> extends TextWebSocketHandler {
   @Autowired
    private ChessService gameService;

    @Override
    protected void afterConnectionEstablished(WebSocketSession session, TextMessage message){
        ObjectMapper mapper = new ObjectMapper();
        GameOverMessage moveMessage = mapper.readValue(message.getPayload(),GameMoveMessage.class);

        gameService.makeMove(moveMessage.getGameId(),moveMessage.getMove(),moveMessage.getPlayerId());
   }
}*/
