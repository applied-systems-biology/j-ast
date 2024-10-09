package org.hkijena.jast.controller;

import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.model.entities.TimeSeries;
import org.hkijena.jast.model.Notification;
import org.hkijena.jast.model.Privileges;
import org.hkijena.jast.model.entities.User;
import org.hkijena.jast.model.messages.CreateUpdateUserMessage;
import org.hkijena.jast.model.messages.DatasetAdminStatusMessage;
import org.hkijena.jast.repositories.TimeSeriesRepository;
import org.hkijena.jast.repositories.UserRepository;
import org.hkijena.jast.services.UserService;
import org.hkijena.jast.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
public class AdminController {

    private final AccountConfig accountConfig;
    private final UserRepository userRepository;
    private final TimeSeriesRepository timeSeriesRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    @Autowired
    public AdminController(AccountConfig accountConfig, UserRepository userRepository, TimeSeriesRepository timeSeriesRepository, PasswordEncoder passwordEncoder, UserService userService) {
        this.accountConfig = accountConfig;
        this.userRepository = userRepository;
        this.timeSeriesRepository = timeSeriesRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    @GetMapping("/admin")
    public ModelAndView getAdminPage(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Privileges.PRIVILEGE_ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        model.addAttribute("accountConfig", accountConfig);
        timeSeriesRepository.putSortedToModel(model, authentication);
        userRepository.putSortedToModel(model, authentication);

        return new ModelAndView("admin");
    }

    @GetMapping("/admin/list-projects")
    public ResponseEntity<List<DatasetAdminStatusMessage>> getDatasetList(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Privileges.PRIVILEGE_ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        List<DatasetAdminStatusMessage> result = new ArrayList<>();
        for (TimeSeries timeSeries : timeSeriesRepository.findAll()) {
            DatasetAdminStatusMessage message = new DatasetAdminStatusMessage();
            message.setId(timeSeries.getId());
            message.setOwner(timeSeries.getOwner() != null ? timeSeries.getOwner().getEmail() : accountConfig.getAdminUsername());
            message.setStatus(timeSeries.getStatus().toString());
            message.setName(timeSeries.getName());
            message.setCanCancel(timeSeries.getStatus() == TimeSeries.Status.Running);
            result.add(message);
        }

        result.sort(Comparator.comparing(DatasetAdminStatusMessage::getOwner).thenComparing(DatasetAdminStatusMessage::getName));

        return ResponseEntity.ok(result);
    }

    @GetMapping("/admin/cancel-all-runs")
    public ModelAndView cancelAllRuns(RedirectAttributes redirectAttributes, Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Privileges.PRIVILEGE_ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        for (TimeSeries timeSeries : timeSeriesRepository.findAll()) {
            if (!timeSeries.canEdit(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            timeSeries.tryCancelCurrentJob();
            timeSeries.setStatus(TimeSeries.Status.Preparing);
            timeSeries.clearOutputData();
            timeSeriesRepository.save(timeSeries);
        }

        Notification.pushToRedirect("All running analyses cancelled", "All running analyses were cancelled.", Notification.Style.info, redirectAttributes);
        return new ModelAndView("redirect:/admin");

    }

    @GetMapping("/admin/add-user")
    public ModelAndView showCreateUserForm(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Privileges.PRIVILEGE_CREATE_ACCOUNT)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        timeSeriesRepository.putSortedToModel(model, authentication);
        return new ModelAndView("admin-user-add");
    }

    @PostMapping("/admin/add-user")
    public ModelAndView createUser(Authentication authentication, RedirectAttributes redirectAttributes, @ModelAttribute CreateUpdateUserMessage createUpdateUserMessage) {
        if (authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Privileges.PRIVILEGE_CREATE_ACCOUNT)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        String userName = createUpdateUserMessage.getEmail().trim().toLowerCase();
        if (StringUtils.isNullOrEmpty(userName)) {
            Notification.pushToRedirect("E-Mail is empty!", "The provided E-Mail is empty'!", Notification.Style.danger, redirectAttributes);
            return new ModelAndView("redirect:/admin");
        }
        if (accountConfig.getAdminUsername().equalsIgnoreCase(userName) || userRepository.existsByEmailIgnoreCase(userName)) {
            Notification.pushToRedirect("User already exists!", "There is already a user with the E-Mail-Address '" + userName + "'!", Notification.Style.danger, redirectAttributes);
            return new ModelAndView("redirect:/admin");
        }
        if (StringUtils.isNullOrEmpty(createUpdateUserMessage.getNewPassword())) {
            Notification.pushToRedirect("Empty password!", "The provided password was empty!", Notification.Style.danger, redirectAttributes);
            return new ModelAndView("redirect:/admin");
        }
        if (!Objects.equals(createUpdateUserMessage.getNewPassword(), createUpdateUserMessage.getNewPasswordConfirm())) {
            Notification.pushToRedirect("Passwords are not equal!", "Please confirm the password via the dedicated field.", Notification.Style.danger, redirectAttributes);
            return new ModelAndView("redirect:/admin");
        }

        User user = new User();
        user.setEmail(userName);
        user.setRole(createUpdateUserMessage.getRole());
        user.setPassword(passwordEncoder.encode(createUpdateUserMessage.getNewPassword()));
        user.setFirstName(StringUtils.nullToEmpty(createUpdateUserMessage.getFirstName()));
        user.setLastName(StringUtils.nullToEmpty(createUpdateUserMessage.getLastName()));
        userRepository.save(user);

        Notification.pushToRedirect("New user created", "Successfully created new user '" + userName + "' (role " + createUpdateUserMessage.getRole() + ")", Notification.Style.success, redirectAttributes);

        return new ModelAndView("redirect:/admin");
    }

    @GetMapping("/admin/edit-user/{id}")
    public ModelAndView showEditUserForm(Model model, Authentication authentication, @PathVariable long id) {
        if (authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Privileges.PRIVILEGE_EDIT_OTHER_ACCOUNT)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Optional<User> user_ = userRepository.findById(id);
        if (user_.isPresent()) {
            User user = user_.get();
            CreateUpdateUserMessage message = new CreateUpdateUserMessage(user);
            model.addAttribute("user", message);
            timeSeriesRepository.putSortedToModel(model, authentication);

            return new ModelAndView("admin-user-edit");
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/admin/edit-user/{id}")
    public ModelAndView createUser(Authentication authentication, RedirectAttributes redirectAttributes, @ModelAttribute CreateUpdateUserMessage createUpdateUserMessage, @PathVariable long id) {
        if (authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Privileges.PRIVILEGE_CREATE_ACCOUNT)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Optional<User> user_ = userRepository.findById(id);
        if (user_.isPresent()) {
            User user = user_.get();

            String userName = createUpdateUserMessage.getEmail().trim().toLowerCase();
            if (StringUtils.isNullOrEmpty(userName)) {
                Notification.pushToRedirect("E-Mail is empty!", "The provided E-Mail is empty'!", Notification.Style.danger, redirectAttributes);
                return new ModelAndView("redirect:/admin");
            }
            if (!user.getEmail().equalsIgnoreCase(userName)) {
                Optional<User> existing = userRepository.findByEmailIgnoreCase(userName);
                if (existing.isPresent()) {
                    Notification.pushToRedirect("E-Mail is already registered!", "The provided E-Mail is already assigned to another user'!", Notification.Style.danger, redirectAttributes);
                    return new ModelAndView("redirect:/admin");
                }
            }
            if (!StringUtils.isNullOrEmpty(createUpdateUserMessage.getNewPassword())) {
                if (!Objects.equals(createUpdateUserMessage.getNewPassword(), createUpdateUserMessage.getNewPasswordConfirm())) {
                    Notification.pushToRedirect("Passwords are not equal!", "Please confirm the password via the dedicated field.", Notification.Style.danger, redirectAttributes);
                    return new ModelAndView("redirect:/admin");
                }
            }

            user.setEmail(userName);
            user.setRole(createUpdateUserMessage.getRole());
            user.setAllowLogin(createUpdateUserMessage.isAllowLogin());
            user.setFirstName(createUpdateUserMessage.getFirstName());
            user.setLastName(createUpdateUserMessage.getLastName());
            if (!StringUtils.isNullOrEmpty(createUpdateUserMessage.getNewPassword())) {
                user.setPassword(passwordEncoder.encode(createUpdateUserMessage.getNewPassword()));
            }

            userRepository.save(user);
            userService.logoutUser(user);
            Notification.pushToRedirect("Updated user", "Successfully updated the settings of the user '" + user.getEmail() + "' (role " + user.getRole() + ")", Notification.Style.success, redirectAttributes);

            return new ModelAndView("redirect:/admin");
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/admin/deactivate-user/{id}")
    public ModelAndView deactivateUser(Authentication authentication, RedirectAttributes redirectAttributes, @ModelAttribute CreateUpdateUserMessage createUpdateUserMessage, @PathVariable long id) {
        if (authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Privileges.PRIVILEGE_CREATE_ACCOUNT)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Optional<User> user_ = userRepository.findById(id);
        if (user_.isPresent()) {
            User user = user_.get();
            userService.deactivateUser(user);
            Notification.pushToRedirect("Updated user", "Successfully deactivated the user '" + user.getEmail() + "' (role " + user.getRole() + ")", Notification.Style.success, redirectAttributes);

            return new ModelAndView("redirect:/admin");
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/admin/activate-user/{id}")
    public ModelAndView activateUser(Authentication authentication, RedirectAttributes redirectAttributes, @ModelAttribute CreateUpdateUserMessage createUpdateUserMessage, @PathVariable long id) {
        if (authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Privileges.PRIVILEGE_CREATE_ACCOUNT)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Optional<User> user_ = userRepository.findById(id);
        if (user_.isPresent()) {
            User user = user_.get();
            userService.activateUser(user);
            Notification.pushToRedirect("Updated user", "Successfully activated the user '" + user.getEmail() + "' (role " + user.getRole() + ")", Notification.Style.success, redirectAttributes);

            return new ModelAndView("redirect:/admin");
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/admin/delete-user/{id}")
    public ModelAndView deleteUser(Authentication authentication, RedirectAttributes redirectAttributes, @ModelAttribute CreateUpdateUserMessage createUpdateUserMessage, @PathVariable long id) {
        if (authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Privileges.PRIVILEGE_DELETE_OTHER_ACCOUNT)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Optional<User> user_ = userRepository.findById(id);
        if (user_.isPresent()) {
            User user = user_.get();
            userService.deleteUser(user);
            Notification.pushToRedirect("Deleted user", "Successfully deleted the user '" + user.getEmail() + "' (role " + user.getRole() + ")", Notification.Style.success, redirectAttributes);

            return new ModelAndView("redirect:/admin");
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
