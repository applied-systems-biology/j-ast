package org.hkijena.jipipe.webapp.growthassay.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.hkijena.jipipe.webapp.growthassay.config.RuntimeConfig;
import org.hkijena.jipipe.webapp.growthassay.model.Dataset;
import org.hkijena.jipipe.webapp.growthassay.model.OutputData;
import org.hkijena.jipipe.webapp.growthassay.repositories.DatasetRepository;
import org.hkijena.jipipe.webapp.growthassay.repositories.OutputDataRepository;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Controller
public class OutputDataController {
    private final RuntimeConfig runtimeConfig;
    private final DatasetRepository datasetRepository;
    private final OutputDataRepository outputDataRepository;

    @Autowired
    public OutputDataController(RuntimeConfig runtimeConfig, DatasetRepository datasetRepository, OutputDataRepository outputDataRepository) {
        this.runtimeConfig = runtimeConfig;
        this.datasetRepository = datasetRepository;
        this.outputDataRepository = outputDataRepository;
    }

    @GetMapping("/output-data/visualization/thumbnail/{id}")
    public ModelAndView getThumbnail(HttpServletResponse response, @PathVariable long id) throws IOException {
        Optional<OutputData> outputData_ = outputDataRepository.findById(id);
        if(outputData_.isPresent()) {
            OutputData outputData = outputData_.get();
            RequestUtils.sendContent(response, Paths.get(outputData.getVisualizationThumbnailStoragePath()));
            return null;
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/output-data/visualization/view/{id}")
    public ModelAndView view(HttpServletResponse response, @PathVariable long id) throws IOException {
        Optional<OutputData> outputData_ = outputDataRepository.findById(id);
        if(outputData_.isPresent()) {
            OutputData outputData = outputData_.get();
            RequestUtils.sendContent(response, Paths.get(outputData.getVisualizationStoragePath()));
            return null;
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/output-data/visualization/download/{id}")
    public ModelAndView download(HttpServletResponse response, @PathVariable long id) throws IOException {
        Optional<OutputData> outputData_ = outputDataRepository.findById(id);
        if(outputData_.isPresent()) {
            OutputData outputData = outputData_.get();
            RequestUtils.sendAttachment(response, Paths.get(outputData.getVisualizationStoragePath()), Paths.get(outputData.getVisualizationStoragePath()).getFileName().toString());
            return null;
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
