package com.buldreinfo.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.infrastructure.RequestContext;
import com.buldreinfo.model.Administrator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Admin")
@RestController
@RequestMapping("/administrators")
public class AdministratorsController {
	private final RequestContext requestContext;
	private final UserRepository userRepo;

	public AdministratorsController(RequestContext requestContext, UserRepository userRepo) {
		this.requestContext = requestContext;
		this.userRepo = userRepo;
	}

	@Operation(summary = "Get administrators")
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<Administrator>> getAdministrators(HttpServletRequest request) {
		var setup = requestContext.getSetup(request);
		return ResponseEntity.ok(userRepo.getAdministrators(setup));
	}
}
