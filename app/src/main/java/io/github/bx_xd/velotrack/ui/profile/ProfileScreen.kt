package io.github.bx_xd.velotrack.ui.profile

import android.app.Application
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.github.bx_xd.velotrack.data.VeloDatabase
import io.github.bx_xd.velotrack.model.UserProfile
import io.github.bx_xd.velotrack.ui.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val db = VeloDatabase.getInstance(app)
    val profile: StateFlow<UserProfile> = db.profileDao().getFlow()
        .map { it ?: UserProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    fun save(profile: UserProfile) {
        viewModelScope.launch { db.profileDao().save(profile) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    var name       by remember(profile) { mutableStateOf(profile.name) }
    var weight     by remember(profile) { mutableStateOf(if (profile.weightKg > 0) profile.weightKg.toString() else "") }
    var bikeWeight by remember(profile) { mutableStateOf(if (profile.bikeWeightKg > 0) profile.bikeWeightKg.toString() else "") }
    var cda        by remember(profile) { mutableStateOf(profile.cda.toString()) }
    var crr        by remember(profile) { mutableStateOf(profile.crr.toString()) }
    var eff        by remember(profile) { mutableStateOf(profile.efficiency.toString()) }
    var saved      by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile header
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = BgCard,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🚴", fontSize = 48.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (profile.name.isNotBlank()) profile.name else "Mon Profil",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (profile.isConfigured) {
                    Text(
                        "${profile.weightKg}kg + ${profile.bikeWeightKg}kg vélo · CdA ${profile.cda}",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                } else {
                    Text(
                        "Configurer pour activer la puissance estimée",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
        }

        SectionTitle("Paramètres physiques")

        VeloTextField(value = name, onValueChange = { name = it }, label = "Nom / Pseudo")

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VeloTextField(
                value = weight,
                onValueChange = { weight = it },
                label = "Poids corps (kg)",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
            VeloTextField(
                value = bikeWeight,
                onValueChange = { bikeWeight = it },
                label = "Poids vélo (kg)",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
        }

        SectionTitle("Aérodynamisme")

        ProfileDropdown(
            value = cda,
            onValueChange = { cda = it },
            label = "Position / CdA",
            options = listOf(
                "0.32" to "Aéro / Drops (0.32)",
                "0.36" to "Route / Capot (0.36)",
                "0.42" to "Gravel / Mixte (0.42)",
                "0.50" to "VTT / Haute (0.50)",
                "0.55" to "Urbain / Droit (0.55)"
            )
        )

        ProfileDropdown(
            value = crr,
            onValueChange = { crr = it },
            label = "Résistance roulement (Crr)",
            options = listOf(
                "0.003" to "Pneu route tubeless (0.003)",
                "0.004" to "Pneu route standard (0.004)",
                "0.006" to "Pneu gravel route (0.006)",
                "0.010" to "Pneu gravel/VTT XC (0.010)",
                "0.015" to "Pneu VTT trail (0.015)"
            )
        )

        SectionTitle("Transmission")

        ProfileDropdown(
            value = eff,
            onValueChange = { eff = it },
            label = "Rendement transmission",
            options = listOf(
                "0.98" to "Propre / neuf (98%)",
                "0.97" to "Normal (97%)",
                "0.95" to "Usé / sale (95%)"
            )
        )

        // Physics explanation
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = BgCard2
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "Formule de puissance",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "P = (F_roulement + F_aéro_vent + F_pente + F_accélération) × v / η\n" +
                    "Le vent est récupéré depuis Open-Meteo (gratuit) toutes les 5min.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }
        }

        // Save button
        if (saved) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GreenGps.copy(alpha = 0.15f)
            ) {
                Text(
                    "✅ Profil enregistré",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    color = GreenGps,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        VeloButton(
            text = "💾  Enregistrer",
            onClick = {
                viewModel.save(UserProfile(
                    name        = name,
                    weightKg    = weight.toDoubleOrNull() ?: 75.0,
                    bikeWeightKg = bikeWeight.toDoubleOrNull() ?: 9.0,
                    cda         = cda.toDoubleOrNull() ?: 0.36,
                    crr         = crr.toDoubleOrNull() ?: 0.004,
                    efficiency  = eff.toDoubleOrNull() ?: 0.97
                ))
                saved = true
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeloTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label, color = TextMuted) },
        modifier      = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = AccentOrange,
            unfocusedBorderColor = BorderColor,
            focusedTextColor     = TextPrimary,
            unfocusedTextColor   = TextPrimary,
            cursorColor          = AccentOrange
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    options: List<Pair<String, String>>
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.find { it.first == value }?.second ?: value

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value         = currentLabel,
            onValueChange = {},
            readOnly      = true,
            label         = { Text(label, color = TextMuted) },
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier      = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = AccentOrange,
                unfocusedBorderColor = BorderColor,
                focusedTextColor     = TextPrimary,
                unfocusedTextColor   = TextPrimary
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (v, label) ->
                DropdownMenuItem(
                    text    = { Text(label, color = if (v == value) AccentOrange else TextPrimary) },
                    onClick = { onValueChange(v); expanded = false }
                )
            }
        }
    }
}
