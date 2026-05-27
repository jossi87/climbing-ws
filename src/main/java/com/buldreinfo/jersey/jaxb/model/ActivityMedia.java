package com.buldreinfo.jersey.jaxb.model;

public record ActivityMedia (MediaIdentity identity, boolean movie, boolean is360, String embedUrl) {
}