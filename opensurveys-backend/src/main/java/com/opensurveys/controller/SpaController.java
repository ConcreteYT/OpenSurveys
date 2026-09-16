package com.opensurveys.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// Makes React Router's client-side routes work on a hard page load/refresh once the built
// frontend is served from this app's static resources (see App.js's windowOptions for the
// full route list). Without this, e.g. a browser refresh on /home would 404: there's no
// static home.html file, only index.html, which the SPA's own router then reads the URL
// from client-side.
//
// Registered here as @GetMapping methods (not a WebMvcConfigurer/ViewControllerRegistry
// entry) so they're resolved by the same RequestMappingHandlerMapping as AuthController's
// POST /auth/login and /auth/signup. That lets Spring MVC disambiguate GET vs POST on the
// exact same path correctly - a ViewControllerRegistry entry lives in a separate, lower-
// precedence HandlerMapping, and RequestMappingHandlerMapping throws
// HttpRequestMethodNotSupportedException as soon as it finds a path match with the wrong
// method, before that other HandlerMapping ever gets a chance to serve the GET request.
@Controller
public class SpaController {

    @GetMapping({"/home", "/auth/login", "/auth/signup", "/user-view", "/login-home", "/surveys",
            "/admin", "/editor", "/editor/{id}", "/settings"})
    public String spaRoutes() {
        return "forward:/index.html";
    }
}
