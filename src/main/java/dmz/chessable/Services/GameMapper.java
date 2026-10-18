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
        return new GameDto(game);
    }
}
