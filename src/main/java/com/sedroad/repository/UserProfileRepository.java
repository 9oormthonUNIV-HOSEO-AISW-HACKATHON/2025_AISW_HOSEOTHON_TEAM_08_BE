package com.sedroad.repository;

import com.sedroad.entity.User;
import com.sedroad.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
    Optional<UserProfile> findByUser(User user);
    Optional<UserProfile> findByUserId(String userId);
}

