# ProGuard Rules for Luleme App

# Room
-keep class androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt/Dagger
-keep class com.luleme.LulemeApplication
-keep class com.luleme.di.** { *; }
-keepnames class dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper$LayoutInflaterFactoryWrapper

# Data Classes (Gson serialization)
-keep class com.luleme.domain.model.** { *; }

# Compose
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.material3.** { *; }
