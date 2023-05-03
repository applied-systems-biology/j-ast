package org.hkijena.jipipe.webapp.growthassay.controller;

import org.hkijena.jipipe.webapp.growthassay.config.AccountConfig;
import org.hkijena.jipipe.webapp.growthassay.model.Dataset;
import org.hkijena.jipipe.webapp.growthassay.model.DatasetAdminStatusMessage;
import org.hkijena.jipipe.webapp.growthassay.model.Notification;
import org.hkijena.jipipe.webapp.growthassay.model.Roles;
import org.hkijena.jipipe.webapp.growthassay.repositories.DatasetRepository;
import org.hkijena.jipipe.webapp.growthassay.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Controller
public class AdminController {

    private final AccountConfig accountConfig;
    private final UserRepository userRepository;

    private final DatasetRepository datasetRepository;

    @Autowired
    public AdminController(AccountConfig accountConfig, UserRepository userRepository, DatasetRepository datasetRepository) {
        this.accountConfig = accountConfig;
        this.userRepository = userRepository;
        this.datasetRepository = datasetRepository;
    }

    @GetMapping("/admin")
    public ModelAndView getAdminPage(Model model, Authentication authentication) {
        if(authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Roles.PRIVILEGE_ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        datasetRepository.putSortedToModel(model, authentication);
        return new ModelAndView("admin");
    }

    @GetMapping("/admin/list-datasets")
    public ResponseEntity<List<DatasetAdminStatusMessage>> getDatasetList(Authentication authentication) {
        if(authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Roles.PRIVILEGE_ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        List<DatasetAdminStatusMessage> result = new ArrayList<>();
        for (Dataset dataset : datasetRepository.findAll()) {
            DatasetAdminStatusMessage message = new DatasetAdminStatusMessage();
            message.setId(dataset.getId());
            message.setOwner(dataset.getOwner() != null ? dataset.getOwner().getEmail() :accountConfig.getAdminUserName());
            message.setStatus(dataset.getStatus().toString());
            message.setName(dataset.getName());
            message.setCanCancel(dataset.getStatus() == Dataset.Status.Running);
            result.add(message);
        }

        result.sort(Comparator.comparing(DatasetAdminStatusMessage::getOwner).thenComparing(DatasetAdminStatusMessage::getName));

        return ResponseEntity.ok(result);
    }

    @GetMapping("/admin/cancel-all-runs")
    public ModelAndView cancelAllRuns(RedirectAttributes redirectAttributes, Authentication authentication) {
        for (Dataset dataset : datasetRepository.findAll()) {
            if(!dataset.canEdit(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            dataset.tryCancelCurrentJob();
            dataset.setStatus(Dataset.Status.Preparing);
            dataset.clearOutputData();
            datasetRepository.save(dataset);
        }

        Notification.pushToRedirect("All running analyses cancelled", "All running analyses were cancelled.", Notification.Style.info, redirectAttributes);
        return new ModelAndView("redirect:/admin");

    }
}
