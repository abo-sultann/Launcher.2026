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
    CLOCK_DIGITAL_LARGE(WidgetType.CLOCK, "ساعة رقمية كبيرة", "أرقام واضحة وكبيرة للرؤية السريعة"),
    CLOCK_WITH_DATE(WidgetType.CLOCK, "ساعة مع التاريخ", "عرض متوازن للوقت والتاريخ"),
    CLOCK_WITH_SECONDS(WidgetType.CLOCK, "ساعة مع الثواني", "تحديث فوري بالثواني"),
    CLOCK_MINIMAL(WidgetType.CLOCK, "تصميم مقتضب (Minimal)", "خطوط ناعمة وبدون إطارات"),
    CLOCK_CARD(WidgetType.CLOCK, "ساعة داخل بطاقة", "تصميم بطاقة كربونية أنيقة"),
    CLOCK_AUTOMOTIVE_LARGE(WidgetType.CLOCK, "تصميم لوحة القيادة (Automotive)", "مظهر شاشة سيارة فاخرة"),
    CLOCK_DAY_DATE(WidgetType.CLOCK, "ساعة + اليوم + التاريخ", "عرض شامل لليوم والتاريخ الميلادي/الهجري"),

    SPEED_DIGITAL_LARGE(WidgetType.SPEEDOMETER, "رقمية كبيرة", "عرض رقمي عريض وواضح"),
    SPEED_GAUGE_CIRCULAR(WidgetType.SPEEDOMETER, "عداد دائري", "قرص دائري بمؤشر متحرك"),
    SPEED_GAUGE_SEMI(WidgetType.SPEEDOMETER, "عداد نصف دائري", "قوس رياضي أنيق للسرعة"),
    SPEED_WITH_UNIT(WidgetType.SPEEDOMETER, "سرعة + كم/س", "عرض السرعة مع الوحدة ومؤشر الحالة"),
    SPEED_WITH_AVG(WidgetType.SPEEDOMETER, "سرعة + متوسط السرعة", "عرض السرعة الحالية والمتوسط"),
    SPEED_DASHBOARD(WidgetType.SPEEDOMETER, "تصميم Dashboard", "لوحة قيادة رياضية متكاملة"),

    DATE_ONLY(WidgetType.DATE, "التاريخ فقط", "عرض يومي بسيط"),
    DATE_DAY_DATE(WidgetType.DATE, "اليوم + التاريخ", "اسم اليوم وتاريخ اليوم بالتفصيل"),
    DATE_HIJRI_GREGORIAN(WidgetType.DATE, "التاريخ + الهجري", "عرض التقويم الهجري والميلادي معاً"),
    DATE_CARD(WidgetType.DATE, "بطاقة التقويم", "تصميم بطاقة رياضية للتاريخ"),
    DATE_MINIMAL(WidgetType.DATE, "تصميم مقتضب", "عرض نصي خفيف وأنيق"),

    GPS_INDICATOR_MINI(WidgetType.GPS, "مؤشر صغير", "رمز حالة الأقمار والاتصال"),
    GPS_CARD(WidgetType.GPS, "بطاقة GPS", "تفاصيل الإشارة والارتفاع والاتجاه"),
    GPS_WITH_SPEED(WidgetType.GPS, "GPS + السرعة", "بيانات الموقع والسرعة الفضائية"),
    GPS_COORDINATES(WidgetType.GPS, "GPS + الإحداثيات", "خطوط الطول والعرض بدقة"),
    GPS_ACCURACY(WidgetType.GPS, "GPS + دقة الموقع", "مستوى دقة التثبيت الفضائي"),

    MUSIC_MINI(WidgetType.MUSIC, "مشغل صغير (Mini)", "أزرار التشغيل والتخطي"),
    MUSIC_COMPACT(WidgetType.MUSIC, "مشغل مدمج", "اسم الأغنية وأزرار التحكم"),
    MUSIC_COVER(WidgetType.MUSIC, "مشغل + صورة الغلاف", "عرض الألبوم / القرص الدوار مع الأغنية"),
    MUSIC_CONTROLS(WidgetType.MUSIC, "مشغل + أزرار كاملة", "أزرار التحكم ومستوى الصوت"),
    MUSIC_LARGE_AUTOMOTIVE(WidgetType.MUSIC, "مشغل سيارة كبير", "واجهة لمسية عريضة للموسيقى"),
    MUSIC_MINIMAL(WidgetType.MUSIC, "مشغل خفيف (Minimal)", "تصميم بسيط وعصري"),

    MAP_MINI(WidgetType.MAP, "خريطة صغيرة", "معاينة مصغرة للمسار والموقع"),
    MAP_MEDIUM(WidgetType.MAP, "خريطة متوسطة", "خريطة تفاعلية مع أدوات التكبير"),
    MAP_WITH_SPEED(WidgetType.MAP, "خريطة + عداد السرعة", "خريطة ملاحة مدمجة مع HUD السرعة"),
    MAP_WITH_GPS(WidgetType.MAP, "خريطة + بيانات GPS", "خريطة مع إحداثيات والبوصلة"),
    MAP_WITH_TRIP(WidgetType.MAP, "خريطة + معلومات الرحلة", "خريطة مع المسافة والزمن"),
    MAP_LARGE(WidgetType.MAP, "خريطة كبيرة", "عرض واسع للطريق والموقع الحالي"),

    TRIP_SPEED_DISTANCE(WidgetType.TRIP, "سرعة + مسافة", "السرعة والمسافة المقطوعة بالكيلومتر"),
    TRIP_SPEED_DURATION(WidgetType.TRIP, "سرعة + مدة", "السرعة وزمن القيادة الحالي"),
    TRIP_DASHBOARD(WidgetType.TRIP, "لوحة معلومات الرحلة", "متوسط وأعلى سرعة وزمن التوقف"),
    TRIP_CARD(WidgetType.TRIP, "بطاقة الرحلة", "بطاقة مدمجة لبيانات الرحلة"),
    TRIP_FULL_METRICS(WidgetType.TRIP, "معلومات الرحلة الكاملة", "تقرير شامل للمسافة والسرعات وأزرار التحكم"),

    APPS_ICONS_ONLY(WidgetType.APPS, "أيقونات فقط", "شبكة أيقونات بدون نصوص"),
    APPS_ICONS_LABELS(WidgetType.APPS, "أيقونات + أسماء", "أيقونات كبيرة مع اسم التطبيق"),
    APPS_GRID_2X2(WidgetType.APPS, "شبكة 2 × 2", "أربعة تطبيقات مفضلة بحجم كبير"),
    APPS_GRID_3X2(WidgetType.APPS, "شبكة 3 × 2", "ستة تطبيقات رئيسية"),
    APPS_GRID_4X2(WidgetType.APPS, "شبكة 4 × 2", "ثمانية تطبيقات في شبكة عريضة"),
    APPS_FAVORITES_CARD(WidgetType.APPS, "بطاقة المفضلة", "بطاقة موحدة للتطبيقات السريعة"),
    APPS_HORIZONTAL_DOCK(WidgetType.APPS, "شريط أفقي", "شريط تمرير سريع للتطبيقات"),

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
    val spanX: Int = 1,
    val spanY: Int = 1,
    val isVisible: Boolean = true,
    val order: Int = 0,
    // Free-form geometry is normalized to the available home canvas (0f..1f),
    // so placement remains stable even if density/safe-area changes.
    val xFraction: Float = -1f,
    val yFraction: Float = -1f,
    val widthFraction: Float = 0f,
    val heightFraction: Float = 0f,
    val opacity: Float = 0.92f,
    val isLocked: Boolean = false,
    val zIndex: Int = 0
) {
    fun hasFreeGeometry(): Boolean = xFraction >= 0f && yFraction >= 0f && widthFraction > 0f && heightFraction > 0f

    companion object {
        private fun geometryFor(order: Int, spanX: Int): FloatArray {
            val col = order % 4
            val row = order / 4
            val width = if (spanX > 1) 0.48f else 0.235f
            val x = if (spanX > 1) {
                if (col >= 2) 0.505f else 0.01f
            } else {
                (0.01f + col * 0.2475f).coerceAtMost(0.755f)
            }
            val y = 0.02f + row * 0.34f
            return floatArrayOf(x, y.coerceAtMost(0.72f), width, 0.30f)
        }

        fun withDefaultGeometry(item: WidgetItem): WidgetItem {
            if (item.hasFreeGeometry()) return item
            val g = geometryFor(item.order, item.spanX)
            return item.copy(
                xFraction = g[0],
                yFraction = g[1],
                widthFraction = g[2],
                heightFraction = g[3],
                zIndex = item.order
            )
        }

        fun createForOrder(id: String, type: WidgetType, style: WidgetStyle, order: Int, spanX: Int = 1): WidgetItem {
            return withDefaultGeometry(
                WidgetItem(
                    id = id,
                    type = type,
                    style = style,
                    spanX = spanX,
                    spanY = 1,
                    isVisible = true,
                    order = order,
                    zIndex = order
                )
            )
        }

        fun createDefaultList(): List<WidgetItem> {
            return listOf(
                createForOrder("widget_speed", WidgetType.SPEEDOMETER, WidgetStyle.SPEED_GAUGE_CIRCULAR, 0),
                createForOrder("widget_clock", WidgetType.CLOCK, WidgetStyle.CLOCK_AUTOMOTIVE_LARGE, 1),
                createForOrder("widget_music", WidgetType.MUSIC, WidgetStyle.MUSIC_COVER, 2),
                createForOrder("widget_trip", WidgetType.TRIP, WidgetStyle.TRIP_CARD, 3),
                createForOrder("widget_map", WidgetType.MAP, WidgetStyle.MAP_MEDIUM, 4, spanX = 2),
                createForOrder("widget_apps", WidgetType.APPS, WidgetStyle.APPS_HORIZONTAL_DOCK, 6, spanX = 2)
            ).mapIndexed { index, item -> item.copy(order = index, zIndex = index) }
        }
    }
}
