# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**

# Keep data/model classes used for Room + report generation (field names are
# not reflectively parsed anywhere else in the app, but keeping is cheap and
# avoids accidental breakage if reflection is added later).
-keep class com.phonedoctor.app.data.local.entity.** { *; }
-keep class com.phonedoctor.app.domain.model.** { *; }
