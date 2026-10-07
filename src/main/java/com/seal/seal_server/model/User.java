package com.seal.seal_server.model;

import jakarta.persistence.*;

//JKA is jakarta persistent API , so yo le chai standard API provide garxa to manage and persist relational data
@Entity
// table; vaneko use table users jastai in jdbc we used previously
@Table(name = "users")

public class User {
    // ID is like assigning this as primary key, and uniquely identifies the table
    @Id
    // Genarated value is like auto increment in sql, aafai generate garaxa
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //from here down, column identifies a column as simple as that, and nullable vaneko null hune condition edither true/false and uinique vaneko unique hana paryo true/false like in sql we used not null and uniuque
    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    // yo is the enum we created userrole and states ki any data in this column shall be from the enum
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId(){
        return id;
    }

    public void setId(Long id){
        this.id = id;
    }

    public String getFullName(){
        return fullName;
    }

    public void setFullName(String fullName){
        this.fullName = fullName;
    }

    public String getUsername(){
        return username;
    }

    public void setUsername(String username){
        this.username = username;
    }

    public String getPasswordHash(){
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash){
        this.passwordHash = passwordHash;
    }

    public UserRole getRole(){
        return role;
    }

    public void setRole(UserRole role){
        this.role = role;
    }

    public boolean isActive(){
        return active;
    }

    public void setActive(boolean active){
        this.active=active;
    }

}
