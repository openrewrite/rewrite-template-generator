![Logo](https://github.com/openrewrite/rewrite/raw/main/doc/logo-oss.png)
# rewrite-template-generator

### What is this?

This generates parser stubs for usage with JavaTemplate.

### Usage

```sh
# generate a stub for a single type:
./gradlew run --args="depends-on --dependency=org.assertj:assertj-core:3.19.0 --types=org.assertj.core.api.Assert"

# generate multiple stubs using multiple types:
./gradlew run --args="depends-on --dependency=org.assertj:assertj-core:3.19.0 --types=org.assertj.core.api.Assert,org.assertj.core.api.AbstractAssert"

# or, generally:
./gradlew run --args="--help"
```
