# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep the application class
-keep class com.n0tez.app.N0tezApplication { *; }

# Keep all activities
-keep class com.n0tez.app.** { *; }

# Keep data classes
-keep class com.n0tez.app.data.** { *; }

# Keep Android Security classes
-keep class androidx.security.crypto.** { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep Preference classes
-keep class androidx.preference.** { *; }

# General Android rules
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Don't warn about missing classes from optional dependencies
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
