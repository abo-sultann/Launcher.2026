package com.example.model

enum class ComponentStatus(val arabicLabel: String) {
    RUNNING("يعمل بشكل ممتاز"),
    NOT_STARTED("لم يتم تشغيله بعد"),
    ERROR("حدث خطأ / معزول")
}

data class ComponentHealth(
    val nameArabic: String,
    val status: ComponentStatus,
    val details: String,
    val isCritical: Boolean = false
)

data class DiagnosticReport(
    val homeStatus: ComponentHealth = ComponentHealth("الشاشة الرئيسية", ComponentStatus.RUNNING, "تعمل بكفاءة عالية وبدون مشاكل"),
    val gpsStatus: ComponentHealth = ComponentHealth("نظام تحديد المواقع GPS", ComponentStatus.RUNNING, "تم تهيئة مزود الموقع الفضائي"),
    val musicStatus: ComponentHealth = ComponentHealth("مشغل الموسيقى والوسائط", ComponentStatus.RUNNING, "المشغل جاهز ومتاح للقراءة"),
    val mapsStatus: ComponentHealth = ComponentHealth("نظام الخرائط غير المتصلة", ComponentStatus.RUNNING, "محرك الخرائط يعمل بنمط العزل"),
    val databaseStatus: ComponentHealth = ComponentHealth("قاعدة البيانات والتخزين", ComponentStatus.RUNNING, "التخزين المحلي متصل وقابل للكتابة"),
    val widgetsStatus: ComponentHealth = ComponentHealth("محرك الودجات (Widgets)", ComponentStatus.RUNNING, "تم تحميل 9 فئات و 52 نمطاً"),
    val storageStatus: ComponentHealth = ComponentHealth("الوصول لذاكرة التخزين / USB", ComponentStatus.RUNNING, "صلاحيات الوصول للوسائط جاهزة"),
    val crashCount: Int = 0,
    val lastCrashMessage: String? = null
)
