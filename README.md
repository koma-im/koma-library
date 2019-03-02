## Kotlin Matrix library

Implementation of matrix api in pure kotlin.

Moshi is used for json serialization and deserialization.
Codegen is used to avoid reflection, reducing package size
and improving performance.OkHttp is used for making
network requests. Retrofit provides a nice interface for
the api. `kotlinx-coroutines` are used to make the code
both non-blocing and easy to read. Exception-handling is
mostly done with
[Result](https://github.com/kittinunf/Result).
Combined they provide a lightweight and fast library.

## Usage

Builds are provided automatically by jitpack. If you are
using Gradle, add the following to `repositories`.

    maven {
        url = 'https://jitpack.io'
    }

Then in `dependencies`, add:

    compile 'io.github.koma-im:koma-library:0.7.6.5'

where `0.7.6.5` is a tag on Github.

## Projects based on koma

- [continuum](https://github.com/koma-im/continuum-desktop): Desktop
  client built with tornadofx
- [userbots](https://github.com/koma-im/kotlin-matrix-userbots): Bots
  that use the user api
