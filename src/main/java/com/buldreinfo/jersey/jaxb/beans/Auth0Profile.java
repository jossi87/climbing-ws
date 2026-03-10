package com.buldreinfo.jersey.jaxb.beans;

import java.util.Objects;

import com.auth0.jwt.interfaces.DecodedJWT;

public record Auth0Profile(String email, String firstname, String lastname, String picture) {
    public static Auth0Profile from(DecodedJWT jwt) {
        String email = jwt.getClaim("email").asString();
        if (email == null) {
            email = jwt.getClaim("https://buldreinfo.com/email").asString();
        }
        Objects.requireNonNull(email, "JWT is missing a valid email claim");
        String firstname = jwt.getClaim("given_name").asString();
        if (firstname == null) {
            firstname = jwt.getClaim("https://buldreinfo.com/firstname").asString();
        }
        if (firstname == null) {
            firstname = jwt.getClaim("name").asString();
        }
        if (firstname == null) {
            firstname = email;
        }
        String lastname = jwt.getClaim("family_name").asString();
        if (lastname == null) {
            lastname = jwt.getClaim("https://buldreinfo.com/lastname").asString();
        }
        String picture = jwt.getClaim("picture").asString();
        return new Auth0Profile(email, firstname, lastname, picture);
    }
}