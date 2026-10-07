package com.seal.seal_server.dto;
// a record is a special kind of java class designed mainly for carrying data
// if we were using a cladd instead of record we  would have to define each field and manage constructors, setters and getters
//in records java does  the most of it and this also allows us to have useful funation like equals, hashCode and more
public record LoginRequest(
        String username,
        String password
) {

}
