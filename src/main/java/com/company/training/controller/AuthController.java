package com.company.training.controller;

import com.company.training.dto.RegistrationForm;
import com.company.training.entity.User;
import com.company.training.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String loginPage(Model model) {
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
                               BindingResult result, Model model) {

        if (result.hasErrors()) {
            return "auth/register";
        }

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.registrationForm", "Пароли не совпадают");
            return "auth/register";
        }

        if (userService.usernameExists(form.getUsername())) {
            result.rejectValue("username", "error.registrationForm", "Имя пользователя уже занято");
            return "auth/register";
        }

        if (userService.emailExists(form.getEmail())) {
            result.rejectValue("email", "error.registrationForm", "Email уже зарегистрирован");
            return "auth/register";
        }

        User user = new User();
        user.setUsername(form.getUsername());
        user.setPassword(form.getPassword());
        user.setEmail(form.getEmail());
        user.setFullName(form.getFullName());

        userService.registerUser(user);

        return "redirect:/?registered=true";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }
}