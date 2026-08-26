package com.example.model

enum class WidgetType(val arabicTitle: String, val iconRes: String) {
    CLOCK("الساعة", "access_time"),
    SPEEDOMETER("السرعة", "speed"),
    DATE("التاريخ", "calendar_today"),
    GPS("الموقع والـ GPS", "gps_fixed"),
    MUSIC("مشغل الموسيقى", "music_note"),
    MAP("الخريطة", "map"),
    TRIP("كمبيوتر الرحلة", "directions_car"),
    APPS("التطبيقات والمفضلة", "apps"),
    CONTROLS("التحكم السريع", "tune")
}

enum class WidgetStyle(val type: WidgetType, val arabicName: String, val description: String) {
    // Clock Styles (7 styles)
    CLOCK_DIGITAL_LARGE(WidgetType.CLOCK, "ساعة رقمية كبيرة", "أرقام واضحة وكبيرة للرؤية السريعة"),
    CLOCK_WITH_DATE(WidgetType.CLOCK, "ساعة مع التاريخ", "عرض متوازن للوقت والتاريخ"),
    CLOCK_WITH_SECONDS(WidgetType.CLOCK, "ساعة مع الثواني", "تحديث فوري بالثواني"),
    CLOCK_MINIMAL(WidgetType.CLOCK, "تصميم مقتضب (Minimal)", "خطوط ناعمة وبدون إطارات"),
    CLOCK_CARD(WidgetType.CLOCK, "ساعة داخل بطاقة", "تصميم بطاقة كربونية أنيقة"),
    CLOCK_AUTOMOTIVE_LARGE(WidgetType.CLOCK, "تصميم لوحة القيادة (Automotive)", "مظهر شاشة سيارة فاخرة"),
    CLOCK_DAY_DATE(WidgetType.CLOCK, "ساعة + اليوم + التاريخ", "عرض شامل لليوم والتاريخ الميلادي/الهجري"),

    // Speedometer Styles (6 styles)
    SPEED_DIGITAL_LARGE(WidgetType.SPEEDOMETER, "رقمية كبيرة", "عرض رقمي عريض وواضح"),
    SPEED_GAUGE_CIRCULAR(WidgetType.SPEEDOMETER, "عداد دائري", "قرص دائري بمؤشر متحرك"),
    SPEED_GAUGE_SEMI(WidgetType.SPEEDOMETER, "عداد نصف دائري", "قوس رياضي أنيق للسرعة"),
    SPEED_WITH_UNIT(WidgetType.SPEEDOMETER, "سرعة + كم/س", "عرض السرعة مع الوحدة ومؤشر الحالة"),
    SPEED_WITH_AVG(WidgetType.SPEEDOMETER, "سرعة + متوسط السرعة", "عرض السرعة الحالية والمتوسط"),
    SPEED_DASHBOARD(WidgetType.SPEEDOMETER, "تصميم Dashboard", "لوحة قيادة رياضية متكاملة"),

    // Date Styles (5 styles)
    DATE_ONLY(WidgetType.DATE, "التاريخ فقط", "عرض يومي بسيط"),
    DATE_DAY_DATE(WidgetType.DATE, "اليوم + التاريخ", "اسم اليوم وتاريخ اليوم بالتفصيل"),
    DATE_HIJRI_GREGORIAN(WidgetType.DATE, "التاريخ + الهجري", "عرض التقويم الهجري والميلادي معاً"),
    DATE_CARD(WidgetType.DATE, "بطاقة التقويم", "تصميم بطاقة رياضية للتاريخ"),
    DATE_MINIMAL(WidgetType.DATE, "تصميم مقتضب", "عرض نصي خفيف وأنيق"),

    // GPS Styles (5 styles)
    GPS_INDICATOR_MINI(WidgetType.GPS, "مؤشر صغير", "رمز حالة الأقمار والاتصال"),
    GPS_CARD(WidgetType.GPS, "بطاقة GPS", "تفاصيل الإشارة والارتفاع والاتجاه"),
    GPS_WITH_SPEED(WidgetType.GPS, "GPS + السرعة", "بيانات الموقع والسرعة الفضائية"),
    GPS_COORDINATES(WidgetType.GPS, "GPS + الإحداثيات", "خطوط الطول والعرض بدقة"),
    GPS_ACCURACY(WidgetType.GPS, "GPS + دقة الموقع", "مستوى دقة التثبيت الفضائي"),

    // Music Styles (6 styles)
    MUSIC_MINI(WidgetType.MUSIC, "مشغل صغير (Mini)", "أزرار التشغيل والتخطي"),
    MUSIC_COMPACT(WidgetType.MUSIC, "مشغل مدمج", "اسم الأغنية وأزرار التحكم"),
    MUSIC_COVER(WidgetType.MUSIC, "مشغل + صورة الغلاف", "عرض الألبوم / القرص الدوار مع الأغنية"),
    MUSIC_CONTROLS(WidgetType.MUSIC, "مشغل + أزرار كاملة", "أزرار التحكم ومستوى الصوت"),
    MUSIC_LARGE_AUTOMOTIVE(WidgetType.MUSIC, "مشغل سيارة كبير", "واجهة لمسية عريضة للموسيقى"),
    MUSIC_MINIMAL(WidgetType.MUSIC, "مشغل خفيف (Minimal)", "تصميم بسيط وعصري"),

    // Map Styles (6 styles)
    MAP_MINI(WidgetType.MAP, "خريطة صغيرة", "معاينة مصغرة للمسار والموقع"),
    MAP_MEDIUM(WidgetType.MAP, "خريطة متوسطة", "خريطة تفاعلية مع أدوات التكبير"),
    MAP_WITH_SPEED(WidgetType.MAP, "خريطة + عداد السرعة", "خريطة ملاحة مدمجة مع HUD السرعة"),
    MAP_WITH_GPS(WidgetType.MAP, "خريطة + بيانات GPS", "خريطة مع إحداثيات والبوصلة"),
    MAP_WITH_TRIP(WidgetType.MAP, "خريطة + معلومات الرحلة", "خريطة مع المسافة والزمن"),
    MAP_LARGE(WidgetType.MAP, "خريطة كبيرة", "عرض واسع للطريق والموقع الحالي"),

    // Trip Styles (5 styles)
    TRIP_SPEED_DISTANCE(WidgetType.TRIP, "سرعة + مسافة", "السرعة والمسافة المقطوعة بالكيلومتر"),
    TRIP_SPEED_DURATION(WidgetType.TRIP, "سرعة + مدة", "السرعة وزمن القيادة الحالي"),
    TRIP_DASHBOARD(WidgetType.TRIP, "لوحة معلومات الرحلة", "متوسط وأعلى سرعة وزمن التوقف"),
    TRIP_CARD(WidgetType.TRIP, "بطاقة الرحلة", "بطاقة مدمجة لبيانات الرحلة"),
    TRIP_FULL_METRICS(WidgetType.TRIP, "معلومات الرحلة الكاملة", "تقرير شامل للمسافة والسرعات وأزرار التحكم"),

    // Apps Styles (7 styles)
    APPS_ICONS_ONLY(WidgetType.APPS, "أيقونات فقط", "شبكة أيقونات بدون نصوص"),
    APPS_ICONS_LABELS(WidgetType.APPS, "أيقونات + أسماء", "أيقونات كبيرة مع اسم التطبيق"),
    APPS_GRID_2X2(WidgetType.APPS, "شبكة 2 × 2", "أربعة تطبيقات مفضلة بحجم كبير"),
    APPS_GRID_3X2(WidgetType.APPS, "شبكة 3 × 2", "ستة تطبيقات رئيسية"),
    APPS_GRID_4X2(WidgetType.APPS, "شبكة 4 × 2", "ثمانية تطبيقات في شبكة عريضة"),
    APPS_FAVORITES_CARD(WidgetType.APPS, "بطاقة المفضلة", "بطاقة موحدة للتطبيقات السريعة"),
    APPS_HORIZONTAL_DOCK(WidgetType.APPS, "شريط أفقي", "شريط تمرير سريع للتطبيقات"),

    // Quick Controls Styles (5 styles)
    CONTROLS_CIRCULAR(WidgetType.CONTROLS, "أزرار دائرية", "أزرار صوت وموسيقى دائرية"),
    CONTROLS_SQUARE(WidgetType.CONTROLS, "أزرار مربعة", "أزرار عريضة سهلة اللمس"),
    CONTROLS_HORIZONTAL_BAR(WidgetType.CONTROLS, "شريط أفقي", "شريط أدوات سريع ومدمج"),
    CONTROLS_CARD(WidgetType.CONTROLS, "بطاقة التحكم السريع", "بطاقة مجمعة لمستوى الصوت والوسائط"),
    CONTROLS_LARGE_AUTOMOTIVE(WidgetType.CONTROLS, "أزرار سيارة كبيرة", "أزرار لمس عملاقة للقيادة الآمنة")
}

data class WidgetItem(
    val id: String,
    val type: WidgetType,
    val style: WidgetStyle,
    val spanX: Int = 1, // 1 to 4 columns in 1024x600 layout
    val spanY: Int = 1, // 1 to 2 rows
    val isVisible: Boolean = true,
    val order: Int = 0
) {
    companion object {
        fun createDefaultList(): List<WidgetItem> {
            return listOf(
                WidgetItem(
                    id = "widget_speed",
                    type = WidgetType.SPEEDOMETER,
                    style = WidgetStyle.SPEED_GAUGE_CIRCULAR,
                    spanX = 1,
                    spanY = 1,
                    order = 0
                ),
                WidgetItem(
                    id = "widget_clock",
                    type = WidgetType.CLOCK,
                    style = WidgetStyle.CLOCK_AUTOMOTIVE_LARGE,
                    spanX = 1,
                    spanY = 1,
                    order = 1
                ),
                WidgetItem(
                    id = "widget_music",
                    type = WidgetType.MUSIC,
                    style = WidgetStyle.MUSIC_COVER,
                    spanX = 1,
                    spanY = 1,
                    order = 2
                ),
                WidgetItem(
                    id = "widget_trip",
                    type = WidgetType.TRIP,
                    style = WidgetStyle.TRIP_CARD,
                    spanX = 1,
                    spanY = 1,
                    order = 3
                ),
                WidgetItem(
                    id = "widget_map",
                    type = WidgetType.MAP,
                    style = WidgetStyle.MAP_MEDIUM,
                    spanX = 2,
                    spanY = 1,
                    order = 4
                ),
                WidgetItem(
                    id = "widget_apps",
                    type = WidgetType.APPS,
                    style = WidgetStyle.APPS_HORIZONTAL_DOCK,
                    spanX = 2,
                    spanY = 1,
                    order = 5
                )
            )
        }
    }
}
