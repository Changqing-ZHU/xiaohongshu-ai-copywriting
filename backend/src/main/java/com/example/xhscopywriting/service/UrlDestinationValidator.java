package com.example.xhscopywriting.service;

import java.net.URI;

@FunctionalInterface
public interface UrlDestinationValidator {

    void validate(URI uri);
}
