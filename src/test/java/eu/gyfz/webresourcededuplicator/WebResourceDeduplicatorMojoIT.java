package eu.gyfz.webresourcededuplicator;

import com.soebes.itf.jupiter.extension.MavenCLIOptions;
import com.soebes.itf.jupiter.extension.MavenGoal;
import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenOption;
import com.soebes.itf.jupiter.extension.MavenRepository;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.soebes.itf.extension.assertj.MavenITAssertions.assertThat;

@MavenJupiterExtension
@MavenRepository
@MavenOption(MavenCLIOptions.VERBOSE)
public class WebResourceDeduplicatorMojoIT {

    private static final String[] HTML = {
            "core/dependency-info.html",
            "core/distribution-management.html",
            "core/plugins.html",
            "core/summary.html",
            "core/index.html",
            "core/project-info.html",
            "core/plugin-management.html",

            "extension/dependency-info.html",
            "extension/distribution-management.html",
            "extension/plugins.html",
            "extension/summary.html",
            "extension/index.html",
            "extension/dependencies.html",
            "extension/project-info.html",
            "extension/plugin-management.html",

            "dependency-info.html",
            "distribution-management.html",
            "plugins.html",
            "foo.html",
            "summary.html",
            "index.html",
            "project-info.html",
            "plugin-management.html",
            "modules.html",
            "dependency-convergence.html"
    };

    private static final String[] CSS = {
            "css/site.css",
            "css/apache-maven-fluido-2.0.1.min.css",
            "css/print.css"
    };

    private static final String[] JS = {
            "js/apache-maven-fluido-2.0.1.min.js"
    };

    private static final String[] ALL_JS = {
            "js/apache-maven-fluido-2.0.1.min.js",
            "core/js/apache-maven-fluido-2.0.1.min.js",
            "extension/js/apache-maven-fluido-2.0.1.min.js"
    };

    private static final String[] PNG = {
            // Same name but different image.
            "extension/images/guinea-pig-logo-min.png",
            "images/accessories-text-editor.png",
            "images/application-certificate.png",
            "images/contact-new.png",
            "images/document-properties.png",
            "images/drive-harddisk.png",
            "images/guinea-pig-logo-min.png",
            "images/guinea-pig-logo.png",
            "images/image-x-generic.png",
            "images/internet-web-browser.png",
            "images/network-server.png",
            "images/package-x-generic.png",
            "images/window-new.png",
            "img/glyphicons-halflings-white.png",
            "img/glyphicons-halflings.png"
    };


    private static final String[] ALL_PNG = {
            "core/images/accessories-text-editor.png",
            "core/images/apache-maven-project-2.png",
            "core/images/application-certificate.png",
            "core/images/contact-new.png",
            "core/images/document-properties.png",
            "core/images/drive-harddisk.png",
            "core/images/guinea-pig-logo-min.png",
            "core/images/guinea-pig-logo.png",
            "core/images/image-x-generic.png",
            "core/images/internet-web-browser.png",
            "core/images/logos/build-by-maven-black.png",
            "core/images/logos/build-by-maven-white.png",
            "core/images/logos/maven-feather.png",
            "core/images/network-server.png",
            "core/images/package-x-generic.png",
            "core/images/profiles/pre-release.png",
            "core/images/profiles/retired.png",
            "core/images/profiles/sandbox.png",
            "core/images/rss.png",
            "core/images/window-new.png",
            "core/img/glyphicons-halflings-white.png",
            "core/img/glyphicons-halflings.png",
            "extension/images/accessories-text-editor.png",
            "extension/images/apache-maven-project-2.png",
            "extension/images/application-certificate.png",
            "extension/images/contact-new.png",
            "extension/images/document-properties.png",
            "extension/images/drive-harddisk.png",
            "extension/images/guinea-pig-logo-min.png",
            "extension/images/image-x-generic.png",
            "extension/images/internet-web-browser.png",
            "extension/images/logos/build-by-maven-black.png",
            "extension/images/logos/build-by-maven-white.png",
            "extension/images/logos/maven-feather.png",
            "extension/images/network-server.png",
            "extension/images/package-x-generic.png",
            "extension/images/profiles/pre-release.png",
            "extension/images/profiles/retired.png",
            "extension/images/profiles/sandbox.png",
            "extension/images/rss.png",
            "extension/images/window-new.png",
            "extension/img/glyphicons-halflings-white.png",
            "extension/img/glyphicons-halflings.png",
            "images/accessories-text-editor.png",
            "images/apache-maven-project-2.png",
            "images/application-certificate.png",
            "images/contact-new.png",
            "images/document-properties.png",
            "images/drive-harddisk.png",
            "images/guinea-pig-logo-min.png",
            "images/guinea-pig-logo.png",
            "images/image-x-generic.png",
            "images/internet-web-browser.png",
            "images/logos/build-by-maven-black.png",
            "images/logos/build-by-maven-white.png",
            "images/logos/maven-feather.png",
            "images/network-server.png",
            "images/package-x-generic.png",
            "images/profiles/pre-release.png",
            "images/profiles/retired.png",
            "images/profiles/sandbox.png",
            "images/rss.png",
            "images/window-new.png",
            "img/glyphicons-halflings-white.png",
            "img/glyphicons-halflings.png"
    };

    private static final String[] JPG = {
            // File used only in module core
            "core/images/guinea-pig-logo-only-here.jpg"
    };

    private static final String[] JPEG = {
            // File used in modules core and extension, but not in parent.
            // As core and extension have the same depth, the file is kept in module core (alphabetical order).
            "core/images/guinea-pig-mirror.jpeg"
    };

    private static final String[] GIF = {
            "extension/images/close.gif",
            "extension/images/icon_info_sml.gif",
            "images/icon_success_sml.gif"
    };

    private static final String[] ALL_GIF = {
            "core/images/icon_error_sml.gif",
            "core/images/update.gif",
            "core/images/remove.gif",
            "core/images/icon_info_sml.gif",
            "core/images/fix.gif",
            "core/images/add.gif",
            "core/images/icon_help_sml.gif",
            "core/images/icon_success_sml.gif",
            "core/images/icon_warning_sml.gif",
            "extension/images/icon_error_sml.gif",
            "extension/images/update.gif",
            "extension/images/close.gif",
            "extension/images/remove.gif",
            "extension/images/icon_info_sml.gif",
            "extension/images/fix.gif",
            "extension/images/add.gif",
            "extension/images/icon_help_sml.gif",
            "extension/images/icon_success_sml.gif",
            "extension/images/icon_warning_sml.gif",
            "images/icon_error_sml.gif",
            "images/update.gif",
            "images/remove.gif",
            "images/icon_info_sml.gif",
            "images/fix.gif",
            "images/add.gif",
            "images/icon_help_sml.gif",
            "images/icon_success_sml.gif",
            "images/icon_warning_sml.gif"
    };

    private static final String[] ALL_SVG = {
            "fonts/glyphicons-halflings-regular.svg",
            "extension/fonts/glyphicons-halflings-regular.svg",
            "core/fonts/glyphicons-halflings-regular.svg"
    };

    private static final String[] ALL_EOT = {
            "fonts/glyphicons-halflings-regular.eot",
            "extension/fonts/glyphicons-halflings-regular.eot",
            "core/fonts/glyphicons-halflings-regular.eot"
    };

    private static final String[] ALL_WOFF = {
            "fonts/glyphicons-halflings-regular.woff",
            "extension/fonts/glyphicons-halflings-regular.woff",
            "core/fonts/glyphicons-halflings-regular.woff"
    };

    private static final String[] UNUSED_EXTENSIONS = {
            "webp", "svg", "woff2", "woff", "ttf", "eot", "otf"
    };

    @MavenTest
    @MavenGoal("clean")
    @MavenGoal("verify")
    @MavenGoal("site:site")
    @MavenGoal("site:stage")
    @MavenGoal("eu.gyfz:web-resource-deduplicator-maven-plugin:deduplicate")
    void guinea_pig(MavenExecutionResult result) {
        assertThat(result).isSuccessful();
        Path target = result.getMavenProjectResult().getTargetProjectDirectory().resolve("target").resolve("staging");

        List<String> extToFind = new ArrayList<>(WebResourceDeduplicatorMojo.DEFAULT_RESOURCES_EXTENSIONS);
        extToFind.add(".html");

        String[] extensions = extToFind.stream()
                .map(s -> s.substring(1)).toArray(String[]::new);
        Collection<File> allResources = FileUtils.listFiles(target.toFile(), extensions, true);

        Map<String, List<File>> resourcesByExt = allResources.stream().collect(Collectors.groupingBy(f -> FilenameUtils.getExtension(f.getName())));

        List<File> expectedHtmls = Arrays.stream(HTML).map(s -> new File(target.toFile(), s)).collect(Collectors.toList());
        assertThat(resourcesByExt.get("html")).containsExactlyInAnyOrderElementsOf(expectedHtmls);

        List<File> expectedCss = Arrays.stream(CSS).map(s -> new File(target.toFile(), s)).collect(Collectors.toList());
        assertThat(resourcesByExt.get("css")).containsExactlyInAnyOrderElementsOf(expectedCss);

        List<File> expectedJs = Arrays.stream(JS).map(s -> new File(target.toFile(), s)).collect(Collectors.toList());
        assertThat(resourcesByExt.get("js")).containsExactlyInAnyOrderElementsOf(expectedJs);

        List<File> expectedPng = Arrays.stream(PNG).map(s -> new File(target.toFile(), s)).collect(Collectors.toList());
        assertThat(resourcesByExt.get("png")).containsExactlyInAnyOrderElementsOf(expectedPng);

        List<File> expectedJpg = Arrays.stream(JPG).map(s -> new File(target.toFile(), s)).collect(Collectors.toList());
        assertThat(resourcesByExt.get("jpg")).containsExactlyInAnyOrderElementsOf(expectedJpg);

        List<File> expectedJpeg = Arrays.stream(JPEG).map(s -> new File(target.toFile(), s)).collect(Collectors.toList());
        assertThat(resourcesByExt.get("jpeg")).containsExactlyInAnyOrderElementsOf(expectedJpeg);

        List<File> expectedGif = Arrays.stream(GIF).map(s -> new File(target.toFile(), s)).collect(Collectors.toList());
        assertThat(resourcesByExt.get("gif")).containsExactlyInAnyOrderElementsOf(expectedGif);

        // Unused extesions :
        for (String ext : UNUSED_EXTENSIONS) {
            assertThat(resourcesByExt.get(ext)).isNull();
        }
    }

    @MavenTest
    @MavenGoal("clean")
    @MavenGoal("verify")
    @MavenGoal("site:site")
    @MavenGoal("site:stage")
    @MavenGoal("eu.gyfz:web-resource-deduplicator-maven-plugin:deduplicate")
    void guinea_pig_only_css(MavenExecutionResult result) {
        // Same site, but only CSS are deduplicated
        assertThat(result).isSuccessful();
        Path target = result.getMavenProjectResult().getTargetProjectDirectory().resolve("target").resolve("staging");

        List<String> extToFind = new ArrayList<>(WebResourceDeduplicatorMojo.DEFAULT_RESOURCES_EXTENSIONS);
        extToFind.add(".html");

        String[] extensions = extToFind.stream()
                .map(s -> s.substring(1)).toArray(String[]::new);
        Collection<File> allResources = FileUtils.listFiles(target.toFile(), extensions, true);

        Map<String, List<File>> resourcesByExt = allResources.stream().collect(Collectors.groupingBy(f -> FilenameUtils.getExtension(f.getName())));

        List<File> expectedHtmls = Arrays.stream(HTML).map(s -> new File(target.toFile(), s)).collect(Collectors.toList());
        assertThat(resourcesByExt.get("html")).containsExactlyInAnyOrderElementsOf(expectedHtmls);

        List<File> expectedCss = Arrays.stream(CSS).map(s -> new File(target.toFile(), s)).collect(Collectors.toList());
        assertThat(resourcesByExt.get("css")).containsExactlyInAnyOrderElementsOf(expectedCss);

        // JS in not deduplicated
        List<File> expectedJs = Arrays.stream(ALL_JS).map(s -> new File(target.toFile(), s)).collect(Collectors.toList());
        assertThat(resourcesByExt.get("js")).containsExactlyInAnyOrderElementsOf(expectedJs);

        List<File> expectedPng = Arrays.stream(ALL_PNG).map(s -> new File(target.toFile(), s)).collect(Collectors.toList());
        assertThat(resourcesByExt.get("png")).containsExactlyInAnyOrderElementsOf(expectedPng);

        List<File> expectedJpg = Arrays.stream(JPG).map(s -> new File(target.toFile(), s)).collect(Collectors.toList());
        assertThat(resourcesByExt.get("jpg")).containsExactlyInAnyOrderElementsOf(expectedJpg);

        List<File> expectedGif = Arrays.stream(ALL_GIF).map(s -> new File(target.toFile(), s)).collect(Collectors.toList());
        assertThat(resourcesByExt.get("gif")).containsExactlyInAnyOrderElementsOf(expectedGif);

        List<File> expectedSvg = Arrays.stream(ALL_SVG).map(s -> new File(target.toFile(), s)).collect(Collectors.toList());
        assertThat(resourcesByExt.get("svg")).containsExactlyInAnyOrderElementsOf(expectedSvg);

        List<File> expectedEot = Arrays.stream(ALL_EOT).map(s -> new File(target.toFile(), s)).collect(Collectors.toList());
        assertThat(resourcesByExt.get("eot")).containsExactlyInAnyOrderElementsOf(expectedEot);

        List<File> expectedWoff = Arrays.stream(ALL_WOFF).map(s -> new File(target.toFile(), s)).collect(Collectors.toList());
        assertThat(resourcesByExt.get("woff")).containsExactlyInAnyOrderElementsOf(expectedWoff);

        // Unused extensions :
        Set<String> unusedExtenstions = new HashSet<>(Set.of(UNUSED_EXTENSIONS));
        // Some unused resources aren't clean in this scenario, because we ask to clean only css and ttf.
        unusedExtenstions.add("jpeg");
        unusedExtenstions.remove("svg");
        unusedExtenstions.remove("eot");
        unusedExtenstions.remove("woff");

        for (String ext : unusedExtenstions) {
            assertThat(resourcesByExt.get(ext)).isNull();
        }
    }
}
