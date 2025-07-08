package dmz.chessable.Model;

import lombok.Getter;

@Getter
public enum PlayerColor {
    WHITE("White"),
    BLACK("Black");

    private final String displayName;

    PlayerColor(String displayName) {
        this.displayName = displayName;
    }

    public PlayerColor opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
