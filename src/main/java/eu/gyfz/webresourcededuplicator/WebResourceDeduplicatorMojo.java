package eu.gyfz.webresourcededuplicator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Remove identical resources declared in the HTML and CSS files of the project but stored in different directories.
 * Also remove resources with no references in the HTML and CSS files of the project.
 *
 * @since 1.0
 */
@Mojo(name = WebResourceDeduplicatorMojo.GOAL_NAME, aggregator = true)
public class WebResourceDeduplicatorMojo extends AbstractMojo {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebResourceDeduplicatorMojo.class);

    public static final String GOAL_NAME = "deduplicate";

    public static final Set<String> DEFAULT_RESOURCES_EXTENSIONS = Set.of(".css", ".js", ".png", ".jpg", ".jpeg", ".webp", ".gif", ".svg", ".woff2", ".woff", ".ttf", ".eot", ".otf");

    public static final Set<String> EXTENSIONS_TO_PARSE = Set.of(".html", ".css");

    private static class ResourceInfo {
        private final String name;
        private final File file;
        private final String checksum;
        private final List<File> referencingFiles;

        ResourceInfo(String name, File file, String checksum) {
            this.name = name;
            this.file = file;
            this.checksum = checksum;
            referencingFiles = new ArrayList<>();
        }

        void addReferencingFile(File file) {
            referencingFiles.add(file);
        }
    }

    /**
     * Staging directory location. This needs to be an absolute path,
     * like C:\stagingArea\myProject\ on Windows or /stagingArea/myProject/ on Unix.
     * If this is not specified, the site will be staged in ${project.build.directory}/staging.
     *
     * @since 1.0
     */
    @Parameter(defaultValue = "${project.build.directory}/staging", property = "stagingDirectory")
    private File stagingDirectory;

    /**
     * Set this to 'true' to skip removing duplicate resources.
     *
     * @since 1.0
     */
    @Parameter(defaultValue = "false")
    private boolean skip;

    /**
     * List of extensions for resource files (default are : .css, .js, .png, .jpg, .jpeg, .webp, .gif, .svg, .woff2, .woff, .ttf, .eot, .otf);
     *
     * @since 1.0
     */
    @Parameter
    private Set<String> resourcesExtensions;

    private final Map<File, ResourceInfo> referencedResources = new HashMap<>();

    private long duplicatedFileSize = 0;

    private long unusedFileSize = 0;

    public void execute() throws MojoExecutionException, MojoFailureException {

        if (skip) {
            LOGGER.info("skip : Skipping web-resource-deduplicator-maven-plugin");
            return;
        }

        if (!stagingDirectory.exists()) {
            throw new MojoExecutionException("Non-existent folder :" + stagingDirectory + "(parameter stagingDirectory)");
        }

        if (resourcesExtensions == null) {
            resourcesExtensions = DEFAULT_RESOURCES_EXTENSIONS;
        }
        LOGGER.debug("Deduplicating resources in {}, having extensions {}", stagingDirectory, resourcesExtensions);

        // Parse HTML and CSS files
        parseFiles(stagingDirectory);

        // Compare resources and remove duplicates
        removeDuplicates();

        LOGGER.info("Removed duplicated files size : {} bytes", duplicatedFileSize);

        // Remove all resources without referencingFile
        referencedResources.entrySet().removeIf(e -> e.getValue().referencingFiles.isEmpty());

        referencedResources.keySet().forEach(f -> LOGGER.debug("Referenced resource : {}", f));

        // Remove unused resources
        removeUnusedResources(stagingDirectory);

        LOGGER.info("Removed unused files size : {} bytes", unusedFileSize);
        LOGGER.info("Total space gained : {} bytes", duplicatedFileSize + unusedFileSize);
    }

    private void parseFiles(File rootDir) throws MojoFailureException {
        File[] files = rootDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    parseFiles(file);
                } else if (isExtension(file, EXTENSIONS_TO_PARSE)) {
                    parseFile(file);
                }
            }
        }
    }

    private void parseFile(File file) throws MojoFailureException {
        String content;
        try {
            content = new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new MojoFailureException("Error reading file " + file, e);
        }
        if (isExtension(file, ".html")) {
            // Use regex to extract links, scripts, and images
            Pattern pattern = Pattern.compile("(src|href)=([\"'])(.*?)\\2");
            Matcher matcher = pattern.matcher(content);

            while (matcher.find()) {
                String url = matcher.group(3);
                addResource(file, url);
            }
        } else if (isExtension(file, ".css")) {
            // Use regex to extract URLs from CSS
            Pattern pattern = Pattern.compile("url\\(([\\\"'])(.*?)\\1\\)");
            Matcher matcher = pattern.matcher(content);

            while (matcher.find()) {
                String url = matcher.group(2);
                addResource(file, url);
            }
        }

    }

    private void addResource(File file, String url) throws MojoFailureException {
        // Only relatives URL
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("/")) {
            File f = new File(file.getParent(), url);
            File resourceFile;
            try {
                resourceFile = f.getCanonicalFile();
            } catch (IOException e) {
                throw new MojoFailureException("Error getting canonical form of file :" + file, e);
            }
            if (resourceFile.exists()) {
                String checksum = calculateChecksum(resourceFile);
                referencedResources.computeIfAbsent(resourceFile, c -> new ResourceInfo(url, resourceFile, checksum)).addReferencingFile(file);
            }
        }
    }

    private String calculateChecksum(File file) throws MojoFailureException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5"); // NOSONAR (MD5 is used for checksum, not for security)
        } catch (NoSuchAlgorithmException e) {
            throw new MojoFailureException("Error getting instance of MD5 algorithm", e);
        }
        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new MojoFailureException("Error reading file " + file, e);
        }
        return Base64.getEncoder().encodeToString(md.digest(md.digest(fileBytes)));
    }

    private void removeDuplicates() throws MojoFailureException {
        Map<String, List<ResourceInfo>> duplicatesByChecksum = duplicatesByChecksum(referencedResources);

        for (List<ResourceInfo> duplicateList : duplicatesByChecksum.values()) {
            if (needDeduplication(duplicateList)) {
                ResourceInfo highestResource = getHighestResource(duplicateList);

                for (ResourceInfo duplicate : duplicateList) {
                    if (!duplicate.file.equals(highestResource.file)) {
                        if (isExtension(duplicate.file, ".css")) {
                            // css can be resource and reference to others resources.
                            referencedResources.values().forEach(rr -> rr.referencingFiles.removeIf(rf -> rf.equals(duplicate.file)));
                        }
                        deleteDuplicatedResource(highestResource, duplicate);
                        referencedResources.remove(duplicate.file);
                        updateFiles(duplicate, highestResource);
                    }
                }
            }
        }
    }

    private boolean needDeduplication(List<ResourceInfo> duplicateList) {
        return duplicateList.size() > 1 && isExtension(duplicateList.get(0).file, resourcesExtensions);
    }

    private void deleteDuplicatedResource(ResourceInfo highestResource, ResourceInfo duplicate) {
        LOGGER.debug("Deleting {} : duplicate of {}", duplicate.file, highestResource.file);
        long size = duplicate.file.length();
        if (!duplicate.file.delete()) {
            LOGGER.error("Error deleting file: {}", duplicate.file);
        } else {
            this.duplicatedFileSize += size;
        }
    }

    private static ResourceInfo getHighestResource(List<ResourceInfo> duplicateList) throws MojoFailureException {
        return duplicateList.stream().min(Comparator
                .comparingInt((ResourceInfo r) -> r.file.getAbsolutePath().split(Pattern.quote(File.separator)).length)
                .thenComparing(r -> r.file.getAbsolutePath())
        ).orElseThrow(() -> new MojoFailureException("Cannot find the highest resource of " + duplicateList.get(0)));
    }

    private static Map<String, List<ResourceInfo>> duplicatesByChecksum(Map<File, ResourceInfo> referencedResources) {
        Map<String, List<ResourceInfo>> duplicates = new HashMap<>();
        for (ResourceInfo resource : referencedResources.values()) {
            duplicates.computeIfAbsent(resource.checksum, k -> new ArrayList<>()).add(resource);
        }
        return duplicates;
    }

    private void updateFiles(ResourceInfo oldResource, ResourceInfo newResource) {
        for (File file : oldResource.referencingFiles) {
            try {
                String content = new String(Files.readAllBytes(file.toPath()));
                content = content.replace(oldResource.name, file.getParentFile().toPath().relativize(newResource.file.toPath()).toString());
                Files.write(file.toPath(), content.getBytes());
            } catch (IOException e) {
                LOGGER.error("Error updating file: {}", file);
            }
        }
    }

    private void removeUnusedResources(File rootDir) throws MojoFailureException {
        File[] files = rootDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    removeUnusedResources(file);
                    deleteEmptyDirectory(file);
                } else if (isExtension(file, resourcesExtensions) && referencedResources.get(file) == null) {
                    deleteUnusedFile(file);
                }

            }
        }
    }

    private void deleteUnusedFile(File file) {
        LOGGER.debug("Deleting {} : resource not referenced anymore", file);
        long size = file.length();
        if (!file.delete()) {
            LOGGER.error("Error deleting file: {}", file);
        } else {
            this.unusedFileSize += size;
        }
    }

    private void deleteEmptyDirectory(File file) throws MojoFailureException {
        if (isDirectoryEmpty(file.toPath())) {
            LOGGER.debug("Deleting {} : empty directory", file);
            if (!file.delete()) {
                LOGGER.error("Error deleting directory: {}", file);
            }
        }
    }

    public boolean isDirectoryEmpty(Path path) throws MojoFailureException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                return entries.findFirst().isEmpty();
            } catch (IOException e) {
                throw new MojoFailureException("Error during testing if folder " + path.toFile().getAbsolutePath() + " is empty", e);
            }
        }

        return false;
    }

    private boolean isExtension(File file, String ext) {
        return file.getName().endsWith(ext);
    }

    private boolean isExtension(File file, Collection<String> extensions) {
        return extensions.contains(getFileExtension(file));
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // empty extension
        }
        return name.substring(lastIndexOf);
    }

}
