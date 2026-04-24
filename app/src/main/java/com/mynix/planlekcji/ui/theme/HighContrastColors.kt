package com.mynix.planlekcji.ui.theme

import androidx.compose.ui.graphics.Color

object HighContrastColors {
    val DarkBlue = Color(0xFF1A3A5C)
    
    val AllColors = listOf(
        Color(0xFFF5A623), // biologia - pomarańcz
        Color(0xFF8BC34A), // biznes - limonka
        Color(0xFF1565C0), // chemia - głęboki niebieski
        Color(0xFF1E88E5), // bezpieczeństwo - jasny niebieski
        Color(0xFFD81B60), // fizyka - magenta
        Color(0xFFE64A19), // geografia - głęboka pomarańcz
        Color(0xFF26C6DA), // wychowawcza - turkus
        Color(0xFF1976D2), // historia - niebieski
        Color(0xFFE53935), // informatyka - czerwony
        Color(0xFFF4511E), // angielski - pomarańcz-czerwony
        Color(0xFF43A047), // niemiecki - zielony
        Color(0xFF5E35B1), // polski - fiolet
        Color(0xFFFFA726), // plastyka - amber
        Color(0xFFEC407A), // religia - róż
        Color(0xFFFFB300), // WF - żółty
        Color(0xFF607D8B), // fallback - blue-grey
        // Additional colors:
        Color(0xFF00897B), // teal ciemny
        Color(0xFF6D4C41), // brązowy
        Color(0xFF546E7A), // stalowy niebieski
        Color(0xFF00ACC1), // cyan
        Color(0xFF7B1FA2), // fiolet ciemny
        Color(0xFF558B2F), // oliwkowy zielony
        DarkBlue
    )

    fun getSubjectColor(subject: String): Int {
        return when {
            subject.contains("biologi",          ignoreCase=true) -> 0xFFF5A623.toInt()
            subject.contains("biznes",           ignoreCase=true) -> 0xFF8BC34A.toInt()
            subject.contains("chemi",            ignoreCase=true) -> 0xFF1565C0.toInt()
            subject.contains("bezpieczeńst",     ignoreCase=true) -> 0xFF1E88E5.toInt()
            subject.contains("fizyk",            ignoreCase=true) -> 0xFFD81B60.toInt()
            subject.contains("geografi",         ignoreCase=true) -> 0xFFE64A19.toInt()
            subject.contains("wychowawcz",       ignoreCase=true) -> 0xFF26C6DA.toInt()
            subject.contains("histori",          ignoreCase=true) -> 0xFF1976D2.toInt()
            subject.contains("informatyk",       ignoreCase=true) -> 0xFFE53935.toInt()
            subject.contains("angielski",        ignoreCase=true) -> 0xFFF4511E.toInt()
            subject.contains("niemiecki",        ignoreCase=true) -> 0xFF43A047.toInt()
            subject.contains("polski",           ignoreCase=true) -> 0xFF5E35B1.toInt()
            subject.contains("matematyk",        ignoreCase=true) -> 0xFFD81B60.toInt()
            subject.contains("plastyk",          ignoreCase=true) -> 0xFFFFA726.toInt()
            subject.contains("religi",           ignoreCase=true) -> 0xFFEC407A.toInt()
            subject.contains("wychowanie fizycz",ignoreCase=true) -> 0xFFFFB300.toInt()
            else                                                   -> 0xFF607D8B.toInt()
        }
    }
}
