package org.hkijena.jipipe.growthassayanalyzerwebapp.controller;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.hkijena.jipipe.growthassayanalyzerwebapp.model.Dataset;
import org.hkijena.jipipe.growthassayanalyzerwebapp.repositories.DatasetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.Comparator;

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
