package com.codeup.capstone3dprinting.controllers;


import com.codeup.capstone3dprinting.models.ConfirmationToken;
import com.codeup.capstone3dprinting.models.User;
import com.codeup.capstone3dprinting.repos.ConfirmationTokenRepository;
import com.codeup.capstone3dprinting.repos.UserRepository;
import com.codeup.capstone3dprinting.services.EmailService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Timestamp;
import java.util.Date;


@Controller
public class AuthenticationController {

    private final EmailService emailService;
    private final ConfirmationTokenRepository tokenDao;
    private final UserRepository userDao;
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String showLoginForm() {
        return "users/login";
    }

    public AuthenticationController(EmailService emailService, ConfirmationTokenRepository tokenDao,
                                    UserRepository userDao, PasswordEncoder passwordEncoder) {
        this.emailService = emailService;
        this.tokenDao = tokenDao;
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    //TODO: logged in user can still directly access this url
    @GetMapping("/sign-up")
    public String showSignupForm(Model model) {
        model.addAttribute("user", new User());
        return "users/sign-up";
    }

    @PostMapping("/sign-up")
    public String saveUser(@ModelAttribute User user, Model model, @RequestParam(name = "confirmPassword") String confirmPassword) {
        //TODO: need to give user an error message
        if (!user.getPassword().equals(confirmPassword)) {
            return "redirect:/sign-up";
        }

        String hash = passwordEncoder.encode(user.getPassword());

        //user is passed in from the form
        user.setPassword(hash);
        user.setAvatarUrl("/image/placeholder-avatar.jpg");
        user.setAdmin(false);
        user.setVerified(false);
        user.setActive(true);
        user.setJoinedAt(new Timestamp(new Date().getTime()));

        User existingUserEmail = userDao.findByEmailIgnoreCase(user.getEmail());
        User existingUsername = userDao.findByUsernameIgnoreCase(user.getUsername());

        if (existingUserEmail != null || existingUsername != null) {

            // TODO: give the user a more detailed message about why account creation failed
            return "redirect:/login/?fail";

        } else {
            userDao.save(user);

            ConfirmationToken confirmationToken = new ConfirmationToken(user);

            tokenDao.save(confirmationToken);

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(user.getEmail());
            mailMessage.setSubject("Complete Registration!");
            mailMessage.setFrom("no-reply@squarecubed.xyz");
            mailMessage.setText("To confirm your account, please click here : "
                    + "http://squarecubed.xyz/confirm-account?token=" + confirmationToken.getConfirmationToken());

            emailService.sendEmail(mailMessage);

            model.addAttribute("email", user.getEmail());

            return "redirect:/login/?success";
        }
    }

    @RequestMapping(value = "/confirm-account", method = {RequestMethod.GET, RequestMethod.POST})
    public String confirmUserAccount(Model model, @RequestParam("token") String confirmationToken) {
        ConfirmationToken token = tokenDao.findByConfirmationToken(confirmationToken);

        if (token != null) {
            User user = userDao.findByEmailIgnoreCase(token.getUser().getEmail());
            user.setVerified(true);
            userDao.save(user);
        } else {
            model.addAttribute("message", "The link is invalid or broken!");
        }
        return "home";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam(name = "currentPassword") String currentPassword,
                                 @RequestParam(name = "newPassword") String newPassword,
                                 @RequestParam(name = "confirmPassword") String confirmPassword,
                                 RedirectAttributes redir) {

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userDao.getOne(user.getId());

        //if the current password is entered correctly
        if (passwordEncoder.matches(currentPassword, user.getPassword())) {

            //if the new and confirm password fields match
            if (newPassword.equals(confirmPassword)) {
                currentUser.setPassword(passwordEncoder.encode(newPassword));
                userDao.save(currentUser);
                return "redirect:/logout-change";
            } else {
                //does not match and can't change password
                redir.addFlashAttribute("errorMsg", "Passwords don't match");
                return "redirect:/messages";
            }
        } else {
            //current password is incorrect and can't change password
            redir.addFlashAttribute("errorMsg", "Incorrect password");
            return "redirect:/messages";
        }
    }

    @GetMapping("/logout-change")
    public String logoutUser(HttpServletRequest request, HttpServletResponse response,
                             RedirectAttributes redir) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            redir.addFlashAttribute("logoutMsg", "Password successfully changed");
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/login?logout";
    }

    //shows the password recovery page
    @GetMapping("/password-recovery")
    public String recoverPasswordPage() {

        if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof User) {
            return "redirect:/";
        }

        return "users/recover-password";
    }

    //shows the password reset page
    @GetMapping("/reset")
    public String resetPassword(@RequestParam("token") String confirmationToken, Model model) {

        User user = userDao.findByPassword(confirmationToken);

        if (user == null) {
            model.addAttribute("msg", "Invalid token");
            model.addAttribute("token", null);
        } else {
            model.addAttribute("token", confirmationToken);
        }

        return "users/password-reset";
    }


    @PostMapping("/reset")
    public String updatePassword(@RequestParam("token") String confirmationToken,
                                 @RequestParam("resetNew") String resetNew,
                                 @RequestParam("resetConfirm") String resetConfirm,
                                 RedirectAttributes redir) {

        //if the new and confirm passwords DO NOT match
        if (!resetNew.equals(resetConfirm)) {

            redir.addFlashAttribute("msg", "Passwords do not match.");

            return "redirect:/reset?token=" + confirmationToken;
        }

        //else continue looking for the correct user
        User user = userDao.findByPassword(confirmationToken);

        //if the user doesn't exist; let user know of failure
        if (user == null) {
            redir.addFlashAttribute("msg", "Invalid token; try recovery process again");
            return "redirect:/users/password-recovery";
        }

        //otherwise, change the user's password the the new one
        user.setPassword(passwordEncoder.encode(resetNew));
        userDao.save(user);

        return "redirect:/login";
    }

    @PostMapping("/password-recovery")
    public String recoverPassword(@RequestParam(name = "email") String email,
                                  RedirectAttributes redir) {

        if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof User) {
            return "redirect:/";
        }

        //if the user email doesn't exist
        if (userDao.findByEmailIgnoreCase(email) == null) {
            redir.addFlashAttribute("msg", "There are no accounts associated with " + email);
        } else {

            User user = userDao.findByEmailIgnoreCase(email);

            ConfirmationToken confirmationToken = new ConfirmationToken(user);
            user.setPassword(confirmationToken.getConfirmationToken());
            userDao.save(user);

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(user.getEmail());
            mailMessage.setSubject("[squarecubed.xyz] Reset your password");
            mailMessage.setFrom("no-reply@squarecubed.xyz");
            mailMessage.setText("Username: " + user.getUsername() + "\nTo finish resetting your password, please click here : "
                    + "http://squarecubed.xyz/reset?token=" + confirmationToken.getConfirmationToken());

            emailService.sendEmail(mailMessage);

            redir.addFlashAttribute("msg", "An email has been sent to " + user.getEmail() +
                    " with further instructions to reset your password.");

        }
        return "redirect:/password-recovery";
    }
}