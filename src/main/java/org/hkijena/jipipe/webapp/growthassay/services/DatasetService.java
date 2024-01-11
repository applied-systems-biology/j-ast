package org.hkijena.jipipe.webapp.growthassay.services;

import org.hkijena.jipipe.webapp.growthassay.model.Dataset;
import org.hkijena.jipipe.webapp.growthassay.model.InputData;
import org.hkijena.jipipe.webapp.growthassay.repositories.DatasetRepository;
import org.hkijena.jipipe.webapp.growthassay.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class DatasetService {
    private final DatasetRepository datasetRepository;

    public DatasetService(DatasetRepository datasetRepository) {
        this.datasetRepository = datasetRepository;
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
}
