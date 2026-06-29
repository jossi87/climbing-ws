package com.buldreinfo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "buldreinfo")
public record AppConfig(@NotBlank String akamaiAccessKey, @NotBlank String akamaiSecretKey, @NotBlank String apifyApiToken, DatabaseConfig db, @NotBlank String googleApikey, @NotBlank String vegvesenAuth) {
	public record DatabaseConfig(@NotBlank String database, @NotBlank String hostname, @NotBlank String password, @NotBlank String username ) {}
}