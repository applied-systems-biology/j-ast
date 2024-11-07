package org.hkijena.jast.controller;

import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.repositories.UserRepository;
import org.hkijena.jast.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;

@Controller
public class AdminController {

    private final AccountConfig accountConfig;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    @Autowired
    public AdminController(AccountConfig accountConfig, UserRepository userRepository, ProjectRepository projectRepository, PasswordEncoder passwordEncoder, UserService userService) {
        this.accountConfig = accountConfig;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

//    @GetMapping("/admin")
//    public ModelAndView getAdminPage(Model model, Authentication authentication) {
//        if (authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Privileges.PRIVILEGE_ADMIN)) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
//        }
//
//        model.addAttribute("accountConfig", accountConfig);
//        projectRepository.putSortedToModel(model, authentication);
//        userRepository.putSortedToModel(model, authentication);
//
//        return new ModelAndView("admin");
//    }
//
//    @GetMapping("/admin/list-projects")
//    public ResponseEntity<List<ProjectAdminStatusMessage>> getDatasetList(Authentication authentication) {
//        if (authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Privileges.PRIVILEGE_ADMIN)) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
//        }
//
//        List<ProjectAdminStatusMessage> result = new ArrayList<>();
//        for (Project project : projectRepository.findAll()) {
//            ProjectAdminStatusMessage message = new ProjectAdminStatusMessage();
//            message.setId(project.getId());
//            message.setOwner(project.getOwner() != null ? project.getOwner().getEmail() : accountConfig.getAdminUsername());
//            message.setName(project.getName());
//            result.add(message);
//        }
//
//        result.sort(Comparator.comparing(ProjectAdminStatusMessage::getOwner).thenComparing(ProjectAdminStatusMessage::getName));
//
//        return ResponseEntity.ok(result);
//    }
//
//    @GetMapping("/admin/cancel-all-runs")
//    public ModelAndView cancelAllRuns(RedirectAttributes redirectAttributes, Authentication authentication) {
//
//        if (authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Privileges.PRIVILEGE_ADMIN)) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
//        }
//
//        for (Project project : projectRepository.findAll()) {
//            if(!project.canEdit(authentication)) {
//                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
//            }
//            // TODO: missing implementation
//        }
//
//        Notification.pushToRedirect("All running analyses cancelled", "All running analyses were cancelled.", Notification.Style.info, redirectAttributes);
//        return new ModelAndView("redirect:/admin");
//
//    }
//
//    @GetMapping("/admin/add-user")
//    public ModelAndView showCreateUserForm(Model model, Authentication authentication) {
//        if (authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Privileges.PRIVILEGE_CREATE_ACCOUNT)) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
//        }
//
//        projectRepository.putSortedToModel(model, authentication);
//        return new ModelAndView("admin-user-add");
//    }
//
//    @PostMapping("/admin/add-user")
//    public ModelAndView createUser(Authentication authentication, RedirectAttributes redirectAttributes, @ModelAttribute CreateUpdateUserMessage createUpdateUserMessage) {
//        if (authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Privileges.PRIVILEGE_CREATE_ACCOUNT)) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
//        }
//
//        String userName = createUpdateUserMessage.getEmail().trim().toLowerCase();
//        if (StringUtils.isNullOrEmpty(userName)) {
//            Notification.pushToRedirect("E-Mail is empty!", "The provided E-Mail is empty'!", Notification.Style.danger, redirectAttributes);
//            return new ModelAndView("redirect:/admin");
//        }
//        if (accountConfig.getAdminUsername().equalsIgnoreCase(userName) || userRepository.existsByEmailIgnoreCase(userName)) {
//            Notification.pushToRedirect("User already exists!", "There is already a user with the E-Mail-Address '" + userName + "'!", Notification.Style.danger, redirectAttributes);
//            return new ModelAndView("redirect:/admin");
//        }
//        if (StringUtils.isNullOrEmpty(createUpdateUserMessage.getNewPassword())) {
//            Notification.pushToRedirect("Empty password!", "The provided password was empty!", Notification.Style.danger, redirectAttributes);
//            return new ModelAndView("redirect:/admin");
//        }
//        if (!Objects.equals(createUpdateUserMessage.getNewPassword(), createUpdateUserMessage.getNewPasswordConfirm())) {
//            Notification.pushToRedirect("Passwords are not equal!", "Please confirm the password via the dedicated field.", Notification.Style.danger, redirectAttributes);
//            return new ModelAndView("redirect:/admin");
//        }
//
//        User user = new User();
//        user.setEmail(userName);
//        user.setRole(createUpdateUserMessage.getRole());
//        user.setPassword(passwordEncoder.encode(createUpdateUserMessage.getNewPassword()));
//        user.setFirstName(StringUtils.nullToEmpty(createUpdateUserMessage.getFirstName()));
//        user.setLastName(StringUtils.nullToEmpty(createUpdateUserMessage.getLastName()));
//        userRepository.save(user);
//
//        Notification.pushToRedirect("New user created", "Successfully created new user '" + userName + "' (role " + createUpdateUserMessage.getRole() + ")", Notification.Style.success, redirectAttributes);
//
//        return new ModelAndView("redirect:/admin");
//    }
//
//    @GetMapping("/admin/edit-user/{id}")
//    public ModelAndView showEditUserForm(Model model, Authentication authentication, @PathVariable long id) {
//        if (authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Privileges.PRIVILEGE_EDIT_OTHER_ACCOUNT)) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
//        }
//
//        Optional<User> user_ = userRepository.findById(id);
//        if (user_.isPresent()) {
//            User user = user_.get();
//            CreateUpdateUserMessage message = new CreateUpdateUserMessage(user);
//            model.addAttribute("user", message);
//            projectRepository.putSortedToModel(model, authentication);
//
//            return new ModelAndView("admin-user-edit");
//        } else {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
//        }
//    }
//
//    @PostMapping("/admin/edit-user/{id}")
//    public ModelAndView createUser(Authentication authentication, RedirectAttributes redirectAttributes, @ModelAttribute CreateUpdateUserMessage createUpdateUserMessage, @PathVariable long id) {
//        if (authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Privileges.PRIVILEGE_CREATE_ACCOUNT)) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
//        }
//
//        Optional<User> user_ = userRepository.findById(id);
//        if (user_.isPresent()) {
//            User user = user_.get();
//
//            String userName = createUpdateUserMessage.getEmail().trim().toLowerCase();
//            if (StringUtils.isNullOrEmpty(userName)) {
//                Notification.pushToRedirect("E-Mail is empty!", "The provided E-Mail is empty'!", Notification.Style.danger, redirectAttributes);
//                return new ModelAndView("redirect:/admin");
//            }
//            if (!user.getEmail().equalsIgnoreCase(userName)) {
//                Optional<User> existing = userRepository.findByEmailIgnoreCase(userName);
//                if (existing.isPresent()) {
//                    Notification.pushToRedirect("E-Mail is already registered!", "The provided E-Mail is already assigned to another user'!", Notification.Style.danger, redirectAttributes);
//                    return new ModelAndView("redirect:/admin");
//                }
//            }
//            if (!StringUtils.isNullOrEmpty(createUpdateUserMessage.getNewPassword())) {
//                if (!Objects.equals(createUpdateUserMessage.getNewPassword(), createUpdateUserMessage.getNewPasswordConfirm())) {
//                    Notification.pushToRedirect("Passwords are not equal!", "Please confirm the password via the dedicated field.", Notification.Style.danger, redirectAttributes);
//                    return new ModelAndView("redirect:/admin");
//                }
//            }
//
//            user.setEmail(userName);
//            user.setRole(createUpdateUserMessage.getRole());
//            user.setAllowLogin(createUpdateUserMessage.isAllowLogin());
//            user.setFirstName(createUpdateUserMessage.getFirstName());
//            user.setLastName(createUpdateUserMessage.getLastName());
//            if (!StringUtils.isNullOrEmpty(createUpdateUserMessage.getNewPassword())) {
//                user.setPassword(passwordEncoder.encode(createUpdateUserMessage.getNewPassword()));
//            }
//
//            userRepository.save(user);
//            userService.logoutUser(user);
//            Notification.pushToRedirect("Updated user", "Successfully updated the settings of the user '" + user.getEmail() + "' (role " + user.getRole() + ")", Notification.Style.success, redirectAttributes);
//
//            return new ModelAndView("redirect:/admin");
//        } else {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
//        }
//    }
//
//    @PostMapping("/admin/deactivate-user/{id}")
//    public ModelAndView deactivateUser(Authentication authentication, RedirectAttributes redirectAttributes, @ModelAttribute CreateUpdateUserMessage createUpdateUserMessage, @PathVariable long id) {
//        if (authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Privileges.PRIVILEGE_CREATE_ACCOUNT)) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
//        }
//
//        Optional<User> user_ = userRepository.findById(id);
//        if (user_.isPresent()) {
//            User user = user_.get();
//            userService.deactivateUser(user);
//            Notification.pushToRedirect("Updated user", "Successfully deactivated the user '" + user.getEmail() + "' (role " + user.getRole() + ")", Notification.Style.success, redirectAttributes);
//
//            return new ModelAndView("redirect:/admin");
//        } else {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
//        }
//    }
//
//    @PostMapping("/admin/activate-user/{id}")
//    public ModelAndView activateUser(Authentication authentication, RedirectAttributes redirectAttributes, @ModelAttribute CreateUpdateUserMessage createUpdateUserMessage, @PathVariable long id) {
//        if (authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Privileges.PRIVILEGE_CREATE_ACCOUNT)) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
//        }
//
//        Optional<User> user_ = userRepository.findById(id);
//        if (user_.isPresent()) {
//            User user = user_.get();
//            userService.activateUser(user);
//            Notification.pushToRedirect("Updated user", "Successfully activated the user '" + user.getEmail() + "' (role " + user.getRole() + ")", Notification.Style.success, redirectAttributes);
//
//            return new ModelAndView("redirect:/admin");
//        } else {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
//        }
//    }
//
//    @PostMapping("/admin/delete-user/{id}")
//    public ModelAndView deleteUser(Authentication authentication, RedirectAttributes redirectAttributes, @ModelAttribute CreateUpdateUserMessage createUpdateUserMessage, @PathVariable long id) {
//        if (authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Privileges.PRIVILEGE_DELETE_OTHER_ACCOUNT)) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
//        }
//
//        Optional<User> user_ = userRepository.findById(id);
//        if (user_.isPresent()) {
//            User user = user_.get();
//            userService.deleteUser(user);
//            Notification.pushToRedirect("Deleted user", "Successfully deleted the user '" + user.getEmail() + "' (role " + user.getRole() + ")", Notification.Style.success, redirectAttributes);
//
//            return new ModelAndView("redirect:/admin");
//        } else {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
//        }
//    }
}
