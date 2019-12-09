package com.Nusstudios.Nuss.Exceptions;

import org.json.JSONObject;

public class ConfigurationLoadException extends Exception {
    public JSONObject extendedDescription;
    public ConfigurationLoadException(String message) {
        super(message);
    }
}
