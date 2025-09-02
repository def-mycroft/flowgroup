# Android Triad MVP

This directory contains a minimal proof of concept of the A→B→C pipeline
implemented in pure Kotlin for illustration purposes.

* **A – Inlet**: not fully implemented; assumed to provide an `Envelope` and
  bytes.
* **B – Archiver**: pure core in `core.archive.planArchive` plus filesystem
  adapter emitting commit events.
* **C – Uploader**: represented by `WorkManagerStub` that enqueues unique work
  items when commits occur.

Run tests with:

```bash
kotlinc \
  android-app/src/main/kotlin/core/archive/ArchiveCore.kt \
  android-app/src/main/kotlin/edge/archive/fs/ArchiveFs.kt \
  android-app/src/main/kotlin/edge/upload/work/UploadWork.kt \
  android-app/src/test/kotlin/MorphTests.kt \
  -classpath /usr/share/java/kotlinx-coroutines-core-1.0.1.jar \
  -include-runtime -d android-app/app.jar
java -jar android-app/app.jar
```

The test prints a one-line morph‑grader report indicating that ordering,
 idempotency and canonicality checks pass.
