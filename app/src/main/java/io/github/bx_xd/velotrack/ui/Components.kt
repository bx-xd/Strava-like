package io.github.bx_xd.velotrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Stat card ─────────────────────────────────────────────────────
@Composable
fun StatCard(
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary
) {
    Surface(
        modifier  = modifier,
        shape     = RoundedCornerShape(14.dp),
        color     = BgCard,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text       = value,
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color      = valueColor,
                    letterSpacing = 0.5.sp
                )
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text     = unit,
                        fontSize = 13.sp,
                        color    = TextMuted
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text          = label.uppercase(),
                fontSize      = 10.sp,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 1.sp,
                color         = TextMuted
            )
        }
    }
}

// ── HUD metric (compact, for record screen) ───────────────────────
@Composable
fun HudStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary
) {
    Column(
        modifier          = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text       = value,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = valueColor,
            letterSpacing = 0.5.sp,
            textAlign  = TextAlign.Center,
            maxLines   = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text          = label.uppercase(),
            fontSize      = 9.sp,
            fontWeight    = FontWeight.Medium,
            letterSpacing = 0.5.sp,
            color         = TextMuted,
            textAlign     = TextAlign.Center
        )
    }
}

// ── Section title ─────────────────────────────────────────────────
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text          = text.uppercase(),
        modifier      = modifier,
        fontSize      = 12.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 2.sp,
        color         = TextMuted
    )
}

// ── Primary button ────────────────────────────────────────────────
@Composable
fun VeloButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = AccentOrange,
    contentColor: Color = Color.White,
    enabled: Boolean = true
) {
    Button(
        onClick  = onClick,
        modifier = modifier.height(52.dp),
        enabled  = enabled,
        shape    = RoundedCornerShape(14.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor   = contentColor
        )
    ) {
        Text(
            text          = text,
            fontSize      = 16.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

// ── GPS dot indicator ─────────────────────────────────────────────
@Composable
fun GpsDot(available: Boolean, accuracy: Float) {
    val color = when {
        !available       -> TextMuted
        accuracy < 20f   -> GreenGps
        else             -> YellowPause
    }
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, shape = RoundedCornerShape(50))
    )
}
