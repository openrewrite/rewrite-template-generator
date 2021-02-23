package org.openrewrite.cli;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Parser;
import org.openrewrite.java.JavaParser;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
    void junitJupiter() {
        assertThat(0).isEqualTo(0);
        printStubFor("org.junit.jupiter:junit-jupiter-api:5.7.1",
                "org.junit.jupiter.api.Assertions");
    }

    @Test
    void assertjAssertions() {
        assertThat(0).isEqualTo(0);
        printStubFor("org.assertj:assertj-core:3.19.0",
                "org.assertj.core.api.Assertions",
                "org.assertj.core.api.AbstractBigDecimalAssert",
                "org.assertj.core.api.AbstractBigIntegerAssert",
                "org.assertj.core.api.AbstractBooleanArrayAssert",
                "org.assertj.core.api.AbstractBooleanAssert",
                "org.assertj.core.api.AbstractByteArrayAssert",
                "org.assertj.core.api.AbstractByteAssert",
                "org.assertj.core.api.AbstractCharacterAssert",
                "org.assertj.core.api.AbstractCharArrayAssert",
                "org.assertj.core.api.AbstractCharSequenceAssert",
                "org.assertj.core.api.AbstractComparableAssert",
                "org.assertj.core.api.AbstractDateAssert",
                "org.assertj.core.api.AbstractDoubleArrayAssert",
                "org.assertj.core.api.AbstractDoubleAssert",
                "org.assertj.core.api.AbstractDurationAssert",
                "org.assertj.core.api.AbstractFileAssert",
                "org.assertj.core.api.AbstractFloatArrayAssert",
                "org.assertj.core.api.AbstractFloatAssert",
                "org.assertj.core.api.AbstractInputStreamAssert",
                "org.assertj.core.api.AbstractInstantAssert",
                "org.assertj.core.api.AbstractIntArrayAssert",
                "org.assertj.core.api.AbstractIntegerAssert",
                "org.assertj.core.api.AbstractLocalDateAssert",
                "org.assertj.core.api.AbstractLocalDateTimeAssert",
                "org.assertj.core.api.AbstractLocalTimeAssert",
                "org.assertj.core.api.AbstractLongArrayAssert",
                "org.assertj.core.api.AbstractLongAssert",
                "org.assertj.core.api.AbstractOffsetDateTimeAssert",
                "org.assertj.core.api.AbstractOffsetTimeAssert",
                "org.assertj.core.api.AbstractPathAssert",
                "org.assertj.core.api.AbstractPeriodAssert",
                "org.assertj.core.api.AbstractShortArrayAssert",
                "org.assertj.core.api.AbstractShortAssert",
                "org.assertj.core.api.AbstractStringAssert",
                "org.assertj.core.api.AbstractThrowableAssert",
                "org.assertj.core.api.AbstractUriAssert",
                "org.assertj.core.api.AbstractUrlAssert",
                "org.assertj.core.api.AbstractZonedDateTimeAssert",
                "org.assertj.core.api.AtomicBooleanAssert",
                "org.assertj.core.api.AtomicIntegerArrayAssert",
                "org.assertj.core.api.AtomicIntegerAssert",
                "org.assertj.core.api.AtomicIntegerFieldUpdaterAssert",
                "org.assertj.core.api.AtomicLongArrayAssert",
                "org.assertj.core.api.AtomicLongAssert",
                "org.assertj.core.api.AtomicLongFieldUpdaterAssert",
                "org.assertj.core.api.AtomicMarkableReferenceAssert",
                "org.assertj.core.api.AtomicReferenceArrayAssert",
                "org.assertj.core.api.AtomicReferenceAssert",
                "org.assertj.core.api.AtomicReferenceFieldUpdaterAssert",
                "org.assertj.core.api.AtomicStampedReferenceAssert",
                "org.assertj.core.api.Boolean2DArrayAssert",
                "org.assertj.core.api.Byte2DArrayAssert",
                "org.assertj.core.api.Char2DArrayAssert",
                "org.assertj.core.api.ClassAssert",
                "org.assertj.core.api.ClassBasedNavigableIterableAssert",
                "org.assertj.core.api.ClassBasedNavigableListAssert",
                "org.assertj.core.api.CompletableFutureAssert",
                "org.assertj.core.api.Double2DArrayAssert",
                "org.assertj.core.api.DoublePredicateAssert",
                "org.assertj.core.api.FactoryBasedNavigableIterableAssert",
                "org.assertj.core.api.FactoryBasedNavigableListAssert",
                "org.assertj.core.api.Float2DArrayAssert",
                "org.assertj.core.api.FutureAssert",
                "org.assertj.core.api.Int2DArrayAssert",
                "org.assertj.core.api.IntPredicateAssert",
                "org.assertj.core.api.IterableAssert",
                "org.assertj.core.api.IteratorAssert",
                "org.assertj.core.api.ListAssert",
                "org.assertj.core.api.Long2DArrayAssert",
                "org.assertj.core.api.LongAdderAssert",
                "org.assertj.core.api.LongPredicateAssert",
                "org.assertj.core.api.MapAssert",
                "org.assertj.core.api.NotThrownAssert",
                "org.assertj.core.api.Object2DArrayAssert",
                "org.assertj.core.api.ObjectArrayAssert",
                "org.assertj.core.api.ObjectAssert",
                "org.assertj.core.api.OptionalAssert",
                "org.assertj.core.api.OptionalDoubleAssert",
                "org.assertj.core.api.OptionalIntAssert",
                "org.assertj.core.api.OptionalLongAssert",
                "org.assertj.core.api.PredicateAssert",
                "org.assertj.core.api.Short2DArrayAssert",
                "org.assertj.core.api.SpliteratorAssert",
                "org.assertj.core.api.ThrowableTypeAssert",
                "org.assertj.core.api.");
    }

    @Test
    void guava() {
        assertDependsOnGenerated("com.google.guava:guava:29.0-jre");
    }

    @SuppressWarnings({"InstantiationOfUtilityClass", "CatchMayIgnoreException"})
    private void printStubFor(String gav, String... types) {
        int exitCode = -1;

        try {
            CommandLine cmd = new CommandLine(new RewriteTemplateGenerator());

            exitCode = cmd.execute(
                    "depends-on",
                    "--types=" + String.join(",", types),
                    "--dependency=" + gav);
        } catch (Throwable t) {
            fail("Threw exception", t);
        }

        assertThat(exitCode).isEqualTo(0);
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

                if (failed.get()) {
                    System.out.println(s);
                }
            }
        } catch (Throwable t) {
            fail("Threw exception", t);
        } finally {
            System.setErr(oldErr);
            System.setOut(oldOut);
        }

        if (exitCode != 0) {
            fail("Failed. Output on the console was: " + out.toString());
        }
    }
}
