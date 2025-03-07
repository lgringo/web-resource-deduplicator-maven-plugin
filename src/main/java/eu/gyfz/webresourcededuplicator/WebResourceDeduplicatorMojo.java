package eu.gyfz.webresourcededuplicator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Scan for html files
 */
@Mojo(name = WebResourceDeduplicatorMojo.GOAL_NAME, aggregator = true)
public class WebResourceDeduplicatorMojo
        extends AbstractMojo {

    public static final String GOAL_NAME = "deduplicate";

    private static final Set<String> DEFAULT_RESOURCES_EXTENSIONS = Set.of(".css", ".js", ".png", ".jpg", ".jpeg", ".webp", ".gif", ".svg", ".woff2", ".woff", ".ttf", ".eot", ".otf");
    private static final Set<String> EXTENSIONS_TO_PARSE = Set.of(".html", ".css");

    private static class ResourceInfo {
        String name;
        File file;
        String checksum;
        List<File> referencingFiles;

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
     */
    @Parameter(defaultValue = "${project.build.directory}/staging", property = "stagingDirectory")
    private File stagingDirectory;

    /**
     * Set this to 'true' to skip removing duplicate resources.
     */
    @Parameter(defaultValue = "false")
    private boolean skip;

    /**
     * List of extensions for resource files (default are : .css,.js,.png,.jpg,.jpeg,.webp,.gif,.svg,.woff2,.woff,.ttf,.eot,.otf);
     */
    @Parameter
    private Set<String> resourcesExtensions;

    private final Map<File, ResourceInfo> referencedResources = new HashMap<>();

    public void execute()
            throws MojoExecutionException, MojoFailureException {

        if (skip) {
            getLog().info("skip : Skipping web-resource-deduplicator-maven-plugin");
            return;
        }

        if (!stagingDirectory.exists()) {
            throw new MojoExecutionException("Non-existent folder :" + stagingDirectory + "(parameter stagingDirectory)");
        }

        if (resourcesExtensions == null) {
            resourcesExtensions = DEFAULT_RESOURCES_EXTENSIONS;
        }

        // Parse HTML and CSS files
        parseFiles(stagingDirectory);

        // Compare resources and remove duplicates
        removeDuplicates(referencedResources);

        // Remove all resources without referencingFile
        referencedResources.entrySet().removeIf(e -> e.getValue().referencingFiles.isEmpty());

        referencedResources.keySet().forEach(f -> System.out.println("Referenced resource : " + f));

        // Remove unused resources
        removeUnusedResources(stagingDirectory, referencedResources);
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
            Pattern pattern = Pattern.compile("url\\([\"'](.*?)\\1\\)");
            Matcher matcher = pattern.matcher(content);

            while (matcher.find()) {
                String url = matcher.group(1);
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
                referencedResources.computeIfAbsent(resourceFile, (c) -> new ResourceInfo(url, resourceFile, checksum)).addReferencingFile(file);
            }
        }
    }

    private String calculateChecksum(File file) throws MojoFailureException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
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

    private void removeDuplicates(Map<File, ResourceInfo> referencedResources) throws MojoFailureException {
        Map<String, List<ResourceInfo>> duplicates = new HashMap<>();

        for (ResourceInfo resource : referencedResources.values()) {
            duplicates.computeIfAbsent(resource.checksum, k -> new ArrayList<>()).add(resource);
        }

        for (List<ResourceInfo> duplicateList : duplicates.values()) {
            if (duplicateList.size() > 1) {
                ResourceInfo highestResource = duplicateList.stream()
                        .min(Comparator.comparingInt(r -> r.file.getAbsolutePath().split(Pattern.quote(File.separator)).length))
                        .orElseThrow(() -> new MojoFailureException("Cannot find the highest resource of " + duplicateList.get(0)));

                for (ResourceInfo duplicate : duplicateList) {
                    if (!duplicate.file.equals(highestResource.file)) {
                        if (isExtension(duplicate.file, ".css")) {
                            // css can be resource and reference to others resources.
                            referencedResources.values().forEach(rr -> rr.referencingFiles.removeIf(rf -> rf.equals(duplicate.file)));
                        }
                        System.out.println("Deleting " + duplicate.file + ": duplicate of " + highestResource.file);
                        if (!duplicate.file.delete()) {
                            System.err.println("Error deleting file: " + duplicate.file);
                        }
                        referencedResources.remove(duplicate.file);
                        updateFiles(duplicate, highestResource);
                    }
                }
            }
        }
    }

    private void updateFiles(ResourceInfo oldResource, ResourceInfo newResource) {
        for (File file : oldResource.referencingFiles) {
            try {
                String content = new String(Files.readAllBytes(file.toPath()));
                content = content.replace(oldResource.name, file.getParentFile().toPath().relativize(newResource.file.toPath()).toString());
                Files.write(file.toPath(), content.getBytes());
            } catch (IOException e) {
                System.err.println("Error updating file: " + file);
            }
        }
    }

    private void removeUnusedResources(File rootDir, Map<File, ResourceInfo> referencedResources) throws MojoFailureException {
        File[] files = rootDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    removeUnusedResources(file, referencedResources);
                    if (isDirectoryEmpty(file.toPath())) {
                        System.out.println("Deleting " + file + ": empty directory");
                        if (!file.delete()) {
                            System.err.println("Error deleting directory: " + file);
                        }
                    }
                } else if (isExtension(file, resourcesExtensions)) {
                    if (referencedResources.get(file) == null) {
                        System.out.println("Deleting " + file + ": resource not referenced anymore");
                        if (!file.delete()) {
                            System.err.println("Error deleting file: " + file);
                        }
                    }
                }
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
