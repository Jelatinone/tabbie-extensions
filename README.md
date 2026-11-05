# tabbie-extensions

Tabbie's first-party content families and distributions, written as any extension is: against the published
[tabbie-sdk](https://github.com/Jelatinone/tabbie-sdk), with its processors checking every contribution.

## Building

The extensions use the SDK through its published builds. For a local snapshot of the SDK, publish it first:

```bash
cd ../sdk && ./gradlew publishToMavenLocal
```

Then build the extensions:

```bash
./gradlew build
```
