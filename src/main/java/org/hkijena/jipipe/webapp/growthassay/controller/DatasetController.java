package org.hkijena.jipipe.webapp.growthassay.controller;

import org.hkijena.jipipe.webapp.growthassay.config.RuntimeConfig;
import org.hkijena.jipipe.webapp.growthassay.model.Dataset;
import org.hkijena.jipipe.webapp.growthassay.repositories.DatasetRepository;
import org.hkijena.jipipe.webapp.growthassay.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Controller
public class DatasetController {

    private final RuntimeConfig runtimeConfig;
    private final DatasetRepository datasetRepository;

    @Autowired
    public DatasetController(RuntimeConfig runtimeConfig, DatasetRepository datasetRepository) {
        this.runtimeConfig = runtimeConfig;
        this.datasetRepository = datasetRepository;
    }

    @GetMapping("/new")
    public ModelAndView newDataset(Model model) throws IOException {
        Dataset dataset = new Dataset();
        Path storageDir;
        if(StringUtils.isNullOrEmpty(runtimeConfig.getCustomTempDirectory())) {
            storageDir = Files.createTempDirectory("jip-webapp");
        }
        else {
            storageDir = Files.createTempDirectory(Paths.get(runtimeConfig.getCustomTempDirectory()), "jip-webapp");
        }
        dataset.setStoragePath(storageDir.toString());
        dataset = datasetRepository.save(dataset);
        return new ModelAndView("redirect:/dataset/" + dataset.getId());
    }

    @GetMapping("/dataset/{id}")
    public ModelAndView viewDataset(Model model, @PathVariable long id) {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent()) {
            Dataset dataset = dataset_.get();

            datasetRepository.putSortedToModel(model);
            model.addAttribute("currentDatasetId", dataset.getId());

            return new ModelAndView("dataset-prepare");
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/delete/{id}")
    public ModelAndView deleteDataset(Model model, @PathVariable long id) {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent()) {

            Dataset dataset = dataset_.get();

            datasetRepository.putSortedToModel(model);
            model.addAttribute("currentDatasetId", -1);
            model.addAttribute("messageText", "Dataset '" + dataset.getName() + "' was deleted.");

            return new ModelAndView("message");
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
