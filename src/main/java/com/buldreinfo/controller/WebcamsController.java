package com.buldreinfo.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buldreinfo.service.VegvesenService;
import com.buldreinfo.service.VegvesenService.Webcam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Webcam")
@RestController
@RequestMapping("/webcams")
public class WebcamsController {
	private final VegvesenService vegvesenService;

	public WebcamsController(VegvesenService vegvesenService) {
		this.vegvesenService = vegvesenService;
	}

	@Operation(summary = "Get webcams")
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<Webcam>> getCameras() {
		return ResponseEntity.ok(vegvesenService.getCameras());
	}
}
