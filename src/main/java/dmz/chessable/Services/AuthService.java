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
        Users newUser = new Users();
        newUser.setUsername(registrationRequest.getUsername());
        newUser.setPassword(passwordEncoder.encode(registrationRequest.password));
        return userRepository.save(newUser);
    }
}
