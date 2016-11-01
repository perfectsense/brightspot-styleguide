package com.psddev.styleguide.viewgenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;

import com.psddev.dari.util.StringUtils;

/**
 * A directory of JSON files that power a FE styleguide.
 */
class JsonDirectory {

    private static final String CONFIG_FILE_NAME = "_config.json";
    private static final String BOWER_COMPONENTS_DIRECTORY_NAME = "bower_components";

    private ViewClassGeneratorContext context;

    private Path path;
    private Set<JsonFile> files;
    private Set<JsonViewMap> viewMaps;

    // a cache of files previous read keyed off of the paths relative to this
    // directory path, normalized to be the most succinct representation.
    private Map<Path, JsonFile> normalizedFilePathsCache;

    /**
     * Creates a new JsonDirectory with the given context and path.
     *
     * @param context the view generator context that supplies configuration information.
     * @param path the path of the JSON directory.
     */
    public JsonDirectory(ViewClassGeneratorContext context, Path path) {
        this.context = context;
        this.path = path;
    }

    /**
     * Gets the view generator context.
     *
     * @return the view generator context.
     */
    public ViewClassGeneratorContext getContext() {
        return context;
    }

    /**
     * Gets the JSON directory path.
     *
     * @return the directory path.
     */
    public Path getPath() {
        return path;
    }

    /**
     * Gets the normalized form of the given path, where "normalized" is defined
     * as the most succinct path relative to this directory.
     *
     * @param ref the file to use to resolve relative paths.
     * @param filePath the path to normalize.
     * @return the normalized path.
     */
    public Path getNormalizedPath(JsonFile ref, Path filePath) {

        Path normalizedPath;
        // resolve it relative to the json base directory
        if (filePath.toString().startsWith("/") || !context.isRelativePaths()) {
            normalizedPath = getPath().resolve(StringUtils.removeStart(filePath.toString(), "/")).normalize();

        } else {
            // resolve it relative to this file.
            normalizedPath = ref.getPath().getParent().resolve(filePath.toString()).normalize();
        }

        normalizedPath = getPath().relativize(normalizedPath);

        return normalizedPath;
    }

    /**
     * Tries to find a known JSON file at the given {@code filePath} relative to
     * this directory IFF the path starts with a slash, otherwise it looks for
     * it relative to the {code ref} file argument. If no existing file is
     * found, the file is read from the filesystem (provided that the file
     * exists within this directory) and is added to normalized files cache.
     *
     * @param ref the file the path is relative to.
     * @param filePath the path to look up.
     * @return the file relative to the given file or this directory.
     */
    public JsonFile getNormalizedFile(JsonFile ref, Path filePath) {

        // get the normalized path.
        Path normalizedPath = getNormalizedPath(ref, filePath);

        // ensure that it is within the scope of this directory.
        if (!"..".equals(normalizedPath.getName(0).toString())) {

            // check to see if the file is already in the directory cache.
            JsonFile normalizedFile = normalizedFilePathsCache.get(normalizedPath);

            // if not, then try to read, parse, and normalize it.
            if (normalizedFile == null) {

                // create the file
                normalizedFile = new JsonFile(this, Paths.get(normalizedPath.toUri()));

                // reads, parses, and normalizes the file.
                normalizedFile.normalize();

                // if the file was successfully normalized, add it to the directory cache
                if (normalizedFile.isNormalized()) {
                    normalizedFilePathsCache.put(normalizedPath, normalizedFile);
                }
            }

            return normalizedFile;

        } else {
            return null;
        }
    }

    /**
     * Gets the template view configuration nearest to the normalized path, by
     * first checking its directory for a config file, and traversing up the
     * directory structure until it finds one, stopping at the root of the base
     * directory. If the normalized path is null, it just checks the root of
     * the base directory.
     *
     * @param normalizedPath the normalized path to start the search.
     * @return the nearest view configuration to the normalized path.
     */
    public TemplateViewConfiguration getTemplateViewConfiguration(Path normalizedPath) {

        if (normalizedPath != null) {

            // calls getNormalizedPath after pre-pending a slash so it knows
            // to treat the path as relative to this directory.
            normalizedPath = getNormalizedPath(null, Paths.get(StringUtils.ensureStart(normalizedPath.toString(), "/")));

            // If the path is outside of the directory, return null immediately
            if (normalizedPath.getName(0).startsWith("..")) {
                return null;
            }
        }

        while (true) {

            if (normalizedPath == null) {
                normalizedPath = Paths.get("");
            }

            Path configFilePath = getPath().resolve(normalizedPath).resolve(CONFIG_FILE_NAME);

            File configFile = configFilePath.toFile();
            if (configFile.exists()) {
                try {
                    return new TemplateViewConfiguration(configFilePath);

                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            if (normalizedPath != null
                    && normalizedPath.getNameCount() > 0
                    && !normalizedPath.getName(0).toString().isEmpty()) {

                normalizedPath = normalizedPath.getParent();
            } else {
                break;
            }
        }

        return null;
    }

    /**
     * Gets the set of all JSON view maps that were discovered and resolved in
     * this directory. Successful execution of this API means that all of the
     * discoverable JSON files are syntactically valid and all references to
     * other JSON files or templates were successfully resolved, and serves as
     * the entry point to the next phase of view generation where Java type
     * information can begin to be inferred and view classes generated.
     *
     * @return the resolved view maps.
     */
    public Set<JsonViewMap> resolveViewMaps() {
        if (viewMaps == null) {

            Set<JsonFile> files = getFiles();

            // check for errors
            checkForErrors(files);

            // parse each file
            files.forEach(JsonFile::parse);

            // check for errors
            checkForErrors(files);

            // normalize each file
            files.forEach(JsonFile::normalize);

            // check for errors
            checkForErrors(files);

            // resolve each file
            Set<JsonViewMap> fileViewMaps = files.stream()
                    .map(JsonFile::resolve)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // check for errors
            checkForErrors(files);

            // find all the nested view maps
            viewMaps = new LinkedHashSet<>();
            fileViewMaps.stream().forEach(viewMap -> populateNestedViewMaps(viewMaps, viewMap));
        }

        return viewMaps;
    }

    /*
     * Gets all the files that are discoverable within this directory.
     */
    private Set<JsonFile> getFiles() {
        if (files == null) {
            files = getFilePaths().stream()
                    .map(path -> new JsonFile(this, path))
                    .collect(Collectors.toSet());

            normalizedFilePathsCache = new HashMap<>();
            for (JsonFile file : files) {
                normalizedFilePathsCache.put(file.getRelativePath(), file);
            }
        }
        return files;
    }

    /*
     * Gets the list of file paths that should be discoverable within this directory.
     */
    public Set<Path> getFilePaths() {

        Set<String> excludedPaths = context.getExcludedPaths().stream().collect(Collectors.toCollection(HashSet::new));

        // exclude some well known paths
        excludedPaths.add(CONFIG_FILE_NAME);
        excludedPaths.add(BOWER_COMPONENTS_DIRECTORY_NAME);

        // get each json file in this directory
        return FileUtils.listFiles(path.toFile(), new String[] { "json" }, true).stream()

                // ignore files beginning with an underscore
                .filter(file -> !file.getName().startsWith("_"))

                // convert the file to a path
                .map(file -> Paths.get(file.toURI()))

                // remove excluded paths
                .filter(path -> isPathExcluded(this.path.relativize(path), excludedPaths))

                // add each file to the set
                .collect(Collectors.toSet());
    }

    /*
     * This just does a String match on each path part with each excluded path.
     * TODO: Need to come up with requirements for how excludes should work.
     */
    private static boolean isPathExcluded(Path path, Set<String> excludedPaths) {
        return !IntStream.range(0, path.getNameCount())
                .mapToObj(path::getName)
                .map(Path::toString)
                .anyMatch(name -> excludedPaths.stream()
                        .anyMatch(pattern -> pattern.equals(name)));
    }

    /*
     * Given a of top-level view map, that corresponds to a distinct JSON file,
     * this method traverses it looking for nested view maps and combines them
     * into a single set.
     */
    private void populateNestedViewMaps(Set<JsonViewMap> viewMaps, JsonValue value) {

        if (value instanceof JsonViewMap) {
            viewMaps.add((JsonViewMap) value);
            ((JsonViewMap) value).getValues().values().forEach(valueItem -> populateNestedViewMaps(viewMaps, valueItem));

        } else if (value instanceof JsonList) {
            ((JsonList) value).getValues().forEach(valueItem -> populateNestedViewMaps(viewMaps, valueItem));
        }
    }

    /*
     * Looks at each of the files to check if there were any errors detected
     * in them, and logs and throws an error if there are any.
     */
    private void checkForErrors(Set<JsonFile> files) {
        // check for errors
        Set<JsonFile> errorFiles = files.stream()
                .filter(JsonFile::hasAnyErrors)
                .collect(Collectors.toSet());

        // if there are errors, log them and stop
        if (!errorFiles.isEmpty()) {
            logErrorFiles(errorFiles);
        }
    }

    /*
     * Logs all of the errors for each file and throws an exception with the details.
     */
    private void logErrorFiles(Set<JsonFile> errorFiles) {

        StringBuilder builder = new StringBuilder();

        builder.append("Error while parsing the JSON files: \n");

        for (JsonFile file : errorFiles) {

            List<JsonFileError> errors = file.getErrors();

            builder.append("    ");
            builder.append(file.getRelativePath());
            builder.append(" has ");
            builder.append(errors.size());
            builder.append(" error");
            if (errors.size() != 1) {
                builder.append("s");
            }
            builder.append(": \n");

            for (JsonFileError error : errors) {

                builder.append("        ");
                builder.append(error.getMessage());

                Throwable throwable = error.getThrowable();
                if (throwable != null) {

                    Throwable cause = throwable.getCause();
                    if (cause != null) {
                        builder.append(" - Cause: ");
                        builder.append(cause.getMessage());
                    }

                    throwable.printStackTrace();
                }

                JsonDataLocation location = error.getLocation();
                if (location != null) {
                    // Ex. at (line no=18, column no=45, offset=521)
                    builder.append(" at (line=");
                    builder.append(location.getLineNumber());
                    builder.append(":, col=");
                    builder.append(location.getColumnNumber());
                    builder.append(", offset=");
                    builder.append(location.getStreamOffset());
                    builder.append(")");
                }
                builder.append("\n");
            }
        }

        throw new RuntimeException(builder.toString());
    }
}
