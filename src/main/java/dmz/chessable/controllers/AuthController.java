package dmz.chessable.controllers;

import dmz.chessable.Model.Users;
import dmz.chessable.Services.AuthService;
import dmz.chessable.dto.RegistrationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistrationRequest registrationRequest){
        try{
            Users registeredUser = authService.registerNewUser(registrationRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
        }catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("{\"message\": \""+ e.getMessage()+ "\"}");
        }
    }
}
