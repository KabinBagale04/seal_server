package com.seal.seal_server.repository;


import com.seal.seal_server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

//jpa repository means it manages user entities whose id type is long
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}

// jpaRepository is a ready-made-database access interface, so hamile normal dbms ma gere
//jasto repetitive code lekhna pardaina.
// so if we initialize such as extends jpa...
//we automatically get methods such as save(user), findById(id) findAll(), and more without implement those by ourselves
// so hamle pass gereko paramaters is
// user and long
// user vaneko chai entity, it describes the jpa to act on which database
// and long vaneko chai id type the uniquee identifier generally primary key ko type

// and the other question
// we never implement this interface in the adminInit wala class because
// it extends jpa...
//so spring generates the implementation in the runtime
//conceptually spring sees, UserREop and says it extends jpa...
// and it knows how to generate an object that implements these db operations
//tei vayerw we can use the function userRepo.save(user) in init wala file

//so spring is intelligent
// findByUsername vaneko hamile banako function, inbuilt xaina
// but it sees as finByUsername so it interprets find by username and know that
//username is a string and creates a query for us
// if we declared

// Optional <USER> findByFullName(String fullName) it will understand
//find user by fullname

// or List<User> findByRole(UserRole role)

//this is called derived query method

//optional<user> vaneko it is explicitly declared to return
//if hamle send gareko or the one we want to retrieve from the database
//doesn;t exist then it sends null
// ani hamle code ma access garda NullPointerException raise garxa
// so optional ma j aaye pani affect gardaina
//so optional<user> result = userRepo.findBuUserName("Admin");
// result.isPresent() function use gererw we saveguard nullExceptionpointer issue
