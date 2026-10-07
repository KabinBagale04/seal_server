package com.seal.seal_server.config;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration//says that spring has to configure for the object, as we are using a hash function that spring will manage so config shall be used
public class PasswordConfig {
    //this tells that, if some class asks for a password encoder give it to this bcrypt password encoder
    @Bean// so bean le chai, spring calls this object and keeps it in a separate container ani everything can access this method
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
