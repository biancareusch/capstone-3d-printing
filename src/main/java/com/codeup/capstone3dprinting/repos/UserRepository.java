package com.codeup.capstone3dprinting.repos;

import com.codeup.capstone3dprinting.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByIdEquals(Long id);
    User findByUsernameIgnoreCase(String name);
    User findByEmailIgnoreCase(String email);
    User findByUsernameEquals(String name);
    List<User> findAllByisFlagged(boolean isFlagged);

}
