package com.bbn.takml_server.controller.models.v2;

import com.bbn.tak_sync_file_manager.model.IndexRow;
import com.bbn.takml_server.model_management.takfs.ModelRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/models/v2")
@Tag(value = "API for managing models - built for new React frontend")
public class ModelManagementControllerV2 {
    private static final Logger logger = LoggerFactory.getLogger(ModelManagementControllerV2.class);
    private final ModelRepository modelRepository;

    public ModelManagementControllerV2(ModelRepository modelRepository) {
        this.modelRepository = modelRepository;
    }

    @GetMapping("/get_models")
    @Operation(summary = "Get metadata for all models")
    public ResponseEntity<List<IndexRow>> getModelDescriptors() {
        Set<IndexRow> takServerDataRows = modelRepository.getModelsMetadata();
        if (takServerDataRows == null) {
            takServerDataRows = new HashSet<>();
        }
        return ResponseEntity.ok(takServerDataRows.stream().toList());
    }
}
