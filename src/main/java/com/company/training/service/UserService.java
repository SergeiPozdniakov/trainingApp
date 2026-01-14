package com.company.training.service;

import com.company.training.entity.User;
import com.company.training.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(User user) {
        // Проверяем, первый ли это пользователь
        boolean isFirstUser = userRepository.count() == 0;

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(isFirstUser); // Первый пользователь сразу активен
        user.setAdmin(isFirstUser); // Первый пользователь - админ
        user.setApprovedAt(isFirstUser ? LocalDateTime.now() : null);

        return userRepository.save(user);
    }

    public User approveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.setEnabled(true);
        user.setApprovedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    public List<User> getPendingUsers() {
        return userRepository.findByEnabledFalse();
    }

    public List<User> getAllActiveUsers() {
        return userRepository.findByEnabledTrue();
    }

    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }
}