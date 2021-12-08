package com.dvbug.dagengine;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.SneakyThrows;

import java.io.IOException;

public class GenericClassTypeAdapter extends TypeAdapter<Class<?>> {
    @Override
    public void write(JsonWriter out, Class<?> value) throws IOException {
        out.value(value.getName());
    }

    @Override
    @SneakyThrows
    public Class<?> read(JsonReader in) {
        String s = in.nextString();
        return Class.forName(s);
    }
}
