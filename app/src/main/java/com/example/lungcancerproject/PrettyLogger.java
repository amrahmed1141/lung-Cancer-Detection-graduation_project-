package com.example.lungcancerproject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import okhttp3.internal.platform.Platform;
import okhttp3.logging.HttpLoggingInterceptor;

public
class PrettyLogger implements HttpLoggingInterceptor.Logger {
    private Gson mGson = new GsonBuilder().setPrettyPrinting().create();
    private JsonParser mJsonParser = new JsonParser();

    @Override
    public void log(String message) {
        String trimMessage = message.trim();
        if ((trimMessage.startsWith("{") && trimMessage.endsWith("}"))
                || (trimMessage.startsWith("[") && trimMessage.endsWith("]"))) {
            try {
                String prettyJson = mGson.toJson(mJsonParser.parse(message));
                Platform.get().log(1, prettyJson, null);
            } catch (Exception e) {
                Platform.get().log(2, message, e);
            }
        } else {
            Platform.get().log(3, message, null);
        }
    }
}
