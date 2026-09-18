# ============================================================================
# Rinde — ProGuard & R8 Optimization and Obfuscation Rules
# ============================================================================

# 1. Preservar atributos para trazabilidad y reflection
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes SourceFile, LineNumberTable

# 2. Firebase Firestore & Realtime Database DTOs
# IMPRESCINDIBLE: Firestore deserializa usando JavaBeans / Reflection.
# Si R8 ofusca los campos, 'doc.toObject(Dto::class.java)' devuelve null o valores vacíos.
-keepclassmembers class com.farbalapps.rinde.data.remote.model.** {
    <fields>;
    public <init>();
    public <init>(...);
}
-keep class com.farbalapps.rinde.data.remote.model.** { *; }

# Firebase Common & Annotations
-keepattributes com.google.firebase.firestore.ServerTimestamp
-keepattributes com.google.firebase.firestore.PropertyName
-keepattributes com.google.firebase.firestore.Exclude
-keepattributes com.google.firebase.firestore.IgnoreExtraProperties

# 3. Modelos de Dominio y Datos (Mappers y Serialización)
-keepclassmembers class com.farbalapps.rinde.domain.model.** {
    <fields>;
    public <init>(...);
}
-keep class com.farbalapps.rinde.domain.model.** { *; }

# 4. Room Database & Entities
-keepclassmembers class com.farbalapps.rinde.data.local.entity.** {
    <fields>;
    public <init>(...);
}
-keep class com.farbalapps.rinde.data.local.entity.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# 5. WorkManager Workers
# WorkManager requiere instanciar workers vía reflection
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# 6. OkHttp & Retrofit
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes *Annotation*
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}

# 7. Gson & Kotlinx Serialization
-keepattributes EnclosingMethod
-keepclassmembers enum * { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# 8. Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**