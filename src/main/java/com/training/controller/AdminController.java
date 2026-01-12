package com.training.controller;

import com.training.model.User;
import com.training.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @GetMapping("/dashboard")
    public String adminDashboard(@AuthenticationPrincipal User currentUser, Model model) {
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("pendingUsers", userService.getPendingApprovals());
        model.addAttribute("allUsers", userService.getAllUsers());
        return "admin/dashboard";
    }

    @PostMapping("/approve/{userId}")
    public String approveUser(@PathVariable Long userId,
                              @AuthenticationPrincipal User approver) {
        userService.approveUser(userId, approver);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/make-admin/{userId}")
    public String makeAdmin(@PathVariable Long userId,
                            @AuthenticationPrincipal User requester) {
        userService.makeAdmin(userId, requester);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/deactivate/{userId}")
    public String deactivateUser(@PathVariable Long userId,
                                 @AuthenticationPrincipal User requester) {
        if (!requester.isFirstAdmin()) {
            throw new SecurityException("Только администраторы могут деактивировать пользователей");
        }

        userService.findById(userId).ifPresent(user -> {
            user.setActive(false);
            userService.save(user);
        });

        return "redirect:/admin/dashboard";
    }
}