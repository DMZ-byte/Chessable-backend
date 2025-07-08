package dmz.chessable.Services;

import dmz.chessable.Model.GameStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.core.MessagePostProcessor;

public class GameUpdateMessage implements MessagePostProcessor {
    public GameUpdateMessage(String move, String fen, GameStatus gameStatus) {
    }

    @Override
    public Message<?> postProcessMessage(Message<?> message) {
        return null;
    }
}
