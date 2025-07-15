// src/main/java/dmz/chessable/config/SecurityConfig.java
package dmz.chessable.config;

import dmz.chessable.Services.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity // Enables Spring Security's web security support
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    // Defines the password encoder (BCrypt is recommended for hashing passwords)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Configures the authentication provider to use our CustomUserDetailsService and PasswordEncoder
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // Configures security filters for HTTP requests
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for simplicity with React for now (READ NOTE BELOW)
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS
                .authorizeHttpRequests(auth -> auth
                        // Allow unauthenticated access to these paths
                        .requestMatchers(HttpMethod.GET,"/api/auth/userid").permitAll()
                        .requestMatchers(HttpMethod.POST, "/login").permitAll() // Allow login POST requests
                        .requestMatchers(HttpMethod.POST, "/register").permitAll() // If you add a registration endpoint
                        .requestMatchers(HttpMethod.POST,"/api/games/{gameid}/join").permitAll()
                        .requestMatchers("/ws/**").permitAll() // Allow WebSocket handshake (STOMP over WS)
                        .requestMatchers("/api/games").permitAll() // Allow viewing all games without auth (optional)
                        .requestMatchers("/api/games/{id}").permitAll() // Allow viewing specific game without auth (optional)
                        .requestMatchers("/api/games/user/**").permitAll()
                        .requestMatchers("/api/games/{gameid}/moves").permitAll()
                        .requestMatchers("/game/**").permitAll()
                        .requestMatchers("/api/games/create").permitAll() // Temporarily allow create without auth for testing
                        .requestMatchers(HttpMethod.POST,"/api/auth/register").permitAll()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginProcessingUrl("/login") // URL where the login form POSTs to
                        .usernameParameter("username") // Name of the username field in the form
                        .passwordParameter("password") // Name of the password field in the form
                        .successHandler((request, response, authentication) -> {
                            // On successful login, send a 200 OK response with user info (e.g., username, ID)
                            response.setStatus(200);
                            response.setContentType("application/json");
                            // You might want to return a more detailed User DTO here
                            response.getWriter().write("{\"message\": \"Login successful\", \"username\": \"" + authentication.getName() + "\"}");
                        })
                        .failureHandler((request, response, exception) -> {
                            // On failed login, send a 401 Unauthorized response
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\": \"Login failed: " + exception.getMessage() + "\"}");
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/logout") // URL to logout
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(200);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\": \"Logout successful\"}");
                        })
                );
        return http.build();
    }

    // Configure CORS to allow requests from your React frontend
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000")); // Allow your React app's origin
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*")); // Allow all headers
        configuration.setAllowCredentials(true); // Allow cookies, authorization headers, etc.
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply to all paths
        return source;
    }
}