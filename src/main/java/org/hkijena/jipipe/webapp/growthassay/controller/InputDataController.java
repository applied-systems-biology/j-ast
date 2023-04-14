package org.hkijena.jipipe.webapp.growthassay.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.hkijena.jipipe.webapp.growthassay.config.RuntimeConfig;
import org.hkijena.jipipe.webapp.growthassay.model.Dataset;
import org.hkijena.jipipe.webapp.growthassay.model.InputData;
import org.hkijena.jipipe.webapp.growthassay.repositories.DatasetRepository;
import org.hkijena.jipipe.webapp.growthassay.repositories.InputDataRepository;
import org.hkijena.jipipe.webapp.growthassay.utils.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
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
public class InputDataController {
    private final RuntimeConfig runtimeConfig;
    private final DatasetRepository datasetRepository;
    private final InputDataRepository inputDataRepository;

    @Autowired
    public InputDataController(RuntimeConfig runtimeConfig, DatasetRepository datasetRepository, InputDataRepository inputDataRepository) {
        this.runtimeConfig = runtimeConfig;
        this.datasetRepository = datasetRepository;
        this.inputDataRepository = inputDataRepository;
    }

    @GetMapping("/input-data/thumbnail/{id}")
    public ModelAndView getThumbnail(HttpServletResponse response, @PathVariable long id) throws IOException {
        Optional<InputData> inputData_ = inputDataRepository.findById(id);
        if(inputData_.isPresent()) {
            InputData inputData = inputData_.get();
            RequestUtils.sendContent(response, Paths.get(inputData.getThumbnailStoragePath()));
            return null;
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/input-data/view/{id}")
    public ModelAndView view(HttpServletResponse response, @PathVariable long id) throws IOException {
        Optional<InputData> inputData_ = inputDataRepository.findById(id);
        if(inputData_.isPresent()) {
            InputData inputData = inputData_.get();
            RequestUtils.sendContent(response, Paths.get(inputData.getStoragePath()));
            return null;
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/input-data/download/{id}")
    public ModelAndView download(HttpServletResponse response, @PathVariable long id) throws IOException {
        Optional<InputData> inputData_ = inputDataRepository.findById(id);
        if(inputData_.isPresent()) {
            InputData inputData = inputData_.get();
            RequestUtils.sendAttachment(response, Paths.get(inputData.getStoragePath()), inputData.getOriginalFileName());
            return null;
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/input-data/delete/{id}")
    public void deleteInputData(HttpServletResponse response, @PathVariable long id) {
        Optional<InputData> inputData_ = inputDataRepository.findById(id);
        if(inputData_.isPresent()) {
            InputData inputData = inputData_.get();
            Dataset dataset = inputData.getDataset();
            dataset.removeInputData(inputData);
            datasetRepository.save(dataset);
            inputDataRepository.delete(inputData);
            inputData.deleteStorage(Path.of(dataset.getStoragePath()));
            response.setStatus(HttpStatus.OK.value());
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
