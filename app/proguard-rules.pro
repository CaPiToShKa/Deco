# Release builds are shrunk with R8. Libraries ship their own consumer rules
# (kotlinx.serialization, Ktor, Koin, SFMC SDK); these cover optional dependencies
# that are referenced but intentionally not packaged.
-dontwarn org.slf4j.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn java.lang.management.**

# DTOs are (de)serialized by generated serializers; keep their names readable in crash reports.
-keepnames class com.example.decosocio.api.** { *; }
