package com.hf.app.persistence.repositories;

import com.hf.app.persistence.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User, Long> {

    User findByName(String name);
}
