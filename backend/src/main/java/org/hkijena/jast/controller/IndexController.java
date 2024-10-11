package org.hkijena.jast.controller;

import org.hkijena.jast.model.Notification;
import org.hkijena.jast.repositories.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.List;

@Controller
public class IndexController {

    private final ProjectRepository projectRepository;

    @Autowired
    public IndexController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @GetMapping("/")
    public ModelAndView index(Model model, Authentication authentication) {
        projectRepository.putSortedToModel(model, authentication);
        model.addAttribute("currentProjectId", -1);
        return new ModelAndView("index");
    }

    @GetMapping("/login")
    public ModelAndView showLoginForm(Model model, Authentication authentication) {
        if(authentication == null || !authentication.isAuthenticated()) {
            projectRepository.putSortedToModel(model, authentication);
            model.addAttribute("currentProjectId", -1);
            return new ModelAndView("login");
        }
        else {
            return new ModelAndView("redirect:/");
        }
    }

    @GetMapping("/login-error")
    public ModelAndView loginError(Model model, Authentication authentication) {
        if(authentication == null || !authentication.isAuthenticated()) {
            projectRepository.putSortedToModel(model, authentication);
            model.addAttribute("currentProjectId", -1);
            List<Notification> notificationList = new ArrayList<>();
            notificationList.add(new Notification("Login error!", "Could not authenticate! Are the username and the password correct?", "danger"));
            model.addAttribute("notifications", notificationList);
            return new ModelAndView("login");
        }
        else {
            return new ModelAndView("redirect:/");
        }
    }
}
