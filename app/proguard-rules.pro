# Room database rules
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep models and entities
-keep class com.expenseapp.salarytracker.data.entity.** { *; }
-keep class com.expenseapp.salarytracker.domain.model.** { *; }
