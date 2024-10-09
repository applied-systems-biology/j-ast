package org.hkijena.jast.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.hkijena.jast.config.RuntimeConfig;
import org.hkijena.jast.model.entities.TimeSeries;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.repositories.TimeSeriesRepository;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.utils.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
public class ImageController {
    private final RuntimeConfig runtimeConfig;
    private final TimeSeriesRepository timeSeriesRepository;
    private final ImageRepository imageRepository;

    @Autowired
    public ImageController(RuntimeConfig runtimeConfig, TimeSeriesRepository timeSeriesRepository, ImageRepository imageRepository) {
        this.runtimeConfig = runtimeConfig;
        this.timeSeriesRepository = timeSeriesRepository;
        this.imageRepository = imageRepository;
    }

    @GetMapping("/input-data/thumbnail/{id}")
    public ModelAndView getThumbnail(HttpServletResponse response, Authentication authentication, @PathVariable long id) throws IOException {
        Optional<Image> inputData_ = imageRepository.findById(id);
        if(inputData_.isPresent()) {
            Image image = inputData_.get();

            if(!image.getSeries().canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            RequestUtils.sendContent(response, Paths.get(image.getThumbnailStoragePath()));
            return null;
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/input-data/view/{id}")
    public ModelAndView view(HttpServletResponse response, Authentication authentication, @PathVariable long id) throws IOException {
        Optional<Image> inputData_ = imageRepository.findById(id);
        if(inputData_.isPresent()) {
            Image image = inputData_.get();

            if(!image.getSeries().canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            RequestUtils.sendContent(response, Paths.get(image.getStoragePath()));
            return null;
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/input-data/download/{id}")
    public ModelAndView download(HttpServletResponse response, Authentication authentication, @PathVariable long id) throws IOException {
        Optional<Image> inputData_ = imageRepository.findById(id);
        if(inputData_.isPresent()) {
            Image image = inputData_.get();

            if(!image.getSeries().canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            RequestUtils.sendAttachment(response, Paths.get(image.getStoragePath()), image.getOriginalFileName());
            return null;
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/input-data/delete/{id}")
    public void deleteInputData(HttpServletResponse response, Authentication authentication, @PathVariable long id) {
        Optional<Image> inputData_ = imageRepository.findById(id);
        if(inputData_.isPresent()) {
            Image image = inputData_.get();
            TimeSeries timeSeries = image.getSeries();

            if(!image.getSeries().canEdit(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            timeSeries.removeImage(image);
            timeSeriesRepository.save(timeSeries);
            imageRepository.delete(image);
            image.deleteStorage(Path.of(timeSeries.getStoragePath()));
            response.setStatus(HttpStatus.OK.value());
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
