package dmz.chessable.controllers;

import dmz.chessable.Model.Users;
import dmz.chessable.Services.AuthService;
import dmz.chessable.dto.RegistrationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    @GetMapping("/userid")
    public ResponseEntity<Long> getUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Users userDetails = (Users) authentication.getPrincipal();
        Long userId = userDetails.getId();
        return ResponseEntity.ok(userId);
    }
}
