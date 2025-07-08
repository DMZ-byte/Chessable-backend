package dmz.chessable.Model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.sql.Timestamp;
import java.util.List;

@Getter
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Users {
    @GeneratedValue
    @Id
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    @NotBlank(message = "Username is required")
    @Size(min = 3,max = 20,message = "Username must be between 3 and 20 char")
    private String username;
    @Column(unique = true,nullable = false,length = 100)
    @Email
    @NotBlank(message = "Email is required")
    private String email;
    private int rating;
    private Timestamp created_at;

    @OneToMany
    @JsonBackReference
    private List<Game> games;

    public Users(){

    }
    public Users(String username,String email){
        this.username = username;
        this.email = email;
    }

}
