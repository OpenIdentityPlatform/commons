package org.forgerock.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonValue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Created by craig on 25/09/15.
 */
public class IOUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static JsonValue jsonFromFile(String resourceFilePath) throws IOException {
        final InputStream configStream = IOUtils.class.getResourceAsStream(resourceFilePath);
        return new JsonValue(MAPPER.readValue(configStream, Map.class));
    }
}
