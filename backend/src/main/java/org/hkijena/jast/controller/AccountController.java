package org.hkijena.jast.controller;

import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;

@Controller
public class AccountController {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AccountConfig accountConfig;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AccountController(ProjectRepository projectRepository, UserRepository userRepository, AccountConfig accountConfig, PasswordEncoder passwordEncoder) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.accountConfig = accountConfig;
        this.passwordEncoder = passwordEncoder;
    }

//    @GetMapping("/account")
//    public ModelAndView getAccountIndex(Model model, Authentication authentication) {
//        if(authentication != null && authentication.isAuthenticated()) {
//            projectRepository.putSortedToModel(model, authentication);
//            User user;
//            if(authentication.getPrincipal() instanceof UserPrincipal) {
//                user = ((UserPrincipal) authentication.getPrincipal()).getUser();
//            }
//            else if(authentication.getPrincipal() instanceof AdminPrincipal) {
//                user = new User();
//                user.setEmail(accountConfig.getAdminUsername());
//                user.setRole(User.Role.Admin);
//            }
//            else {
//                throw new IllegalArgumentException("Unsupported principal type!");
//            }
//            model.addAttribute("currentUser", user);
//
//            return new ModelAndView("account");
//        }
//        else {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
//        }
//    }
//
//    @PostMapping("/account/edit")
//    public ModelAndView updateAccount(Authentication authentication, RedirectAttributes redirectAttributes, @ModelAttribute CreateUpdateUserMessage createUpdateUserMessage) {
//        if(authentication != null && authentication.isAuthenticated()) {
//            User user;
//            if(authentication.getPrincipal() instanceof UserPrincipal) {
//                user = ((UserPrincipal) authentication.getPrincipal()).getUser();
//            }
//            else if(authentication.getPrincipal() instanceof AdminPrincipal) {
//                Notification.pushToRedirect("Unable to update account!",
//                        "This admin account can only be changed by editing the J-AST settings file.",
//                        Notification.Style.danger,
//                        redirectAttributes);
//                return new ModelAndView("redirect:/account");
//            }
//            else {
//                throw new IllegalArgumentException("Unsupported principal type!");
//            }
//
//            if(!StringUtils.isNullOrEmpty(createUpdateUserMessage.getNewPassword())) {
//                if(!Objects.equals(createUpdateUserMessage.getNewPassword(), createUpdateUserMessage.getNewPasswordConfirm())) {
//                    Notification.pushToRedirect("Unable to update password!",
//                            "Please ensure that you correctly repeat the password.",
//                            Notification.Style.danger,
//                            redirectAttributes);
//                    return new ModelAndView("redirect:/account");
//                }
//                user.setPassword(passwordEncoder.encode(createUpdateUserMessage.getNewPassword()));
//            }
//
//            user.setFirstName(StringUtils.nullToEmpty(createUpdateUserMessage.getFirstName()));
//            user.setLastName(StringUtils.nullToEmpty(createUpdateUserMessage.getLastName()));
//            userRepository.save(user);
//
//            return new ModelAndView("redirect:/account");
//        }
//        else {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
//        }
//    }
}
