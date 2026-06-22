package com.buldreinfo.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buldreinfo.dao.MediaRepository;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.infrastructure.OpenApiConstants;
import com.buldreinfo.io.StorageManager;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.xml.VegvesenParser;
import com.buldreinfo.xml.Webcam;

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

    public WebcamsController(StorageManager storage, ClimbingTransactionManager txManager, MediaRepository mediaRepo, RegionRepository regionRepo, UserRepository userRepo) {
        super(storage, txManager, mediaRepo, regionRepo, userRepo);
    }

    @Operation(summary = "Get webcams", responses = {
            @ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = Webcam.class)))}),
            @ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Webcam>> getCameras() throws Exception {
        VegvesenParser vegvesenParser = new VegvesenParser();
        return ResponseEntity.ok(vegvesenParser.getCameras());
    }
}