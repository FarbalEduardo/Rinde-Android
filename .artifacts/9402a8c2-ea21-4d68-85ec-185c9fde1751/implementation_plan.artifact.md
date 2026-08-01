# Implementation Plan - Fix KSP NoSuchFileException

The project is experiencing a `java.nio.file.NoSuchFileException` during the `:app:kspDevDebugKotlin` task. This error specifically points to a Hilt-generated file (`MainActivity_GeneratedInjector.java`) being missing during a KSP incremental build round.

This is a known issue with KSP's incremental processing in certain project configurations, especially when using Kotlin 2.x with KSP1/KSP2 transitions or Hilt.

## Proposed Changes

### Build Configuration

#### [MODIFY] [gradle.properties](file:///D:/PROYECTOSANDROID/Rinde/gradle.properties)
- Disable KSP incremental processing to ensure a clean generation of files in every build.
- Enable KSP2 explicitly to align with Kotlin 2.1.0, or at least ensure the configuration is stable.

```properties
ksp.incremental=false
ksp.useKSP2=true
```

## Verification Plan

### Automated Tests
1. Run `./gradlew clean :app:kspDevDebugKotlin` to verify the build completes successfully.
2. Run `./gradlew :app:assembleDevDebug` to ensure the full build works.

### Manual Verification
- Verify that the IDE no longer reports missing generated classes after a successful build.
