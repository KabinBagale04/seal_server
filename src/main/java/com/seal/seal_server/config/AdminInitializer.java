package com.seal.seal_server.config;

import com.seal.seal_server.model.User;
import com.seal.seal_server.model.UserRole;
import com.seal.seal_server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    // @value says, spring look into your config. for the values in the constructor and put its value in the variable
    @Value("${seal.admin.username}")
    private String adminUsername;
    // so adminusername ma it stores the value stored in the config file seal.admin...
    @Value("${seal.admin.password}")
    private String adminPassword;
// in the constructor we have used some interfaces, as they reside in the special container managed by spring it automatically calls them/ supplies them
    //this is called dependency injection
    private AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    // as we implement codmmandlinerunner spring le run lai call garxa and it runs
    @Override
    public void run(String... args) throws Exception {

        if(userRepository.findByUsername(adminUsername).isPresent()){
            return;
        }
        User admin = new User();

        admin.setFullName("SEAL ADMINISTRATOR");
        admin.setUsername(adminUsername);
        admin.setPasswordHash(
                passwordEncoder.encode(adminPassword)
        );
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);

        //this later line saved to the sql
        // flow is like this
        // user java object and we call userRepo..save(Admin) then it calls
        //spring data jpa then hibernate then generate sql then actual database
        //hibernate produces the actual sql
        userRepository.save(admin);

        System.out.println("initial seal administrator created.");
    }
}
