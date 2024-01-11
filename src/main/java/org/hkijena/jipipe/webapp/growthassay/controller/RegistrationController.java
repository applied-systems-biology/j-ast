package org.hkijena.jipipe.webapp.growthassay.controller;

import org.hkijena.jipipe.webapp.growthassay.config.AccountConfig;
import org.hkijena.jipipe.webapp.growthassay.model.CreateUpdateUserMessage;
import org.hkijena.jipipe.webapp.growthassay.model.Notification;
import org.hkijena.jipipe.webapp.growthassay.model.Privileges;
import org.hkijena.jipipe.webapp.growthassay.model.User;
import org.hkijena.jipipe.webapp.growthassay.repositories.DatasetRepository;
import org.hkijena.jipipe.webapp.growthassay.repositories.UserRepository;
import org.hkijena.jipipe.webapp.growthassay.utils.StringUtils;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Controller
public class RegistrationController {
    private final DatasetRepository datasetRepository;
    private final UserRepository userRepository;
    private final AccountConfig accountConfig;
    private final PasswordEncoder passwordEncoder;

    public RegistrationController(DatasetRepository datasetRepository, UserRepository userRepository, AccountConfig accountConfig, PasswordEncoder passwordEncoder) {
        this.datasetRepository = datasetRepository;
        this.userRepository = userRepository;
        this.accountConfig = accountConfig;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public ModelAndView loginError(Model model, RedirectAttributes redirectAttributes, Authentication authentication) {

        // Add basic info
        datasetRepository.putSortedToModel(model, authentication);
        model.addAttribute("currentDatasetId", -1);

        if(authentication == null || !authentication.isAuthenticated()) {
            if(accountConfig.isAllowGuestAccounts() || accountConfig.isAllowSelfRegister()) {
                model.addAttribute("allowAccountTypeGuest", accountConfig.isAllowGuestAccounts());
                model.addAttribute("allowAccountTypeUser", accountConfig.isAllowSelfRegister());
                return new ModelAndView("register");
            }
            else {
                Notification.pushToRedirect("Registration not supported", "Please contact " + accountConfig.getAdminContact() + " to ask for an account.", Notification.Style.danger, redirectAttributes);
                return new ModelAndView("redirect:/");
            }
        }
        else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("/register")
    public ModelAndView createUser(Authentication authentication, RedirectAttributes redirectAttributes, @ModelAttribute CreateUpdateUserMessage createUpdateUserMessage) {
        if(authentication != null && authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if(createUpdateUserMessage.getRole() == User.Role.Admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if(createUpdateUserMessage.getRole() == User.Role.Guest && !accountConfig.isAllowGuestAccounts()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if(createUpdateUserMessage.getRole() == User.Role.User && !accountConfig.isAllowSelfRegister()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        String userName = createUpdateUserMessage.getEmail().trim().toLowerCase();
        if(StringUtils.isNullOrEmpty(userName)) {
            Notification.pushToRedirect("E-Mail is empty!", "The provided E-Mail is empty'!", Notification.Style.danger, redirectAttributes);
            return new ModelAndView("redirect:/register");
        }
        if(accountConfig.getAdminUsername().equalsIgnoreCase(userName) || userRepository.existsByEmailIgnoreCase(userName)) {
            Notification.pushToRedirect("User already exists!", "There is already a user with the E-Mail-Address '" + userName + "'!", Notification.Style.danger, redirectAttributes);
            return new ModelAndView("redirect:/register");
        }
        if(StringUtils.isNullOrEmpty(createUpdateUserMessage.getNewPassword())) {
            Notification.pushToRedirect("Empty password!", "The provided password was empty!", Notification.Style.danger, redirectAttributes);
            return new ModelAndView("redirect:/register");
        }
        if(!Objects.equals(createUpdateUserMessage.getNewPassword(), createUpdateUserMessage.getNewPasswordConfirm())) {
            Notification.pushToRedirect("Passwords are not equal!", "Please confirm the password via the dedicated field.", Notification.Style.danger, redirectAttributes);
            return new ModelAndView("redirect:/register");
        }

        User user = new User();
        user.setEmail(userName);
        user.setRole(createUpdateUserMessage.getRole());
        user.setPassword(passwordEncoder.encode(createUpdateUserMessage.getNewPassword()));
        user.setFirstName(StringUtils.nullToEmpty(createUpdateUserMessage.getFirstName()));
        user.setLastName(StringUtils.nullToEmpty(createUpdateUserMessage.getLastName()));
        if(user.getRole() == User.Role.Guest) {
            user.setGuestExpire(LocalDateTime.now().plusMinutes(accountConfig.getGuestAccountExpireMinutes()));
        }
        userRepository.save(user);

        Notification.pushToRedirect("Account created", "Please login with the E-mail '" + userName + "' and your password", Notification.Style.success, redirectAttributes);
        if(user.getRole() == User.Role.Guest) {
            Notification.pushToRedirect("Guest account limitations", "Please note that guest accounts have a limit of " + accountConfig.getGuestDatasetLimit() + " data set(s) with at most " + accountConfig.getGuestInputDataLimit() + " images. " +
                    "Guest accounts will be automatically deleted after " + accountConfig.getGuestAccountExpireMinutes() + " minutes from now on." , Notification.Style.danger, redirectAttributes);
        }

        return new ModelAndView("redirect:/login");
    }
}
