package com.seal.seal_server.service;

import java.util.Optional;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

// yo class ko responsibility, so validating users after login so that curl use garerw bypass garna nasakos!!
@Service
public class SessionService {
    // map links the hash value generated to the primary key in db of a user
    // string represents the hash ksy ani long is our primary key's datatype
    //concurrent because spring gets multiple response os shall  be able to handle all of them concurrently
    // on the left we use map and right concurrent hash map
    //because we want a behavoir of a map and it shall act like a concurrent hash map

    private final Map<String, Long> sessions = new ConcurrentHashMap<>();
    //to create and store a token
    public String createSession(Long userId){
        String token = UUID.randomUUID().toString();
        sessions.put(token,userId);
        return token;
    }
    // to check during session and return token when asked
    public Optional<Long> getUserId(String token){
        return Optional.ofNullable(sessions.get(token));
    }
    //to remove during logout, logout gare paxi the key shall be deleted
    public void removeSession(String token){
        sessions.remove(token);
    }
}


