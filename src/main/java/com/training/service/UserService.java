package com.training.service;

import com.training.model.User;
import com.training.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Метод для регистрации нового пользователя
    @Transactional
    public User registerUser(String username, String password, String email, String fullName) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Имя пользователя уже занято");
        }

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email уже используется");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setFullName(fullName);

        // Проверяем, это первый пользователь?
        boolean isFirstUser = userRepository.count() == 0;

        if (isFirstUser) {
            // Первый пользователь - активный администратор
            user.setActive(true);
            user.setFirstAdmin(true);
            user.setApprovedAt(LocalDateTime.now());
            user.setApprovedBy("system");
        } else {
            // Остальные пользователи неактивны, пока не подтверждены
            user.setActive(false);
            user.setFirstAdmin(false);
        }

        return userRepository.save(user);
    }

    // Метод для утверждения пользователя администратором
    @Transactional
    public User approveUser(Long userId, User approver) {
        // Проверяем, что утверждающий является администратором
        if (!approver.isFirstAdmin()) {
            throw new SecurityException("Только администраторы могут утверждать пользователей");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.setActive(true);
        user.setApprovedAt(LocalDateTime.now());
        user.setApprovedBy(approver.getUsername());

        return userRepository.save(user);
    }

    // Метод для назначения пользователя администратором
    @Transactional
    public User makeAdmin(Long userId, User requester) {
        // Проверяем, что запрашивающий является администратором
        if (!requester.isFirstAdmin()) {
            throw new SecurityException("Только администраторы могут назначать других администраторов");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.setFirstAdmin(true);

        return userRepository.save(user);
    }

    // Метод для регистрации (старая версия, для совместимости с AuthController)
    @Transactional
    public User registerUser(User user) {
        return registerUser(user.getUsername(), user.getPassword(),
                user.getEmail(), user.getFullName());
    }

    // Проверка, является ли пользователь администратором
    public boolean isAdmin(User user) {
        return user.isFirstAdmin();
    }

    // Получение всех неактивных пользователей (ожидающих подтверждения)
    public List<User> getPendingApprovals() {
        return userRepository.findByActiveFalse();
    }

    // Получение всех пользователей
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Поиск пользователя по имени
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // Получение пользователя по ID
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    // Сохранение пользователя
    public User save(User user) {
        return userRepository.save(user);
    }

    // Проверка, первый ли это пользователь
    public boolean isFirstUser() {
        return userRepository.count() == 0;
    }
}