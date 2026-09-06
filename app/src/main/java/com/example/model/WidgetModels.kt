package com.example.model

enum class WidgetType(val arabicTitle: String, val iconRes: String) {
    CLOCK("الساعة", "access_time"),
    SPEEDOMETER("السرعة", "speed"),
    DATE("التاريخ", "calendar_today"),
    GPS("الموقع والـ GPS", "gps_fixed"),
    MUSIC("مشغل الموسيقى", "music_note"),
    MAP("الخريطة", "map"),
    TRIP("رحلتي", "directions_car"),
    APPS("التطبيقات والمفضلة", "apps"),
    CONTROLS("التحكم السريع", "tune"),
    MAINTENANCE("الصيانة", "build")
}

enum class WidgetSurfaceStyle(val arabicName: String) {
    TRANSPARENT("شفاف"),
    GLASS("زجاجي"),
    CARD("بطاقة")
}

/**
 * Launcher 2.0 deliberately keeps widget colour decisions simple.
 *
 * The old editor exposed a collection of unrelated accent colours which made otherwise
 * different widgets look like variations of the same neon card. White and black are the two
 * useful choices on a car wallpaper; surface style is what creates the third, glass family.
 */
enum class WidgetTone(val arabicName: String, val argb: Int) {
    WHITE("أبيض", 0xFFF7F7F7.toInt()),
    BLACK("أسود", 0xFF101010.toInt());

    companion object {
        fun fromArgb(argb: Int?): WidgetTone = if (argb == BLACK.argb) BLACK else WHITE
    }
}

enum class WidgetSizePreset(val arabicName: String) {
    CONTENT("على المحتوى"),
    SMALL("صغير"),
    MEDIUM("متوسط"),
    LARGE("كبير"),
    WIDE("عريض")
}

enum class WidgetStyle(val type: WidgetType, val arabicName: String, val description: String) {
    CLOCK_DIGITAL_LARGE(WidgetType.CLOCK, "رقمية واضحة", "وقت فقط بأرقام واضحة"),
    CLOCK_WITH_DATE(WidgetType.CLOCK, "الوقت والتاريخ", "الوقت مع اليوم والتاريخ"),
    CLOCK_WITH_SECONDS(WidgetType.CLOCK, "مع الثواني", "وقت رقمي مع الثواني"),
    CLOCK_MINIMAL(WidgetType.CLOCK, "وقت فقط", "أخف شكل للساعة بدون عناصر زائدة"),
    CLOCK_CARD(WidgetType.CLOCK, "ساعة بطاقة", "وقت وتاريخ داخل بطاقة"),
    CLOCK_AUTOMOTIVE_LARGE(WidgetType.CLOCK, "لوحة قيادة", "وقت كبير بطابع شاشة السيارة"),
    CLOCK_DAY_DATE(WidgetType.CLOCK, "الوقت واليوم", "الوقت مع اسم اليوم والتاريخ"),

    SPEED_DIGITAL_LARGE(WidgetType.SPEEDOMETER, "رقم فقط", "السرعة بشكل رقمي نظيف"),
    SPEED_GAUGE_CIRCULAR(WidgetType.SPEEDOMETER, "عداد دائري", "قرص دائري للسرعة"),
    SPEED_GAUGE_SEMI(WidgetType.SPEEDOMETER, "عداد نصف دائري", "قوس رياضي للسرعة"),
    SPEED_WITH_UNIT(WidgetType.SPEEDOMETER, "السرعة والوحدة", "السرعة مع كم/س وحالة GPS"),
    SPEED_WITH_AVG(WidgetType.SPEEDOMETER, "السرعة والمتوسط", "الحالية ومتوسط الرحلة"),
    SPEED_DASHBOARD(WidgetType.SPEEDOMETER, "لوحة السرعة", "سرعة وأعلى سرعة في لوحة واحدة"),

    DATE_ONLY(WidgetType.DATE, "التاريخ فقط", "تاريخ اليوم فقط"),
    DATE_DAY_DATE(WidgetType.DATE, "اليوم والتاريخ", "اسم اليوم وتاريخ اليوم"),
    DATE_HIJRI_GREGORIAN(WidgetType.DATE, "هجري وميلادي", "التقويمان في ودجت واحد"),
    DATE_CARD(WidgetType.DATE, "بطاقة التاريخ", "تاريخ واضح داخل بطاقة"),
    DATE_MINIMAL(WidgetType.DATE, "تاريخ خفيف", "عرض نصي بلا ازدحام"),

    GPS_INDICATOR_MINI(WidgetType.GPS, "حالة GPS", "مؤشر صغير لجودة الإشارة"),
    GPS_CARD(WidgetType.GPS, "بيانات GPS", "الإشارة والارتفاع والاتجاه"),
    GPS_WITH_SPEED(WidgetType.GPS, "GPS والسرعة", "حالة GPS مع السرعة"),
    GPS_COORDINATES(WidgetType.GPS, "الإحداثيات", "خطوط الطول والعرض"),
    GPS_ACCURACY(WidgetType.GPS, "دقة الموقع", "دقة التثبيت وعدد الأقمار"),

    MUSIC_MINI(WidgetType.MUSIC, "أزرار عائمة", "اسم مختصر وتحكم سريع بلا إطار"),
    MUSIC_COMPACT(WidgetType.MUSIC, "شريط قيادة", "شريط أفقي منخفض يناسب شاشة السيارة"),
    MUSIC_COVER(WidgetType.MUSIC, "بطاقة الألبوم", "هوية بصرية واضحة مع المقطع والتحكم"),
    MUSIC_CONTROLS(WidgetType.MUSIC, "تحكم كامل", "التحكم والتقدم ومستوى الصوت"),
    MUSIC_LARGE_AUTOMOTIVE(WidgetType.MUSIC, "مشغل كبير", "واجهة موسيقى كبيرة للمس"),
    MUSIC_MINIMAL(WidgetType.MUSIC, "مشغل خفيف", "أقل عناصر ممكنة"),

    MAP_MINI(WidgetType.MAP, "وجهتي", "اتجاه ومسافة الهدف في مساحة صغيرة"),
    MAP_MEDIUM(WidgetType.MAP, "بطاقة الملاحة", "الموقع والاتجاه مع فتح الخريطة"),
    MAP_WITH_SPEED(WidgetType.MAP, "الملاحة والسرعة", "الملاحة مع السرعة الحالية"),
    MAP_WITH_GPS(WidgetType.MAP, "الملاحة وGPS", "الموقع والاتجاه وجودة GPS"),
    MAP_WITH_TRIP(WidgetType.MAP, "الملاحة والرحلة", "الملاحة مع مسافة الرحلة"),
    MAP_LARGE(WidgetType.MAP, "ملاحة كبيرة", "عرض أوسع لمعلومات الملاحة"),

    TRIP_SPEED_DISTANCE(WidgetType.TRIP, "السرعة والمسافة", "السرعة ومسافة الرحلة"),
    TRIP_SPEED_DURATION(WidgetType.TRIP, "السرعة والمدة", "السرعة ووقت الحركة"),
    TRIP_DASHBOARD(WidgetType.TRIP, "لوحة الرحلة", "المتوسط والأعلى والتوقف"),
    TRIP_CARD(WidgetType.TRIP, "رحلتي المختصرة", "المسافة والمدة والمتوسط"),
    TRIP_FULL_METRICS(WidgetType.TRIP, "رحلتي الكاملة", "جميع بيانات الرحلة والتحكم"),

    APPS_ICONS_ONLY(WidgetType.APPS, "أيقونات فقط", "بدون أسماء للحفاظ على نظافة الشاشة"),
    APPS_ICONS_LABELS(WidgetType.APPS, "أيقونات وأسماء", "أيقونات مع أسماء التطبيقات"),
    APPS_GRID_2X2(WidgetType.APPS, "4 تطبيقات", "شبكة 2 × 2"),
    APPS_GRID_3X2(WidgetType.APPS, "6 تطبيقات", "شبكة 3 × 2"),
    APPS_GRID_4X2(WidgetType.APPS, "8 تطبيقات", "شبكة 4 × 2"),
    APPS_FAVORITES_CARD(WidgetType.APPS, "المفضلة", "مجموعة التطبيقات المفضلة"),
    APPS_HORIZONTAL_DOCK(WidgetType.APPS, "شريط التطبيقات", "شريط أفقي خفيف"),

    CONTROLS_CIRCULAR(WidgetType.CONTROLS, "أزرار دائرية", "تحكم دائري بالصوت والوسائط"),
    CONTROLS_SQUARE(WidgetType.CONTROLS, "أزرار مربعة", "أزرار كبيرة سهلة اللمس"),
    CONTROLS_HORIZONTAL_BAR(WidgetType.CONTROLS, "شريط تحكم", "شريط أفقي مدمج"),
    CONTROLS_CARD(WidgetType.CONTROLS, "بطاقة التحكم", "الصوت والوسائط في بطاقة"),
    CONTROLS_LARGE_AUTOMOTIVE(WidgetType.CONTROLS, "تحكم كبير", "أزرار لمس كبيرة أثناء القيادة"),

    MAINTENANCE_VERTICAL(WidgetType.MAINTENANCE, "شريط الصيانة", "ستة عدادات رأسية على حافة الشاشة"),
    MAINTENANCE_GRID(WidgetType.MAINTENANCE, "لوحة الصيانة", "شبكة 2 × 3 تعرض جميع عناصر الصيانة"),
    MAINTENANCE_ALERTS(WidgetType.MAINTENANCE, "الأقرب للصيانة", "ملخص ذكي لأقرب ثلاثة عناصر تحتاج انتباه")
}

/**
 * The complete enum stays readable for old saved layouts, but the rebuilt library offers only
 * genuinely different constructions per widget instead of a long list of near-duplicates.
 */
fun preferredWidgetStylesFor(type: WidgetType): List<WidgetStyle> = when (type) {
    WidgetType.CLOCK -> listOf(
        WidgetStyle.CLOCK_MINIMAL,
        WidgetStyle.CLOCK_WITH_DATE,
        WidgetStyle.CLOCK_AUTOMOTIVE_LARGE
    )
    WidgetType.SPEEDOMETER -> listOf(
        WidgetStyle.SPEED_DIGITAL_LARGE,
        WidgetStyle.SPEED_GAUGE_CIRCULAR,
        WidgetStyle.SPEED_DASHBOARD
    )
    WidgetType.DATE -> listOf(
        WidgetStyle.DATE_ONLY,
        WidgetStyle.DATE_DAY_DATE,
        WidgetStyle.DATE_HIJRI_GREGORIAN
    )
    WidgetType.GPS -> listOf(
        WidgetStyle.GPS_INDICATOR_MINI,
        WidgetStyle.GPS_COORDINATES,
        WidgetStyle.GPS_CARD
    )
    WidgetType.MUSIC -> listOf(
        WidgetStyle.MUSIC_MINIMAL,
        WidgetStyle.MUSIC_COMPACT,
        WidgetStyle.MUSIC_COVER
    )
    WidgetType.MAP -> listOf(
        WidgetStyle.MAP_MINI,
        WidgetStyle.MAP_WITH_GPS,
        WidgetStyle.MAP_LARGE
    )
    WidgetType.TRIP -> listOf(
        WidgetStyle.TRIP_SPEED_DISTANCE,
        WidgetStyle.TRIP_CARD,
        WidgetStyle.TRIP_DASHBOARD
    )
    WidgetType.APPS -> listOf(
        WidgetStyle.APPS_ICONS_ONLY,
        WidgetStyle.APPS_HORIZONTAL_DOCK,
        WidgetStyle.APPS_GRID_2X2
    )
    WidgetType.CONTROLS -> listOf(
        WidgetStyle.CONTROLS_CIRCULAR,
        WidgetStyle.CONTROLS_HORIZONTAL_BAR,
        WidgetStyle.CONTROLS_LARGE_AUTOMOTIVE
    )
    WidgetType.MAINTENANCE -> listOf(
        WidgetStyle.MAINTENANCE_VERTICAL,
        WidgetStyle.MAINTENANCE_GRID,
        WidgetStyle.MAINTENANCE_ALERTS
    )
}

/** Maps every 1.x near-duplicate to the closest rebuilt construction. */
fun modernWidgetStyle(style: WidgetStyle): WidgetStyle {
    if (style in preferredWidgetStylesFor(style.type)) return style
    return when (style.type) {
        WidgetType.CLOCK -> when (style) {
            WidgetStyle.CLOCK_AUTOMOTIVE_LARGE -> WidgetStyle.CLOCK_AUTOMOTIVE_LARGE
            WidgetStyle.CLOCK_DIGITAL_LARGE, WidgetStyle.CLOCK_MINIMAL -> WidgetStyle.CLOCK_MINIMAL
            else -> WidgetStyle.CLOCK_WITH_DATE
        }
        WidgetType.SPEEDOMETER -> when (style) {
            WidgetStyle.SPEED_DIGITAL_LARGE, WidgetStyle.SPEED_WITH_UNIT -> WidgetStyle.SPEED_DIGITAL_LARGE
            WidgetStyle.SPEED_WITH_AVG, WidgetStyle.SPEED_DASHBOARD -> WidgetStyle.SPEED_DASHBOARD
            else -> WidgetStyle.SPEED_GAUGE_CIRCULAR
        }
        WidgetType.DATE -> when (style) {
            WidgetStyle.DATE_ONLY, WidgetStyle.DATE_MINIMAL -> WidgetStyle.DATE_ONLY
            WidgetStyle.DATE_HIJRI_GREGORIAN -> WidgetStyle.DATE_HIJRI_GREGORIAN
            else -> WidgetStyle.DATE_DAY_DATE
        }
        WidgetType.GPS -> when (style) {
            WidgetStyle.GPS_INDICATOR_MINI -> WidgetStyle.GPS_INDICATOR_MINI
            WidgetStyle.GPS_COORDINATES -> WidgetStyle.GPS_COORDINATES
            else -> WidgetStyle.GPS_CARD
        }
        WidgetType.MUSIC -> when (style) {
            WidgetStyle.MUSIC_MINI, WidgetStyle.MUSIC_MINIMAL -> WidgetStyle.MUSIC_MINIMAL
            WidgetStyle.MUSIC_COVER, WidgetStyle.MUSIC_LARGE_AUTOMOTIVE -> WidgetStyle.MUSIC_COVER
            else -> WidgetStyle.MUSIC_COMPACT
        }
        WidgetType.MAP -> when (style) {
            WidgetStyle.MAP_MINI -> WidgetStyle.MAP_MINI
            WidgetStyle.MAP_LARGE, WidgetStyle.MAP_WITH_TRIP -> WidgetStyle.MAP_LARGE
            else -> WidgetStyle.MAP_WITH_GPS
        }
        WidgetType.TRIP -> when (style) {
            WidgetStyle.TRIP_SPEED_DISTANCE, WidgetStyle.TRIP_SPEED_DURATION -> WidgetStyle.TRIP_SPEED_DISTANCE
            WidgetStyle.TRIP_DASHBOARD, WidgetStyle.TRIP_FULL_METRICS -> WidgetStyle.TRIP_DASHBOARD
            else -> WidgetStyle.TRIP_CARD
        }
        WidgetType.APPS -> when (style) {
            WidgetStyle.APPS_GRID_2X2, WidgetStyle.APPS_GRID_3X2, WidgetStyle.APPS_GRID_4X2 -> WidgetStyle.APPS_GRID_2X2
            WidgetStyle.APPS_HORIZONTAL_DOCK -> WidgetStyle.APPS_HORIZONTAL_DOCK
            else -> WidgetStyle.APPS_ICONS_ONLY
        }
        WidgetType.CONTROLS -> when (style) {
            WidgetStyle.CONTROLS_CIRCULAR -> WidgetStyle.CONTROLS_CIRCULAR
            WidgetStyle.CONTROLS_LARGE_AUTOMOTIVE, WidgetStyle.CONTROLS_SQUARE -> WidgetStyle.CONTROLS_LARGE_AUTOMOTIVE
            else -> WidgetStyle.CONTROLS_HORIZONTAL_BAR
        }
        WidgetType.MAINTENANCE -> WidgetStyle.MAINTENANCE_VERTICAL
    }
}

data class WidgetItem(
    val id: String,
    val type: WidgetType,
    val style: WidgetStyle,
    val spanX: Int = 1,
    val spanY: Int = 1,
    val isVisible: Boolean = true,
    val order: Int = 0,
    val xFraction: Float = -1f,
    val yFraction: Float = -1f,
    val widthFraction: Float = 0f,
    val heightFraction: Float = 0f,
    val opacity: Float = 1f,
    val isLocked: Boolean = false,
    val zIndex: Int = 0,
    val surfaceStyle: WidgetSurfaceStyle = WidgetSurfaceStyle.TRANSPARENT,
    val showBorder: Boolean = false,
    val foregroundColorArgb: Int? = null,
    val accentColorArgb: Int? = null,
    val surfaceOpacity: Float = 1f
) {
    fun hasFreeGeometry(): Boolean = xFraction >= 0f && yFraction >= 0f && widthFraction > 0f && heightFraction > 0f

    companion object {
        fun defaultSurfaceFor(type: WidgetType): WidgetSurfaceStyle = when (type) {
            WidgetType.CLOCK, WidgetType.SPEEDOMETER, WidgetType.DATE, WidgetType.GPS -> WidgetSurfaceStyle.TRANSPARENT
            WidgetType.MUSIC, WidgetType.MAP, WidgetType.TRIP, WidgetType.APPS, WidgetType.CONTROLS, WidgetType.MAINTENANCE -> WidgetSurfaceStyle.GLASS
        }

        fun recommendedSize(type: WidgetType, preset: WidgetSizePreset): Pair<Float, Float> {
            if (type == WidgetType.MAINTENANCE) {
                return when (preset) {
                    WidgetSizePreset.CONTENT -> .15f to .92f
                    WidgetSizePreset.SMALL -> .13f to .70f
                    WidgetSizePreset.MEDIUM -> .15f to .82f
                    WidgetSizePreset.LARGE -> .17f to .94f
                    WidgetSizePreset.WIDE -> .22f to .94f
                }
            }
            return when (preset) {
                WidgetSizePreset.CONTENT -> when (type) {
                    WidgetType.CLOCK -> .18f to .12f
                    WidgetType.SPEEDOMETER -> .15f to .20f
                    WidgetType.DATE -> .22f to .11f
                    WidgetType.GPS -> .16f to .14f
                    WidgetType.MUSIC -> .27f to .15f
                    WidgetType.MAP -> .21f to .20f
                    WidgetType.TRIP -> .20f to .23f
                    WidgetType.APPS -> .30f to .16f
                    WidgetType.CONTROLS -> .24f to .15f
                    WidgetType.MAINTENANCE -> error("handled above")
                }
                WidgetSizePreset.SMALL -> when (type) {
                    WidgetType.CLOCK -> .23f to .15f
                    WidgetType.SPEEDOMETER -> .18f to .24f
                    WidgetType.DATE -> .26f to .14f
                    WidgetType.GPS -> .20f to .18f
                    WidgetType.MUSIC -> .30f to .18f
                    WidgetType.MAP -> .25f to .24f
                    WidgetType.TRIP -> .24f to .27f
                    WidgetType.APPS -> .34f to .19f
                    WidgetType.CONTROLS -> .29f to .18f
                    WidgetType.MAINTENANCE -> error("handled above")
                }
                WidgetSizePreset.MEDIUM -> .32f to .30f
                WidgetSizePreset.LARGE -> .43f to .42f
                WidgetSizePreset.WIDE -> when (type) {
                    WidgetType.CLOCK, WidgetType.DATE, WidgetType.GPS -> .42f to .16f
                    WidgetType.SPEEDOMETER -> .34f to .25f
                    WidgetType.MUSIC, WidgetType.APPS, WidgetType.CONTROLS -> .55f to .22f
                    WidgetType.MAP, WidgetType.TRIP -> .48f to .30f
                    WidgetType.MAINTENANCE -> error("handled above")
                }
            }
        }

        private fun legacyGeometryFor(order: Int, spanX: Int): FloatArray {
            val col = order % 4
            val row = order / 4
            val width = if (spanX > 1) 0.48f else 0.235f
            val x = if (spanX > 1) if (col >= 2) 0.505f else 0.01f else (0.01f + col * 0.2475f).coerceAtMost(0.755f)
            val y = 0.02f + row * 0.34f
            return floatArrayOf(x, y.coerceAtMost(0.72f), width, 0.30f)
        }

        fun withDefaultGeometry(item: WidgetItem): WidgetItem {
            if (item.hasFreeGeometry()) return item
            if (item.type == WidgetType.MAINTENANCE) {
                return item.copy(xFraction = .835f, yFraction = .025f, widthFraction = .15f, heightFraction = .92f, zIndex = item.order)
            }
            val g = legacyGeometryFor(item.order, item.spanX)
            return item.copy(xFraction = g[0], yFraction = g[1], widthFraction = g[2], heightFraction = g[3], zIndex = item.order)
        }

        fun createForOrder(id: String, type: WidgetType, style: WidgetStyle, order: Int, spanX: Int = 1): WidgetItem {
            val (w, h) = if (type == WidgetType.MAINTENANCE) {
                when (style) {
                    WidgetStyle.MAINTENANCE_GRID -> .40f to .42f
                    WidgetStyle.MAINTENANCE_ALERTS -> .36f to .28f
                    else -> recommendedSize(type, WidgetSizePreset.SMALL)
                }
            } else recommendedSize(type, WidgetSizePreset.SMALL)
            val x = if (type == WidgetType.MAINTENANCE) (1f - w - .015f) else (0.035f + (order % 3) * .31f).coerceAtMost((1f - w).coerceAtLeast(0f))
            val y = if (type == WidgetType.MAINTENANCE) .025f else (0.05f + (order / 3) * .36f).coerceAtMost((1f - h).coerceAtLeast(0f))
            return WidgetItem(
                id = id,
                type = type,
                style = style,
                spanX = spanX,
                spanY = 1,
                isVisible = true,
                order = order,
                xFraction = x,
                yFraction = y,
                widthFraction = w,
                heightFraction = h,
                opacity = 1f,
                isLocked = false,
                zIndex = order,
                surfaceStyle = defaultSurfaceFor(type),
                showBorder = false,
                foregroundColorArgb = WidgetTone.WHITE.argb,
                accentColorArgb = WidgetTone.WHITE.argb,
                surfaceOpacity = 1f
            )
        }

        fun createDefaultList(): List<WidgetItem> = listOf(
            WidgetItem(
                id = "widget_speed", type = WidgetType.SPEEDOMETER, style = WidgetStyle.SPEED_DIGITAL_LARGE,
                order = 0, xFraction = .025f, yFraction = .025f, widthFraction = .13f, heightFraction = .15f,
                zIndex = 0, surfaceStyle = WidgetSurfaceStyle.TRANSPARENT,
                foregroundColorArgb = WidgetTone.BLACK.argb, accentColorArgb = WidgetTone.BLACK.argb
            ),
            WidgetItem(
                id = "widget_clock", type = WidgetType.CLOCK, style = WidgetStyle.CLOCK_MINIMAL,
                order = 1, xFraction = .38f, yFraction = .015f, widthFraction = .24f, heightFraction = .13f,
                zIndex = 1, surfaceStyle = WidgetSurfaceStyle.TRANSPARENT,
                foregroundColorArgb = WidgetTone.WHITE.argb, accentColorArgb = WidgetTone.WHITE.argb
            ),
            WidgetItem(
                id = "widget_music", type = WidgetType.MUSIC, style = WidgetStyle.MUSIC_MINIMAL,
                order = 2, xFraction = .70f, yFraction = .035f, widthFraction = .27f, heightFraction = .16f,
                zIndex = 2, surfaceStyle = WidgetSurfaceStyle.GLASS,
                foregroundColorArgb = WidgetTone.WHITE.argb, accentColorArgb = WidgetTone.WHITE.argb
            ),
            WidgetItem(
                id = "widget_trip", type = WidgetType.TRIP, style = WidgetStyle.TRIP_CARD,
                order = 3, xFraction = .79f, yFraction = .36f, widthFraction = .19f, heightFraction = .27f,
                zIndex = 3, surfaceStyle = WidgetSurfaceStyle.GLASS,
                foregroundColorArgb = WidgetTone.WHITE.argb, accentColorArgb = WidgetTone.WHITE.argb
            ),
            WidgetItem(
                id = "widget_map", type = WidgetType.MAP, style = WidgetStyle.MAP_MINI,
                order = 4, xFraction = .025f, yFraction = .40f, widthFraction = .22f, heightFraction = .24f,
                zIndex = 4, surfaceStyle = WidgetSurfaceStyle.GLASS,
                foregroundColorArgb = WidgetTone.WHITE.argb, accentColorArgb = WidgetTone.WHITE.argb
            ),
            WidgetItem(
                id = "widget_apps", type = WidgetType.APPS, style = WidgetStyle.APPS_HORIZONTAL_DOCK,
                order = 5, xFraction = .31f, yFraction = .79f, widthFraction = .38f, heightFraction = .17f,
                zIndex = 5, surfaceStyle = WidgetSurfaceStyle.GLASS,
                foregroundColorArgb = WidgetTone.WHITE.argb, accentColorArgb = WidgetTone.WHITE.argb
            )
        )
    }
}
