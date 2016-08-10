package org.lep.senate.loader.resource;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class ResourceLoader {
    public static List<String> asList(String resource) throws FileNotFoundException {
        List<String> result = null;

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResourceAsStream(resource);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            result = br.lines().collect(Collectors.toList());
        } catch(NullPointerException e) {
            throw new FileNotFoundException("Missing resource \"" + resource + "\"");
        } catch(IOException e) {
            e.printStackTrace();
        }

        return result;
    }
}
