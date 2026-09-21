package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val DejaVuSans = FontFamily(
    Font(R.font.dejavu_sans, FontWeight.Normal)
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = DejaVuSans,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        color = ColorTagline
    )
)
