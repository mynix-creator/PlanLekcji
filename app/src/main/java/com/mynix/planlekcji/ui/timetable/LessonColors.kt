package com.mynix.planlekcji.ui.timetable

import androidx.compose.ui.graphics.Color

fun subjectColor(subject: String): Color = when {
    subject.contains("biologi",           ignoreCase=true) -> Color(0xFFF1B35C)
    subject.contains("biznes",            ignoreCase=true) -> Color(0xFFB7C94F)
    subject.contains("chemi",             ignoreCase=true) -> Color(0xFF203C9F)
    subject.contains("doradztwo",         ignoreCase=true) -> Color(0xFFD80B36)
    subject.contains("bezpieczeńst",      ignoreCase=true) -> Color(0xFF294599)
    subject.contains("fizyk",             ignoreCase=true) -> Color(0xFFB61469)
    subject.contains("geografi",          ignoreCase=true) -> Color(0xFFE6661D)
    subject.contains("wychowawcz",        ignoreCase=true) -> Color(0xFF50B0C9)
    subject.contains("histori",           ignoreCase=true) -> Color(0xFF18399A)
    subject.contains("informatyk",        ignoreCase=true) -> Color(0xFFD52F1F)
    subject.contains("angielski",         ignoreCase=true) -> Color(0xFFFE5722)
    subject.contains("niemiecki",         ignoreCase=true) -> Color(0xFF50AC23)
    subject.contains("polski",            ignoreCase=true) -> Color(0xFF5431B3)
    subject.contains("matematyk",         ignoreCase=true) -> Color(0xFFA22F8A)
    subject.contains("plastyk",           ignoreCase=true) -> Color(0xFFEAA540)
    subject.contains("religi",            ignoreCase=true) -> Color(0xFFEA1E63)
    subject.contains("wychowanie fizycz", ignoreCase=true) -> Color(0xFFFF9700)
    else                                                   -> Color(0xFF607D8B)
}
