package org.hkijena.jipipe.webapp.growthassay.services;

import org.hkijena.jipipe.webapp.growthassay.config.AccountConfig;
import org.hkijena.jipipe.webapp.growthassay.model.*;
import org.hkijena.jipipe.webapp.growthassay.repositories.DatasetRepository;
import org.hkijena.jipipe.webapp.growthassay.repositories.InputDataRepository;
import org.hkijena.jipipe.webapp.growthassay.utils.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class DatasetService {
    private final DatasetRepository datasetRepository;
    private final InputDataRepository inputDataRepository;
    private final AccountConfig accountConfig;

    public DatasetService(DatasetRepository datasetRepository, InputDataRepository inputDataRepository, AccountConfig accountConfig) {
        this.datasetRepository = datasetRepository;
        this.inputDataRepository = inputDataRepository;
        this.accountConfig = accountConfig;
    }

    public boolean canCreateProject(Authentication authentication) {
        if(authentication != null) {
            if(authentication.getAuthorities().contains(Privileges.PRIVILEGE_CREATE_TASKS)) {
                if(authentication.getPrincipal() instanceof UserPrincipal) {
                    User user = ((UserPrincipal) authentication.getPrincipal()).getUser();
                    if(user.getRole() == User.Role.Guest) {
                        return datasetRepository.findByOwner(user).size() < accountConfig.getGuestDatasetLimit();
                    }
                    else {
                        return true;
                    }
                }
                else {
                    return true;
                }
            }
        }
        return false;
    }
    public void delete(Dataset dataset) {
        // Delete all input files
        for (InputData data : dataset.getInputData()) {

            // Main data
            if(!StringUtils.isNullOrEmpty(data.getStoragePath())) {
                Path path = Paths.get(data.getStoragePath());
                if (Files.isRegularFile(path)) {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Thumbnail
            if(!StringUtils.isNullOrEmpty(data.getThumbnailStoragePath())) {
                Path path = Paths.get(data.getThumbnailStoragePath());
                if (Files.isRegularFile(path)) {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // Delete from database
        datasetRepository.delete(dataset);
    }

    public boolean canUploadInput(Dataset dataset, Authentication authentication) {
        if(authentication != null) {
            if(authentication.getAuthorities().contains(Privileges.PRIVILEGE_CREATE_TASKS)) {
                if(authentication.getPrincipal() instanceof UserPrincipal) {
                    User user = ((UserPrincipal) authentication.getPrincipal()).getUser();
                    if(user.getRole() == User.Role.Guest) {
                        return inputDataRepository.findByDataset(dataset).size() < accountConfig.getGuestInputDataLimit();
                    }
                    else {
                        return true;
                    }
                }
                else {
                    return true;
                }
            }
        }
        return false;
    }
}
