package org.hkijena.jipipe.webapp.growthassay.controller;

import org.hkijena.jipipe.webapp.growthassay.config.AccountConfig;
import org.hkijena.jipipe.webapp.growthassay.model.Notification;
import org.hkijena.jipipe.webapp.growthassay.repositories.DatasetRepository;
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

    private final DatasetRepository datasetRepository;

    @Autowired
    public IndexController(DatasetRepository datasetRepository) {
        this.datasetRepository = datasetRepository;
    }

    @GetMapping("/")
    public ModelAndView index(Model model, Authentication authentication) {
        datasetRepository.putSortedToModel(model, authentication);
        model.addAttribute("currentDatasetId", -1);
        return new ModelAndView("index");
    }

    @GetMapping("/login")
    public ModelAndView showLoginForm(Model model, Authentication authentication) {
        if(authentication == null || !authentication.isAuthenticated()) {
            datasetRepository.putSortedToModel(model, authentication);
            model.addAttribute("currentDatasetId", -1);
            return new ModelAndView("login");
        }
        else {
            return new ModelAndView("redirect:/");
        }
    }

    @GetMapping("/login-error")
    public ModelAndView loginError(Model model, Authentication authentication) {
        if(authentication == null || !authentication.isAuthenticated()) {
            datasetRepository.putSortedToModel(model, authentication);
            model.addAttribute("currentDatasetId", -1);
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
