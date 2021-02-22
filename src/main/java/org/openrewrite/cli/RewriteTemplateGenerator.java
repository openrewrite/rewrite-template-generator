/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.cli;

import org.objectweb.asm.ClassReader;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.cache.LocalMavenArtifactCache;
import org.openrewrite.maven.cache.MavenArtifactCache;
import org.openrewrite.maven.cache.ReadOnlyLocalMavenArtifactCache;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.utilities.MavenArtifactDownloader;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Command(
        name = "rewrite-template-generator",
        version = "0.1.0",
        description = "OpenRewrite: structured code search and transformation.",
        mixinStandardHelpOptions = true,
        subcommands = {
                RewriteTemplateGenerator.DependsOn.class
        }
)
public class RewriteTemplateGenerator {
    @SuppressWarnings("InstantiationOfUtilityClass")
    public static void main(String... args) {
        int exitCode = new CommandLine(new RewriteTemplateGenerator()).execute(args);
        System.exit(exitCode);
    }

    @Command(name = "depends-on", description = "Builds type information into a template.")
    static class DependsOn implements Callable<Integer>, CommandLine.IExitCodeGenerator {
        @Option(names = "--dependency",
                description = "group:artifact:version coordinates of a dependency in a Maven repository.",
                required = true)
        private String dependency;

        @Option(names = "--maven-repository",
                description = "URL of a Maven repository to fetch dependency from.",
                defaultValue = "https://repo1.maven.org/maven2")
        @Nullable
        private String mavenRepository;

        @Option(names = {"-u", "--username"},
                description = "Username to authenticate to the Maven repository.",
                defaultValue = "https://repo1.maven.org/maven2")
        @Nullable
        private String username;

        @Option(names = {"-p", "--password"},
                description = "Password to authenticate to the Maven repository.",
                arity = "0..1",
                interactive = true)
        @Nullable
        private String password;

        @Option(names = "--cache-dir",
                description = "The directory to download Maven artifacts to.")
        @Nullable
        private String cacheDir;

        private int exitCode;

        @Option(names = {"-s", "--stacktrace"},
                description = "Print out the stacktrace for all exceptions.",
                defaultValue = "false")
        private boolean stacktrace;

        @Override
        public Integer call() {
            MavenArtifactCache mavenArtifactCache = ReadOnlyLocalMavenArtifactCache.MAVEN_LOCAL.orElse(
                    new LocalMavenArtifactCache(cacheDir == null ?
                            Paths.get(System.getProperty("user.home"), ".rewrite-cache", "artifacts") :
                            Paths.get(cacheDir)));

            String[] gav = dependency.split(":");
            if (gav.length != 3) {
                System.out.println("Dependency must be of the form group:artifact:version");
                return CommandLine.ExitCode.USAGE;
            }

            Maven maven = MavenParser.builder()
                    .resolveOptional(false)
                    .build()
                    .parse("<project>" +
                            "<modelVersion>4.0.0</modelVersion>" +
                            "<groupId>org.openrewrite</groupId>" +
                            "<artifactId>rewrite-template-generator</artifactId>" +
                            "<version>1</version>" +
                            "<dependencies>" + "" +
                            "  <dependency>" +
                            "    <groupId>" + gav[0] + "</groupId>" +
                            "    <artifactId>" + gav[1] + "</artifactId>" +
                            "    <version>" + gav[2] + "</version>" +
                            "  </dependency>" +
                            "</dependencies>" +
                            "</project>")
                    .get(0);

            MavenArtifactDownloader mavenArtifactDownloader = new MavenArtifactDownloader(mavenArtifactCache, null,
                    t -> {
                        throw new IllegalStateException("Unable to download artifact.", t);
                    });

            Collection<Pom.Dependency> dependencies = maven.getModel().getDependencies();
            if (dependencies.isEmpty()) {
                System.out.println("Unable to resolve the dependency");
                return CommandLine.ExitCode.USAGE;
            }

            Path artifact = mavenArtifactDownloader.downloadArtifact(dependencies.iterator().next());
            if (artifact == null) {
                System.out.println("Unable to download artifact");
                return CommandLine.ExitCode.USAGE;
            }

            PublicApiPrinter publicApiPrinter = new PublicApiPrinter();
            try (FileInputStream fis = new FileInputStream(artifact.toFile());
                 ZipInputStream zis = new ZipInputStream(fis)) {
                ZipEntry zipEntry;
                while ((zipEntry = zis.getNextEntry()) != null) {
                    if (!zipEntry.isDirectory() && zipEntry.getName().endsWith(".class")) {
                        new ClassReader(zis).accept(publicApiPrinter, ClassReader.SKIP_DEBUG);
                    }
                }
                publicApiPrinter.print();
            } catch (IOException e) {
                if (stacktrace) {
                    throw new UncheckedIOException(e);
                }
                return CommandLine.ExitCode.SOFTWARE;
            }

            return CommandLine.ExitCode.OK;
        }

        @Override
        public int getExitCode() {
            return exitCode;
        }
    }
}
