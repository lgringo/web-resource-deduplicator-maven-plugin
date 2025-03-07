package eu.gyfz.webresourcededuplicator;

import com.soebes.itf.jupiter.extension.MavenGoal;
import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenRepository;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;

import static com.soebes.itf.extension.assertj.MavenITAssertions.assertThat;

@MavenJupiterExtension
@MavenRepository
public class WebResourceDeduplicatorMojoIT {

    @MavenTest
    @MavenGoal("clean")
    @MavenGoal("verify")
    @MavenGoal("site:site")
    @MavenGoal("site:stage")
    @MavenGoal("eu.gyfz:web-resource-deduplicator-maven-plugin:deduplicate")
    void guinea_pig(MavenExecutionResult result) {
        assertThat(result).isSuccessful();
    }
}
