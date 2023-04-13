package org.hkijena.jipipe.webapp.growthassay.controller;

import org.hkijena.jipipe.webapp.growthassay.model.Notification;
import org.hkijena.jipipe.webapp.growthassay.repositories.DatasetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class IndexController {

    private final DatasetRepository datasetRepository;

    @Autowired
    public IndexController(DatasetRepository datasetRepository) {
        this.datasetRepository = datasetRepository;
    }

    @GetMapping("/")
    public ModelAndView index(Model model) {
        datasetRepository.putSortedToModel(model);
        model.addAttribute("currentDatasetId", -1);
        return new ModelAndView("index");
    }
}
