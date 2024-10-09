package org.hkijena.jast.controller;

import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.model.AdminPrincipal;
import org.hkijena.jast.model.Notification;
import org.hkijena.jast.model.User;
import org.hkijena.jast.model.UserPrincipal;
import org.hkijena.jipipe.webapp.growthassay.model.*;
import org.hkijena.jast.model.messages.CreateUpdateUserMessage;
import org.hkijena.jast.repositories.DatasetRepository;
import org.hkijena.jast.repositories.UserRepository;
import org.hkijena.jast.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

@Controller
public class AccountController {

    private final DatasetRepository datasetRepository;
    private final UserRepository userRepository;
    private final AccountConfig accountConfig;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AccountController(DatasetRepository datasetRepository, UserRepository userRepository, AccountConfig accountConfig, PasswordEncoder passwordEncoder) {
        this.datasetRepository = datasetRepository;
        this.userRepository = userRepository;
        this.accountConfig = accountConfig;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/account")
    public ModelAndView getAccountIndex(Model model, Authentication authentication) {
        if(authentication != null && authentication.isAuthenticated()) {
            datasetRepository.putSortedToModel(model, authentication);
            User user;
            if(authentication.getPrincipal() instanceof UserPrincipal) {
                user = ((UserPrincipal) authentication.getPrincipal()).getUser();
            }
            else if(authentication.getPrincipal() instanceof AdminPrincipal) {
                user = new User();
                user.setEmail(accountConfig.getAdminUsername());
                user.setRole(User.Role.Admin);
            }
            else {
                throw new IllegalArgumentException("Unsupported principal type!");
            }
            model.addAttribute("currentUser", user);

            return new ModelAndView("account");
        }
        else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("/account/edit")
    public ModelAndView updateAccount(Authentication authentication, RedirectAttributes redirectAttributes, @ModelAttribute CreateUpdateUserMessage createUpdateUserMessage) {
        if(authentication != null && authentication.isAuthenticated()) {
            User user;
            if(authentication.getPrincipal() instanceof UserPrincipal) {
                user = ((UserPrincipal) authentication.getPrincipal()).getUser();
            }
            else if(authentication.getPrincipal() instanceof AdminPrincipal) {
                Notification.pushToRedirect("Unable to update account!",
                        "This admin account can only be changed by editing the web application settings file.",
                        Notification.Style.danger,
                        redirectAttributes);
                return new ModelAndView("redirect:/account");
            }
            else {
                throw new IllegalArgumentException("Unsupported principal type!");
            }

            if(!StringUtils.isNullOrEmpty(createUpdateUserMessage.getNewPassword())) {
                if(!Objects.equals(createUpdateUserMessage.getNewPassword(), createUpdateUserMessage.getNewPasswordConfirm())) {
                    Notification.pushToRedirect("Unable to update password!",
                            "Please ensure that you correctly repeat the password.",
                            Notification.Style.danger,
                            redirectAttributes);
                    return new ModelAndView("redirect:/account");
                }
                user.setPassword(passwordEncoder.encode(createUpdateUserMessage.getNewPassword()));
            }

            user.setFirstName(StringUtils.nullToEmpty(createUpdateUserMessage.getFirstName()));
            user.setLastName(StringUtils.nullToEmpty(createUpdateUserMessage.getLastName()));
            userRepository.save(user);

            return new ModelAndView("redirect:/account");
        }
        else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
