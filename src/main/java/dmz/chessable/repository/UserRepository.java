package dmz.chessable.repository;

import dmz.chessable.Model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository <Users,Long> {
    Optional<Users> findByUsername(String username);
    Optional<Users> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // Find users by rating range for matchmaking
    @Query("SELECT u FROM Users u WHERE u.rating BETWEEN :minRating AND :maxRating ORDER BY u.rating")
    List<Users> findUsersByRatingRange(int minRating, int maxRating);

    // Find top rated users
    @Query("SELECT u FROM Users u ORDER BY u.rating DESC")
    List<Users> findTopRatedUsers();
}
