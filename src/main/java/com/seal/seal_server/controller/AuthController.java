package com.seal.seal_server.controller;


import com.seal.seal_server.dto.LoginRequest;
import com.seal.seal_server.dto.LoginResponse;
import com.seal.seal_server.service.AuthService;
import com.seal.seal_server.service.SessionService;
import org.hibernate.Session;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// this annotation tells spring that this class handles HTTP request for a REST API
// ani as we use LoginResponse to Spring know it shall serialize the object to json for us
@RestController
// this mapping says everything handled by this controller starts with /aapi/auth
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final SessionService sessionService;
    public AuthController(AuthService authService,
                          SessionService sessionService){
        this.authService = authService;
        this.sessionService=sessionService;
    }
    //post mapping handles http post request to /login
    // and request body says, take the json contained in the http request body and deserilize it into loginrequest
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request){// here request is a json obj, and requestbody converts it into login request obj
        LoginResponse response= authService.login(request);

        if(response.success()){
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(401).body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authorizationHeader
    ){
        if(!authorizationHeader.startsWith("Bearer ")){
            return ResponseEntity.status(401).build();
        }
        String token = authorizationHeader.substring(7);
        sessionService.removeSession(token);
        return ResponseEntity.noContent().build();
    }
}
