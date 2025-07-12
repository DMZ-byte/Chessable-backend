package dmz.chessable.config;

import dmz.chessable.Model.Game;
import dmz.chessable.Model.Users;
import dmz.chessable.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import dmz.chessable.repository.GameRepository;

@Configuration
public class LoadData {

    private static final Logger log = LoggerFactory.getLogger(LoadData.class);



    @Bean
    CommandLineRunner initDatabase(GameRepository gameRepository,UserRepository userRepository){
        return args -> {
            Users user1 = new Users("user1", "filip.domazetovski5@gmail.com","password1");
            Users user2 = new Users("user2", "filip.domazetovski7@gmail.com","password2");

            // Save users first
            user1 = userRepository.save(user1);
            user2 = userRepository.save(user2);

            // Now create and save Game with saved users
            Game game = new Game(user1, user2, "blitz");
            game = gameRepository.save(game);

            log.info("Preloaded game: " + game.getId());
        };
    }
}
