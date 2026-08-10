# ML Kit on-device models are loaded via reflection; keep their entry points.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_bundled_latin.** { *; }

# Room-generated code
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**

# Keep Parcelable / data classes used across process boundaries (overlay <-> app)
-keepclassmembers class com.screenlens.app.** implements android.os.Parcelable {
    static ** CREATOR;
}
