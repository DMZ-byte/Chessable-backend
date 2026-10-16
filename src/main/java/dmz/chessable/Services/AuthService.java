package dmz.chessable.Services;

import dmz.chessable.Model.Users;
import dmz.chessable.dto.RegistrationRequest;
import dmz.chessable.repository.UserRepository;
import jakarta.servlet.Registration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    public final UserRepository userRepository;
    public final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    public Users registerNewUser(RegistrationRequest registrationRequest){
        if(userRepository.findByUsername(registrationRequest.getUsername()).isPresent()){
            throw new RuntimeException("User already exists!");
        }
        String nickname = registrationRequest.getNickname() == null ? "" : registrationRequest.getNickname().trim();
        if (!nickname.matches("^[A-Za-z0-9_-]{3,20}$")) {
            throw new IllegalArgumentException(
                    "Nickname must be 3-20 characters: letters, numbers, _ or -.");
        }

        Users newUser = new Users();
        newUser.setUsername(registrationRequest.getUsername());
        newUser.setNickname(registrationRequest.nickname);
        newUser.setPassword(passwordEncoder.encode(registrationRequest.password));
        return userRepository.save(newUser);
    }
}
