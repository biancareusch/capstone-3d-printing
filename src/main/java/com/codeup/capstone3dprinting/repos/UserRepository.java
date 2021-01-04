package com.codeup.capstone3dprinting.repos;

import com.codeup.capstone3dprinting.models.File;
import com.codeup.capstone3dprinting.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByIdEquals(Long id);
    User findByPassword(String str);
    User findByUsernameIgnoreCase(String name);
    User findByEmailIgnoreCase(String email);
    List<User> findAllByisFlagged(boolean isFlagged);
    List<User> findAllByisActive(boolean isActive);
    List<User> findAllByIsAdmin(boolean isAdmin);
    User findByFiles(File files);
    User findByUsername(String username);
}
