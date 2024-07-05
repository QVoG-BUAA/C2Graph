package org.c2graph.config;

import lombok.Data;
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * use jackson jar to extract info from the json.
 */
@Data
public class ProjectConfig {
    private String host;
    private Integer port;
    private String project;
    private String dir;
    private String[] includePath;
    private Boolean highPrecision;

    private static ProjectConfig config;

    public static ProjectConfig loadConfig() {
        if (config != null) {
            return config;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = Files.readString(Paths.get("config.json"));
            config = mapper.readValue(json, ProjectConfig.class);
            return config;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
