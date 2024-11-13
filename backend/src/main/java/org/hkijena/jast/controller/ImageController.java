package org.hkijena.jast.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.payloads.ImagePayload;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.services.ProjectService;
import org.hkijena.jast.services.UserService;
import org.hkijena.jast.utils.MimeTypeUtils;
import org.hkijena.jast.utils.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public class ImageController {
    private final ProjectRepository projectRepository;
    private final ImageRepository imageRepository;
    private final ProjectService projectService;
    private final UserService userService;

    @Autowired
    public ImageController(ProjectRepository projectRepository, ImageRepository imageRepository, ProjectService projectService, UserService userService) {
        this.projectRepository = projectRepository;
        this.imageRepository = imageRepository;
        this.projectService = projectService;
        this.userService = userService;
    }

    @GetMapping("/api/project/{id}/list-images")
    public ResponseEntity<List<ImagePayload>> listImages(Authentication authentication, @PathVariable long id) {
        userService.validateAuthentication(authentication);
        Project project = projectService.getProjectByIdOrError(id);
        if (!project.canAccess(authentication)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        List<ImagePayload> result = new ArrayList<>();
        for (Image image : project.getImages()) {
            result.add(ImagePayload.create(image));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/image/{id}/thumbnail")
    public void getThumbnail(HttpServletResponse response, Authentication authentication, @PathVariable long id) throws IOException {
        userService.validateAuthentication(authentication);
        Optional<Image> inputData_ = imageRepository.findById(id);
        if (inputData_.isPresent()) {
            Image image = inputData_.get();

            if (!image.getProject().canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            RequestUtils.sendContent(response, image.getThumbnailData(), MimeTypeUtils.MIME_TYPE_PNG);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/api/image/{id}/delete")
    public void deleteImage(Authentication authentication, @PathVariable long id) {
        userService.validateAuthentication(authentication);
        Optional<Image> inputData_ = imageRepository.findById(id);
        if (inputData_.isPresent()) {
            Image image = inputData_.get();

            if (!image.getProject().canEdit(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            imageRepository.delete(image);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

//
//    @GetMapping("/image/view/{id}")
//    public ModelAndView view(HttpServletResponse response, Authentication authentication, @PathVariable long id) throws IOException {
//        Optional<Image> inputData_ = imageRepository.findById(id);
//        if(inputData_.isPresent()) {
//            Image image = inputData_.get();
//
//            if(!image.getProject().canAccess(authentication)) {
//                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
//            }
//
//            RequestUtils.sendContent(response, image.getRawData(), MimeTypeUtils.MIME_TYPE_PNG);
//            return null;
//        }
//        else {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
//        }
//    }
//
//    @GetMapping("/image/download/{id}")
//    public ModelAndView download(HttpServletResponse response, Authentication authentication, @PathVariable long id) throws IOException {
//        Optional<Image> inputData_ = imageRepository.findById(id);
//        if(inputData_.isPresent()) {
//            Image image = inputData_.get();
//
//            if(!image.getProject().canAccess(authentication)) {
//                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
//            }
//
//            RequestUtils.sendAttachment(response, image.getRawData(), image.getOriginalFileName(), MimeTypeUtils.MIME_TYPE_PNG);
//            return null;
//        }
//        else {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
//        }
//    }
}
