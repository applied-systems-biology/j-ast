package org.hkijena.jast.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.utils.MimeTypeUtils;
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
import java.nio.file.Paths;
import java.util.Optional;

@Controller
public class ImageController {
    private final ProjectRepository projectRepository;
    private final ImageRepository imageRepository;

    @Autowired
    public ImageController(ProjectRepository projectRepository, ImageRepository imageRepository) {
        this.projectRepository = projectRepository;
        this.imageRepository = imageRepository;
    }

    @GetMapping("/image/thumbnail/{id}")
    public ModelAndView getThumbnail(HttpServletResponse response, Authentication authentication, @PathVariable long id) throws IOException {
        Optional<Image> inputData_ = imageRepository.findById(id);
        if(inputData_.isPresent()) {
            Image image = inputData_.get();

            if(!image.getProject().canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            RequestUtils.sendContent(response, image.getThumbnailData(), MimeTypeUtils.MIME_TYPE_PNG);
            return null;
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/image/view/{id}")
    public ModelAndView view(HttpServletResponse response, Authentication authentication, @PathVariable long id) throws IOException {
        Optional<Image> inputData_ = imageRepository.findById(id);
        if(inputData_.isPresent()) {
            Image image = inputData_.get();

            if(!image.getProject().canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            RequestUtils.sendContent(response, image.getRawData(), MimeTypeUtils.MIME_TYPE_PNG);
            return null;
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/image/download/{id}")
    public ModelAndView download(HttpServletResponse response, Authentication authentication, @PathVariable long id) throws IOException {
        Optional<Image> inputData_ = imageRepository.findById(id);
        if(inputData_.isPresent()) {
            Image image = inputData_.get();

            if(!image.getProject().canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            RequestUtils.sendAttachment(response, image.getRawData(), image.getOriginalFileName(), MimeTypeUtils.MIME_TYPE_PNG);
            return null;
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/image/delete/{id}")
    public void deleteImage(HttpServletResponse response, Authentication authentication, @PathVariable long id) {
        Optional<Image> inputData_ = imageRepository.findById(id);
        if(inputData_.isPresent()) {
            Image image = inputData_.get();
            Project project = image.getProject();

            if(!project.canEdit(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            project.removeImage(image);
            projectRepository.save(project);
            imageRepository.delete(image);
            response.setStatus(HttpStatus.OK.value());
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
