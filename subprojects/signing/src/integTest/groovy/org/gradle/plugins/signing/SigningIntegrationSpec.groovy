/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.plugins.signing

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.TestResources
import org.gradle.integtests.fixtures.executer.IntegrationTestBuildContext
import org.gradle.test.fixtures.file.TestFile
import org.junit.Rule

import java.nio.file.Path

import static org.gradle.util.TextUtil.escapeString

abstract class SigningIntegrationSpec extends AbstractIntegrationSpec {
    enum SignMethod {
        OPEN_GPG,
        GPG_CMD
    }

    @Rule public final TestResources resources = new TestResources(temporaryFolder, "keys")

    Path gpgHomeSymlink

    final String artifactId = "sign"
    final String version = "1.0"
    final String jarFileName = "$artifactId-${version}.jar"

    def setup() {
        buildFile << """
            apply plugin: 'java'
            apply plugin: 'signing'
            archivesBaseName = '$artifactId'
            group = 'sign'
            version = '$version'
        """

        file("src", "main", "java", "Thing.java") << """
            public class Thing {}
        """

        if (getSignMethod() == SignMethod.GPG_CMD) {
            setupGpgCmd()
        }
    }

    def setupGpgCmd() {
        TestFile sampleDir = new IntegrationTestBuildContext().getSamplesDir()
        sampleDir.file('signing/gnupg-signatory/gnupg-home').copyTo(file('gnupg-home'))
        sampleDir.file('signing/gnupg-signatory/gradle.properties').copyTo(file('gradle.properties'))
        GpgCmdFixture.setupGpgCmd(temporaryFolder.testDirectory)
    }

    def cleanup() {
        if (gpgHomeSymlink != null) {
            GpgCmdFixture.cleanupGpgCmd(gpgHomeSymlink)
        }
    }

    def signingConfiguration() {
        if (getSignMethod() == SignMethod.OPEN_GPG) {
            return ''
        } else {
            return 'useGpgCmd()'
        }
    }

    static class KeyInfo {
        String keyId
        String password
        String keyRingFilePath

        Map<String, String> asProperties(String name = null) {
            def prefix = name ? "signing.${name}." : "signing."
            def properties = [:]
            properties[prefix + "keyId"] = keyId
            properties[prefix + "password"] = password
            properties[prefix + "secretKeyRingFile"] = keyRingFilePath
            properties
        }

        String addAsPropertiesScript(addTo = "project.ext", name = null) {
            asProperties(name).collect { k, v ->
                "${addTo}.setProperty('${escapeString(k)}', '${escapeString(v)}')"
            }.join(";")
        }
    }

    KeyInfo getKeyInfo(set = "default") {
        new KeyInfo(
            keyId: file(set, "keyId.txt").text.trim(),
            password: file(set, "password.txt").text.trim(),
            keyRingFilePath: file(set, "secring.gpg")
        )
    }

    String getJavadocAndSourceJarsScript(String configurationName = null) {
        def tasks = """
            task("sourcesJar", type: Jar, dependsOn: classes) { 
                classifier = 'sources' 
                from sourceSets.main.allSource
            } 

            task("javadocJar", type: Jar, dependsOn: javadoc) { 
                classifier = 'javadoc' 
                from javadoc.destinationDir 
            } 
        """

        if (configurationName == null) {
            tasks
        } else {
            tasks + """
                configurations {
                    $configurationName
                }
                
                artifacts {
                    $configurationName sourcesJar, javadocJar
                }
            """
        }
    }

    String uploadArchives() {
        return """
            apply plugin: "maven"
            uploadArchives {
                repositories {
                    mavenDeployer {
                        repository(url: "file://\$buildDir/m2Repo/")
                    }
                    flatDir {
                        name "fileRepo"
                        dirs "build/fileRepo"
                    }
                    ivy {
                        url "file://\$buildDir/ivyRepo/"
                        layout "pattern"
                        artifactPattern "\$buildDir/ivyRepo/[artifact]-[revision](.[ext])"
                        ivyPattern "\$buildDir/ivyRepo/[artifact]-[revision](.[ext])"
                    }
                }
            }
        """
    }

    TestFile m2RepoFile(String name) {
        file("build", "m2Repo", "sign", artifactId, version, name)
    }

    TestFile ivyRepoFile(String name) {
        file("build", "ivyRepo", name)
    }

    TestFile fileRepoFile(String name) {
        file("build", "fileRepo", name)
    }


    void jarUploaded(String jarFileName = jarFileName) {
        assert m2RepoFile(jarFileName).exists()
        assert ivyRepoFile(jarFileName).exists()
        assert fileRepoFile(jarFileName).exists()
    }

    void jarNotUploaded() {
        assert !m2RepoFile(jarFileName).exists()
        assert !ivyRepoFile(jarFileName).exists()
        assert !fileRepoFile(jarFileName).exists()
    }

    void signatureUploaded(String jarFileName = jarFileName) {
        assert m2RepoFile("${jarFileName}.asc").exists()
        assert ivyRepoFile("${jarFileName - '.jar'}.asc").exists()
        assert fileRepoFile("${jarFileName - '.jar'}.asc").exists()
    }

    void signatureNotUploaded(String jarFileName = jarFileName) {
        assert !m2RepoFile("${jarFileName}.asc").exists()
        assert !ivyRepoFile("${jarFileName - '.jar'}.asc").exists()
        assert !fileRepoFile("${jarFileName - '.jar'}.asc").exists()
    }

    String signDeploymentPom() {
        return """
            uploadArchives {
                repositories.mavenDeployer {
                    beforeDeployment { signing.signPom(it) }
                }
            }
        """
    }

    TestFile pom(String name = "sign-1.0") {
        m2RepoFile("${name}.pom")
    }

    TestFile pomSignature(String name = "sign-1.0") {
        m2RepoFile("${name}.pom.asc")
    }

    SignMethod getSignMethod() {
        return SignMethod.OPEN_GPG
    }
}
