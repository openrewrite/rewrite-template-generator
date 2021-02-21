package org.openrewrite.cli;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Parser;
import org.openrewrite.java.JavaParser;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class RewriteTemplateGeneratorTest {
    @Test
    void commonsText() {
        assertDependsOnGenerated("org.apache.commons:commons-text:1.9");
    }

    @Test
    void assertj() {
        assertDependsOnGenerated("org.assertj:assertj-core:3.19.0");
    }

    @Test
    void guava() {
        assertDependsOnGenerated("com.google.guava:guava:29.0-jre");
    }

    @SuppressWarnings({"InstantiationOfUtilityClass", "CatchMayIgnoreException"})
    private void assertDependsOnGenerated(String gav) {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream oldErr = System.err;
        PrintStream oldOut = System.out;

        int exitCode = -1;

        System.setErr(new PrintStream(err));
        System.setOut(new PrintStream(out));

        try {
            CommandLine cmd = new CommandLine(new RewriteTemplateGenerator());

            exitCode = cmd.execute(
                    "depends-on",
                    "--dependency=" + gav);

            System.setErr(oldErr);
            System.setOut(oldOut);

            String[] sources = out.toString().split("---");

            for (String s : sources) {
                AtomicBoolean failed = new AtomicBoolean(false);
                JavaParser.fromJavaVersion()
                        .doOnParse(new Parser.Listener() {
                            @Override
                            public void onWarn(@NotNull String message) {
                                System.out.println("WARNING: " + message);
                                failed.set(true);
                            }
                        })
                        .build()
                        .parse(s);

                if(failed.get()) {
                    System.out.println(s);
                }
            }
        } catch (Throwable t) {
            fail("Threw exception", t);
        } finally {
            System.setErr(oldErr);
            System.setOut(oldOut);
        }

        assertThat(exitCode).isEqualTo(0);
    }
}
