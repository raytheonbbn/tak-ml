package com.bbn.takml_server.controller.model_management;

import com.bbn.takml_server.model_management.ui.AddTakmlModelUIRequest;
import com.bbn.takml_server.model_management.ui.EditTakmlModelUIRequest;
import com.bbn.takml_server.model_management.takfs.ModelRepository;
import com.bbn.takml_server.takml_model.ModelTypeConstants;
import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

@Controller
@RequestMapping("/model_management/ui")
@Tag(name = "Model Management", description = "API for managing TAK ML models")
public class ModelUIManagementController {
    private static final Logger logger = LogManager.getLogger(ModelUIManagementController.class);

    private final ModelRepository modelRepository;

    public ModelUIManagementController(ModelRepository modelRepository) {
        this.modelRepository = modelRepository;
    }

    @GetMapping("")
    public String getModelUpload(Model model){
        AddTakmlModelUIRequest takmlModelRequest = new AddTakmlModelUIRequest();
        model.addAttribute("takmlModelRequest", takmlModelRequest);
        model.addAttribute("models", modelRepository.getModelsMetadata());
        model.addAttribute("modelTypes", ModelTypeConstants.getTypes());
        return "model";
    }

    @ResponseBody
    @PostMapping("/add_model")
    public ModelAndView addModel(@ModelAttribute AddTakmlModelUIRequest takmlModelRequest,
                                 RedirectAttributes redirectAttribute) {
        logger.info("Received add model request from: {}", takmlModelRequest.getRequesterCallsign());

        if (takmlModelRequest.getModel() == null) {
            logger.error("Missing Model");
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, "Missing Model");
        }

        byte[] modelWrapper;
        try {
            modelWrapper = takmlModelRequest.getModel().getBytes();
        } catch (IOException e) {
            logger.error("Missing Model bytes");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Model Bytes is Null");
        }

        // Normalize input into a List
        if(takmlModelRequest.getSupportedDevices() != null) {
            takmlModelRequest.setSupportedDevices(
                    takmlModelRequest.getSupportedDevices().stream()
                            .flatMap(devices -> Stream.of(devices.split(",")))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList()
            );
        }

        CompletableFuture<String> future = modelRepository.saveModelWrapper(modelWrapper,
                takmlModelRequest.getRequesterCallsign(), takmlModelRequest.getRunOnTakmlServer(),
                takmlModelRequest.getSupportedDevices());
        CompletableFuture.allOf(future).join();

        if(redirectAttribute != null)
            redirectAttribute.addFlashAttribute("message", "Success");

        ModelAndView modelAndView = new ModelAndView("redirect:/model_management/ui");
        try {
            modelAndView.addObject("hash", future.get());
        } catch (InterruptedException e) {
            throw new ResponseStatusException(HttpStatus.REQUEST_TIMEOUT);
        } catch (ExecutionException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        return modelAndView;
    }

    @PostMapping("/edit_model/{modelHash}")
    public ModelAndView editModel(@PathVariable String modelHash,
                                  @ModelAttribute EditTakmlModelUIRequest takmlModelRequest,
                                       RedirectAttributes redirectAttribute) {
        logger.info("Received add model request from: {}",
                new Gson().toJson(takmlModelRequest.getRequesterCallsign()));

        if (takmlModelRequest.getModel() == null) {
            logger.error("Missing Model");
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, "Missing Model");
        }

        byte[] modelWrapper;
        try {
            modelWrapper = takmlModelRequest.getModel().getBytes();
        } catch (IOException e) {
            logger.error("Missing Model bytes");
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, "Model Bytes is Null");
        }

        // Normalize input into a List
        if(takmlModelRequest.getSupportedDevices() != null) {
            takmlModelRequest.setSupportedDevices(
                    takmlModelRequest.getSupportedDevices().stream()
                            .flatMap(devices -> Stream.of(devices.split(",")))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList()
            );
        }

        CompletableFuture<String> future = modelRepository.editModelWrapper(
                modelHash,
                modelWrapper,
                takmlModelRequest.getRequesterCallsign(),
                takmlModelRequest.getRunOnTakmlServer(),
                takmlModelRequest.getSupportedDevices()
        );

        future.join();

        if(redirectAttribute != null)
            redirectAttribute.addFlashAttribute("message", "Success");

        ModelAndView modelAndView = new ModelAndView("redirect:/model_management/ui");
        try {
            modelAndView.addObject("hash", future.get());
        } catch (InterruptedException e) {
            throw new ResponseStatusException(HttpStatus.REQUEST_TIMEOUT);
        } catch (ExecutionException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        return modelAndView;
    }

    @DeleteMapping("/remove_model/{modelHash}")
    public ModelAndView removeModel(@PathVariable String modelHash) {
        logger.info("Received remove model request: {}", modelHash);
        if (modelHash == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing hash");
        }
        CompletableFuture<String> future = modelRepository.removeModel(modelHash);
        CompletableFuture.allOf(future).join();

        return new ModelAndView("redirect:/model_management/ui", HttpStatus.OK);
    }
}
