package io.github.gonbei774.calisthenicsmemory.ui.theme

import androidx.compose.ui.graphics.Color

// Fallback palette (used below Android 12 or when dynamic colour is off): deep teal primary, orange accent.
// The accent is reserved for what the owner asked to stand out: worked muscles and earned stars (plan §6).
val Teal10 = Color(0xFF00201E)
val Teal20 = Color(0xFF003734)
val Teal30 = Color(0xFF00504C)
val Teal40 = Color(0xFF00696A)
val Teal80 = Color(0xFF4FDAD2)
val Teal90 = Color(0xFF6FF7EE)
val Teal95 = Color(0xFFC9F2EF)

val Slate10 = Color(0xFF111827)
val Slate20 = Color(0xFF1F2937)
val Slate90 = Color(0xFFE5E7EB)
val Slate95 = Color(0xFFF3F4F6)
val Slate99 = Color(0xFFFAFBFC)

val Accent = Color(0xFFE8590C)          // orange-red: primary muscles, filled stars
val AccentLight = Color(0xFFFFB27A)     // secondary muscles
val AccentOnDark = Color(0xFFFF8A3D)
val ErrorLight = Color(0xFFBA1A1A)
val ErrorDark = Color(0xFFFFB4AB)

/** Text on the accent colour: near-black, because white on orange is only about 3.7:1. */
val ACCENT_TEXT_LIGHT = Color(0xFF1A1A1A)
val ACCENT_TEXT_DARK = Color(0xFF1A1A1A)

// ---- Direction B tokens (docs/17-ui-direction-b.md §1). Dynamic colour is off; these are the only colours.
val BgDark = Color(0xFF111416);        val BgLight = Color(0xFFF6F7F8)
val SurfaceDark = Color(0xFF1B2024);   val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceHighDark = Color(0xFF2E353A); val SurfaceHighLight = Color(0xFFE6E9EC)
val DividerDark = Color(0xFF252B30);   val DividerLight = Color(0xFFDDE2E5)
val TextDark = Color(0xFFECEFF1);      val TextLight = Color(0xFF111416)
val TextSecondaryDark = Color(0xFFC9D0D4); val TextSecondaryLight = Color(0xFF33393E)
val TextMutedDark = Color(0xFF9AA3A9); val TextMutedLight = Color(0xFF5E676D)
val Gold = Color(0xFFE9C046)           // primary fill, selected segment, timers, stars, active tab
val OnGold = Color(0xFF111416)
val GoldMarkOnLight = Color(0xFFB8901A) // doc 17 "accentText": thin marks and graphics on white (about 3:1, not for small text)
val GoldTextOnLight = Color(0xFF7A5C00) // gold as TEXT on white, kept at 4.5:1 or more (deviation from doc 17, see progress notes V08b)
val GoldDimDark = Color(0xFF8A7A3E);   val GoldDimLight = Color(0xFFD8C88A)   // accentDim: secondary muscles
val CautionDarkSurface = Color(0xFF2A2410); val CautionDarkBorder = Color(0xFF5C4A12); val CautionDarkText = Color(0xFFF3E3B0)
val CautionLightSurface = Color(0xFFFFF4E5); val CautionLightBorder = Color(0xFFE8C98A); val CautionLightText = Color(0xFF5C3A00)
