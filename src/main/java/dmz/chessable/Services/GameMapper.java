package dmz.chessable.Services;

import dmz.chessable.Model.Game;
import dmz.chessable.Model.Users;
import dmz.chessable.dto.GameDto;
import dmz.chessable.dto.UserDto;

public class GameMapper {

    public static UserDto toUserDto(Users user) {
        if (user == null) return null;
        return new UserDto(user.getId(), user.getUsername());
    }

    public static GameDto toGameDto(Game game) {
        if (game == null) return null;

        GameDto dto = new GameDto();
        dto.setId(game.getId());
        dto.setWhitePlayer(game.getWhitePlayer());
        dto.setBlackPlayer(game.getBlackPlayer());
        dto.setCurrentTurn(game.getCurrentTurn());
        dto.setFenPosition(game.getFenPosition());
        dto.setPgnMoves(game.getPgnMoves());
        dto.setGameStatus(game.getGameStatus());
        dto.setWhiteTimeRemaining(game.getWhiteTimeRemaining());
        dto.setBlackTimeRemaining(game.getBlackTimeRemaining());
        dto.setIncrement(game.getIncrement());
        dto.setTimeControl(game.getTimeControl());

        return dto;
    }
}
