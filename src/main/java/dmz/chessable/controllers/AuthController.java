package dmz.chessable.controllers;

import dmz.chessable.Model.Users;
import dmz.chessable.Services.AuthService;
import dmz.chessable.Services.CustomUserDetailsService;
import dmz.chessable.dto.RegistrationRequest;
import dmz.chessable.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    public AuthController(AuthService authService, UserRepository userRepository, PasswordEncoder passwordEncoder, CustomUserDetailsService userDetailsService) {

        this.authService = authService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
    }
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistrationRequest registrationRequest){
        try{
            Users registeredUser = authService.registerNewUser(registrationRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
        } catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
        catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("error", "User already exists.")
            );
        }
    }
    @PostMapping("/guest")
    public ResponseEntity<?> createGuest(HttpServletRequest request, HttpServletResponse response){
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated() && !(existing instanceof AnonymousAuthenticationToken)){
            return ResponseEntity.ok(Map.of("message", "Already authenticated"));

        }
        String username = "Guest-" + UUID.randomUUID().toString().substring(0,8);
        Users guest = new Users();
        guest.setUsername(username);
        guest.setNickname(username);
        guest.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        guest.setGuest(true);
        guest.setCreated_at(Timestamp.from(Instant.now()));
        guest = userRepository.save(guest);

        UserDetails details = userDetailsService.loadUserByUsername(username);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                details, null, details.getAuthorities()
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context,request,response);
        return ResponseEntity.ok(Map.of("id",guest.getId(),"username", username, "nickname",username,"guest", true));
    }
    @GetMapping("/userid")
    public ResponseEntity<Long> getUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        Users userDetails = (Users) authentication.getPrincipal();
        Long userId = userDetails.getId();
        return ResponseEntity.ok(userId);
    }
}
