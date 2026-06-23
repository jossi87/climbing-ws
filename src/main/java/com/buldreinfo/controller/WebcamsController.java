package com.buldreinfo.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.infrastructure.OpenApiConstants;
import com.buldreinfo.service.VegvesenService;
import com.buldreinfo.service.VegvesenService.Webcam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Webcam")
@RestController
@RequestMapping("/webcams")
public class WebcamsController extends BaseController {
	private final VegvesenService vegvesenService;

    public WebcamsController(ClimbingTransactionManager txManager, RegionRepository regionRepo, VegvesenService vegvesenService) {
        super(txManager, regionRepo);
        this.vegvesenService = vegvesenService;
    }

    @Operation(summary = "Get webcams", responses = {
            @ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = Webcam.class)))}),
            @ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Webcam>> getCameras() throws Exception {
        return ResponseEntity.ok(vegvesenService.getCameras());
    }
}