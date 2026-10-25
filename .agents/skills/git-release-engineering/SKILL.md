---
name: git-release-engineering
description: >-
  Git version control hygiene, branch workflows, conventional atomic commits, diff reviews,
  secret prevention, semantic versioning, Proguard/R8 optimization, and release build engineering.
  Use when committing changes, managing branches, tagging releases, or configuring build artifacts.
---

# Git & Release Engineering

This skill defines the Git version control conventions, safety guards, and release engineering practices for the Android Finance Application. It ensures clean change tracking, zero secret leakage, reproducible builds, and safe deployment.

---

## 1. Commit Guidelines & Hygiene

### Atomic & Logical Commits
- Every commit must represent a single logical change (e.g. one feature, one bugfix, or one architectural refactoring).
- Avoid monolithic commits mixing UI changes, database migrations, and build configuration.

### Conventional Commit Format
Structure commit messages following the Conventional Commits specification:
```text
<type>(<scope>): <short imperative summary>

[optional detailed body explaining why and what changed]

[optional issue/ticket reference]
```
- **Types**:
  - `feat`: New user-facing feature or domain capability.
  - `fix`: Bug fix.
  - `refactor`: Code change that neither fixes a bug nor adds a feature.
  - `test`: Adding or correcting tests.
  - `docs`: Documentation, architecture diagrams, or skill updates.
  - `chore`: Gradle dependency updates, build script adjustments, tool configs.
- **Example**:
  ```text
  feat(salary-cycle): support custom pay period starting on arbitrary day

  Handle month-end boundaries and leap year rollover when pay day falls
  on days 29, 30, or 31.
  ```

---

## 2. Safety Guards & Diff Review

1. **Pre-Commit Diff Review**:
   - Always run `git diff` or review staged files prior to committing (`git status`, `git diff --cached`).
   - Verify that unrelated formatting, whitespace changes, or unintended file deletions are not included.
2. **Never Overwrite Unrelated Code**:
   - Respect existing modules and non-conflicting features.
   - When resolving merge conflicts or refactoring, preserve intentional business logic and tests.
3. **Strict Secret Prevention**:
   - NEVER commit keystores, signing keys (`*.jks`, `*.keystore`), credentials, passwords, or local developer properties.
   - The `.gitignore` must immediately ignore:
     ```gitignore
     # Built files
     build/
     .gradle/
     app/build/
     *.apk
     *.aab

     # Local settings & sensitive data
     local.properties
     *.jks
     *.keystore
     key.properties
     .idea/
     *.iml

     # OS & temporary files
     .DS_Store
     Thumbs.db
     ```

---

## 3. Branch Strategy

- **`main`**: Production-ready, stable codebase. Every commit in `main` passes all unit and integration tests.
- **`develop`** (or topic branches): Staging area for ongoing feature integration.
- **Feature Branches (`feat/<feature-name>`)**: Isolated branches for specific tasks (e.g., `feat/biometric-auth`, `feat/xlsx-export`).
- **Release Branches (`release/vX.Y.Z`)**: Hardening, version bumping, and release validation before merging into `main`.

---

## 4. Versioning & Release Preparation

### Semantic Versioning (SemVer)
- Format: `MAJOR.MINOR.PATCH` (e.g., `1.2.0`).
  - `MAJOR`: Incompatible database migrations, major architectural redesigns.
  - `MINOR`: New features added in a backward-compatible manner.
  - `PATCH`: Backward-compatible bug fixes and security patches.
- Synchronize Gradle `versionCode` and `versionName`:
  - `versionCode`: Monotonically increasing integer (e.g., `10200`).
  - `versionName`: Semantic version string (`"1.2.0"`).

### ProGuard / R8 Release Optimization
- Enable code shrinking, obfuscation, and optimization in `build.gradle.kts` for `release` builds:
  ```kotlin
  buildTypes {
      release {
          isMinifyEnabled = true
          isShrinkResources = true
          proguardFiles(
              getDefaultProguardFile("proguard-android-optimize.txt"),
              "proguard-rules.pro"
          )
      }
  }
  ```
- Retain Room entities and JSON serialization models in `proguard-rules.pro` to prevent reflection stripping.
- Always build and test the signed release APK/bundle locally (`./gradlew assembleRelease`) before tagging.

---

## 5. Release Engineering Checklist

Before declaring a release ready:
- [ ] Working tree is clean (`git status` shows no unstaged or untracked sensitive files).
- [ ] Commit history is clean with descriptive Conventional Commit messages.
- [ ] `.gitignore` is validated; no `.jks`, `local.properties`, or build caches are tracked.
- [ ] Version code and version name are updated in `build.gradle.kts`.
- [ ] `./gradlew test` passes 100%.
- [ ] `./gradlew assembleRelease` compiles cleanly with R8 optimization enabled.
- [ ] Git tag created for release: `git tag -a v1.0.0 -m "Release v1.0.0"`.
