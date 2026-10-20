package com.buldreinfo.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buldreinfo.infrastructure.RequestContext;
import com.buldreinfo.model.Webcam;
import com.buldreinfo.service.WebcamService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Webcam")
@RestController
@RequestMapping("/webcams")
public class WebcamsController {
	private final RequestContext requestContext;
	private final WebcamService webcamService;

	public WebcamsController(RequestContext requestContext, WebcamService webcamService) {
		this.requestContext = requestContext;
		this.webcamService = webcamService;
	}

	@Operation(summary = "Get webcams")
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<Webcam>> getCameras(HttpServletRequest request) {
		return ResponseEntity.ok(webcamService.getCameras(requestContext.getSetup(request)));
	}
}
