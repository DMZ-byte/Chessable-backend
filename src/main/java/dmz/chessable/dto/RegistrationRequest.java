package dmz.chessable.dto;

public class RegistrationRequest {
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setNickname(String nickname){this.nickname = nickname;}

    public String getNickname(){return this.nickname;}

    public String username;
    public String password;
    public String nickname;

}
