package dev.ratmir.cloudstorage.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	public static final String SESSION_COOKIE = "sessionCookie";

	@Bean
	OpenAPI cloudStorageOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Cloud Data Base API")
						.version("1.0")
						.description("Session-authenticated API for a multi-user file cloud."))
				.components(new Components().addSecuritySchemes(
						SESSION_COOKIE,
						new SecurityScheme()
								.type(SecurityScheme.Type.APIKEY)
								.in(SecurityScheme.In.COOKIE)
								.name("SESSION")));
	}
}
