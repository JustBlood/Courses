package ru.just.monolithmvp.service;

import java.net.URI;
import java.net.URISyntaxException;

public class LinkValidator {
        public static boolean isUrl(String value) {
            if (value == null || value.isBlank()) {
                return false;
            }

            try {
                URI uri = new URI(value);

                return ("http".equalsIgnoreCase(uri.getScheme())
                        || "https".equalsIgnoreCase(uri.getScheme()))
                        && uri.getHost() != null;
            } catch (URISyntaxException e) {
                return false;
            }
        }
}
