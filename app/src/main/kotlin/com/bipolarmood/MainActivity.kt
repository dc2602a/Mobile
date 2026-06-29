@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.bipolarmood

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Canvas
import com.bipolarmood.R
import com.bipolarmood.data.DiaryEntryEntity
import com.bipolarmood.data.ImpulseEntryEntity
import com.bipolarmood.data.MedicationEntity
import com.bipolarmood.data.MedicationIntakeEntity
import com.bipolarmood.data.MoodEntryEntity
import com.bipolarmood.data.ProfileEntity
import com.bipolarmood.data.SleepEntryEntity
import com.bipolarmood.data.TrustedPersonEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val application = context.applicationContext as Application
            val appViewModel: AppViewModel = viewModel(factory = AppViewModelFactory(application))
            BipolarMoodApp(appViewModel)
        }
    }
}

private enum class Screen(val title: String) {
    Insights("Обзор"),
    Diary("Дневник"),
    Medications("Лекарства"),
    Profile("Профиль");

    val icon
        @Composable get() = when (this) {
            Insights -> Icons.Filled.Timeline
            Diary -> Icons.Filled.Book
            Medications -> Icons.Filled.Medication
            Profile -> Icons.Filled.Person
        }
}

private fun parseScreen(name: String?): Screen = when (name) {
    "Home", "Mood", "Diary" -> Screen.Diary
    "Medications" -> Screen.Medications
    "Insights", "Graphs" -> Screen.Insights
    "Settings", "Profile" -> Screen.Profile
    else -> runCatching { Screen.valueOf(name ?: Screen.Diary.name) }.getOrDefault(Screen.Diary)
}

@Composable
private fun BipolarMoodApp(viewModel: AppViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val moodEntries by viewModel.moodEntries.collectAsStateWithLifecycle()
    val impulseEntries by viewModel.impulseEntries.collectAsStateWithLifecycle()
    val medications by viewModel.medications.collectAsStateWithLifecycle()
    val medicationIntakes by viewModel.medicationIntakes.collectAsStateWithLifecycle()
    val feedItems by viewModel.feedItems.collectAsStateWithLifecycle()
    val sleepEntries by viewModel.sleepEntries.collectAsStateWithLifecycle()
    val trustedPeople by viewModel.trustedPeople.collectAsStateWithLifecycle()

    var selectedScreenName by rememberSaveable { mutableStateOf(Screen.Diary.name) }
    var showSafetyPlan by rememberSaveable { mutableStateOf(false) }
    val screen = parseScreen(selectedScreenName)
    val hasMissedMeds = medications.any { it.missedReminders > 0 }

    BipolarMoodTheme(darkTheme = profile.darkTheme) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Screen.entries.forEach { item ->
                        val selected = item == screen
                        NavigationBarItem(
                            selected = selected,
                            onClick = { selectedScreenName = item.name },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    modifier = if (item == Screen.Diary && selected) Modifier.size(28.dp) else Modifier
                                )
                            },
                            label = {
                                Text(
                                    item.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (item == Screen.Diary) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        ) { padding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                color = MaterialTheme.colorScheme.background
            ) {
                when (screen) {
                    Screen.Diary -> MainDiaryScreen(
                        profile = profile,
                        feedItems = feedItems,
                        moods = moodEntries,
                        medications = medications,
                        onAddMood = viewModel::addMoodEntry,
                        onDeleteMood = viewModel::deleteMoodEntry,
                        onAddDiary = viewModel::addDiaryEntry,
                        onUpdateDiary = viewModel::updateDiaryEntry,
                        onDeleteDiary = viewModel::deleteDiaryEntry,
                        onOpenSafetyPlan = { showSafetyPlan = true },
                        hasMissedMeds = hasMissedMeds
                    )

                    Screen.Medications -> MedicationsScreen(
                        profile = profile,
                        medications = medications,
                        intakes = medicationIntakes,
                        onAdd = viewModel::addMedication,
                        onTaken = viewModel::markMedicationTaken,
                        onMissed = viewModel::registerMissedMedication,
                        onDelete = viewModel::deleteMedication
                    )

                    Screen.Insights -> InsightsScreen(
                        moods = moodEntries,
                        impulses = impulseEntries,
                        intakes = medicationIntakes,
                        sleepEntries = sleepEntries,
                        onAddMood = viewModel::addMoodEntry,
                        onDeleteMood = viewModel::deleteMoodEntry,
                        onAddSleep = viewModel::addSleepEntry,
                        onAddImpulse = viewModel::addImpulse,
                        onTrustedScore = viewModel::addTrustedImpulseScore,
                        onDeleteImpulse = viewModel::deleteImpulse,
                        trustedPeople = trustedPeople,
                        onOpenSafetyPlan = { showSafetyPlan = true }
                    )

                    Screen.Profile -> ProfileScreen(
                        profile = profile,
                        trustedPeople = trustedPeople,
                        exportCsv = viewModel::exportCsv,
                        onSaveProfile = viewModel::saveProfile,
                        onAddTrusted = viewModel::addTrustedPerson,
                        onRevokeTrusted = viewModel::revokeTrustedAccess,
                        onRestoreTrusted = viewModel::restoreTrustedAccess,
                        onDeleteTrusted = viewModel::deleteTrustedPerson,
                        onOpenSafetyPlan = { showSafetyPlan = true }
                    )
                }
            }
        }

        if (showSafetyPlan) {
            SafetyPlanDialog(profile = profile, onDismiss = { showSafetyPlan = false })
        }
    }
}

@Composable
private fun MainDiaryScreen(
    profile: ProfileEntity,
    feedItems: List<FeedItem>,
    moods: List<MoodEntryEntity>,
    medications: List<MedicationEntity>,
    onAddMood: (Double, String, List<String>, String, Long) -> Unit,
    onDeleteMood: (MoodEntryEntity) -> Unit,
    onAddDiary: (String, List<String>) -> Unit,
    onUpdateDiary: (DiaryEntryEntity, String, List<String>) -> Unit,
    onDeleteDiary: (DiaryEntryEntity) -> Unit,
    onOpenSafetyPlan: () -> Unit,
    hasMissedMeds: Boolean
) {
    var feedFilter by rememberSaveable { mutableStateOf(FeedFilter.All.name) }
    var showMoodDialog by rememberSaveable { mutableStateOf(false) }
    var showDiaryDialog by rememberSaveable { mutableStateOf(false) }
    var showSafetyHint by rememberSaveable { mutableStateOf(false) }
    var editEntry by remember { mutableStateOf<DiaryEntryEntity?>(null) }
    var selectedPhoto by rememberSaveable { mutableStateOf<String?>(null) }

    val filter = runCatching { FeedFilter.valueOf(feedFilter) }.getOrDefault(FeedFilter.All)
    val filteredFeed = remember(feedItems, filter) { feedItems.applyFilter(filter) }
    val latestMood = moods.firstOrNull()?.mood ?: 0.0

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Привет, ${profile.userName}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Барсик рисует в дневнике — мы рядом",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onOpenSafetyPlan,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(RedAccent.copy(alpha = if (hasMissedMeds) 0.35f else 0.18f))
                    ) {
                        Icon(Icons.Outlined.Favorite, contentDescription = "Safety Plan", tint = RedAccent)
                    }
                }
            }

            item {
                MascotSceneBanner(
                    imageRes = R.drawable.mascot_barsik_diary,
                    title = "Барсик ведёт дневник",
                    subtitle = "Запиши мысль или прикрепи фото — без спешки",
                    accentColor = TurquoiseAccent
                )
            }

            item {
                MoodSummaryChip(latestMood = latestMood)
            }

            item {
                ProactiveHint(
                    latestMood = latestMood,
                    hasRecentSleepIssue = moods.any { it.symptoms.contains("нарушение сна") }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = YellowAccent, contentColor = Color.Black),
                        onClick = { showMoodDialog = true }
                    ) {
                        Text("Состояние", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { showDiaryDialog = true }
                    ) {
                        Text("Заметка")
                    }
                }
            }

            item {
                if (moods.size >= 2) {
                    SectionTitle("Динамика")
                    MoodLineChart(entries = moods.take(12).reversed(), compact = true)
                }
            }

            item {
                SectionTitle("Лента")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeedFilter.entries.forEach { option ->
                        FilterChip(
                            selected = filter == option,
                            onClick = { feedFilter = option.name },
                            label = { Text(option.label) },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TurquoiseAccent.copy(alpha = 0.25f),
                                selectedLabelColor = TurquoiseAccent
                            )
                        )
                    }
                }
            }

            if (filteredFeed.isEmpty()) {
                item {
                    EmptyState(
                        when (filter) {
                            FeedFilter.Notes -> "Пока нет заметок. Нажми «Заметка», чтобы добавить."
                            FeedFilter.Moods -> "Пока нет состояний. Нажми «Состояние», чтобы зафиксировать."
                            FeedFilter.All -> "Лента пуста. Добавь заметку или зафиксируй состояние."
                        }
                    )
                }
            } else {
                items(filteredFeed, key = { it.sortKey }) { item ->
                    when (item) {
                        is FeedItem.Note -> DiaryFeedCard(
                            entry = item.entry,
                            onEdit = { editEntry = item.entry },
                            onDelete = { onDeleteDiary(item.entry) },
                            onPhotoClick = { selectedPhoto = it }
                        )
                        is FeedItem.Mood -> MoodFeedCard(
                            entry = item.entry,
                            onDelete = { onDeleteMood(item.entry) }
                        )
                    }
                }
            }
        }
    }

    if (showMoodDialog) {
        MoodEntryDialog(
            onDismiss = { showMoodDialog = false },
            onSave = { mood, category, symptoms, note, timestamp ->
                onAddMood(mood, category, symptoms, note, timestamp)
                showMoodDialog = false
                if (mood <= -4.0 || mood >= 4.0) showSafetyHint = true
            }
        )
    }
    if (showDiaryDialog) {
        DiaryDialog(
            onDismiss = { showDiaryDialog = false },
            onSave = { text, photos ->
                onAddDiary(text, photos)
                showDiaryDialog = false
            }
        )
    }
    editEntry?.let { entry ->
        DiaryDialog(
            initialText = entry.text,
            initialPhotos = splitPipe(entry.photoUris),
            onDismiss = { editEntry = null },
            onSave = { text, photos ->
                onUpdateDiary(entry, text, photos)
                editEntry = null
            }
        )
    }
    if (showSafetyHint) {
        AlertDialog(
            onDismissRequest = { showSafetyHint = false },
            title = { Text("Открыть Safety Plan?") },
            text = { Text("Оценка состояния высокая по риску. Лучше свериться с планом поддержки.") },
            confirmButton = {
                Button(onClick = {
                    showSafetyHint = false
                    onOpenSafetyPlan()
                }) { Text("Открыть") }
            },
            dismissButton = { TextButton(onClick = { showSafetyHint = false }) { Text("Позже") } }
        )
    }
    selectedPhoto?.let { uri ->
        AlertDialog(
            onDismissRequest = { selectedPhoto = null },
            confirmButton = { TextButton(onClick = { selectedPhoto = null }) { Text("Закрыть") } },
            text = {
                UriImage(
                    uri = uri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
            }
        )
    }
}

@Composable
private fun MascotSceneBanner(
    imageRes: Int,
    title: String,
    subtitle: String,
    accentColor: Color = TurquoiseAccent
) {
    SectionCard(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Image(
                painter = painterResource(imageRes),
                contentDescription = title,
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun MoodSummaryChip(latestMood: Double) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Последнее состояние", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(moodDescription(latestMood), fontWeight = FontWeight.Medium)
            }
            Text(
                latestMood.prettyMood(),
                style = MaterialTheme.typography.headlineMedium,
                color = moodColor(latestMood),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DiaryFeedCard(
    entry: DiaryEntryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPhotoClick: (String) -> Unit
) {
    PaperCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = {},
                label = { Text("Заметка") },
                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                    containerColor = LavenderAccent.copy(alpha = 0.2f),
                    labelColor = LavenderAccent
                )
            )
            Text(
                formatDateTime(entry.timestamp),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF5A5A5A)
            )
        }
        Text(entry.text, color = Color(0xFF1F1F1F))
        val photos = splitPipe(entry.photoUris)
        if (photos.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                photos.forEachIndexed { index, uri ->
                    UriPolaroid(uri = uri, index = index, onClick = { onPhotoClick(uri) })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onEdit) { Text("Редактировать", color = Color(0xFF333333)) }
            TextButton(onClick = onDelete) { Text("Удалить", color = RedAccent) }
        }
    }
}

@Composable
private fun MoodFeedCard(
    entry: MoodEntryEntity,
    onDelete: () -> Unit
) {
    SectionCard(
        containerColor = moodColor(entry.mood).copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = {},
                label = { Text("Состояние") },
                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                    containerColor = moodColor(entry.mood).copy(alpha = 0.2f),
                    labelColor = moodColor(entry.mood)
                )
            )
            Text(
                formatDateTime(entry.timestamp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.category.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold)
                if (entry.note.isNotBlank()) Text(entry.note, modifier = Modifier.padding(top = 4.dp))
            }
            Text(
                entry.mood.prettyMood(),
                style = MaterialTheme.typography.headlineMedium,
                color = moodColor(entry.mood),
                fontWeight = FontWeight.Bold
            )
        }
        val symptoms = splitPipe(entry.symptoms)
        if (symptoms.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                symptoms.forEach { symptom ->
                    AssistChip(onClick = {}, label = { Text(symptom, style = MaterialTheme.typography.labelSmall) })
                }
            }
        }
        TextButton(onClick = onDelete) { Text("Удалить", color = RedAccent) }
    }
}

@Composable
private fun InsightsScreen(
    moods: List<MoodEntryEntity>,
    impulses: List<ImpulseEntryEntity>,
    intakes: List<MedicationIntakeEntity>,
    sleepEntries: List<SleepEntryEntity>,
    onAddMood: (Double, String, List<String>, String, Long) -> Unit,
    onDeleteMood: (MoodEntryEntity) -> Unit,
    onAddSleep: (String, String, String, Int, List<String>) -> Unit,
    onAddImpulse: (String, Double?, String, Int, String) -> Unit,
    onTrustedScore: (ImpulseEntryEntity, Int, String) -> Unit,
    onDeleteImpulse: (ImpulseEntryEntity) -> Unit,
    @Suppress("UNUSED_PARAMETER") trustedPeople: List<TrustedPersonEntity>,
    onOpenSafetyPlan: () -> Unit
) {
    var mode by rememberSaveable { mutableStateOf("График") }
    var section by rememberSaveable { mutableStateOf("Графики") }
    var showMoodDialog by rememberSaveable { mutableStateOf(false) }
    var showSleepDialog by rememberSaveable { mutableStateOf(false) }
    var showImpulseDialog by rememberSaveable { mutableStateOf(false) }
    var scoringEntry by remember { mutableStateOf<ImpulseEntryEntity?>(null) }
    val elevated = moods.firstOrNull()?.mood ?: 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Header("Обзор", "Графики, импульсы, сон и тренды")
            MascotSceneBanner(
                imageRes = R.drawable.mascot_owl_overview,
                title = "Сова смотрит на картину",
                subtitle = "Замечай паттерны — без давления и оценок",
                accentColor = LavenderAccent
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Графики", "Импульсы", "Сон").forEach { tab ->
                    FilterChip(
                        selected = section == tab,
                        onClick = { section = tab },
                        label = { Text(tab) }
                    )
                }
            }
        }

        if (section == "Графики") {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("График", "Теплокарта").forEach {
                        FilterChip(selected = mode == it, onClick = { mode = it }, label = { Text(it) })
                    }
                }
            }
            item {
                SectionCard {
                    if (mode == "График") {
                        MoodLineChart(entries = moods.reversed(), compact = false)
                    } else {
                        MoodHeatMap(entries = moods)
                    }
                }
            }
            item {
                SectionCard {
                    SectionTitle("Приверженность терапии")
                    val taken = intakes.count { it.status == "taken" }
                    val missed = intakes.count { it.status != "taken" }
                    AdherenceBars(taken = taken, missed = missed)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("Записей", moods.size.toString(), TurquoiseAccent)
                    StatPill("Импульсов", impulses.size.toString(), RedAccent)
                    StatPill("Среднее", averageMood(moods).prettyMood(), moodColor(averageMood(moods)))
                }
            }
        }

        if (section == "Импульсы") {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (elevated >= 4.0) {
                        SectionCard(containerColor = RedAccent.copy(alpha = 0.16f)) {
                            Text(
                                "Сейчас высокий подъём. Перед крупными решениями лучше спросить близкого.",
                                color = RedAccent
                            )
                        }
                    }
                    Button(onClick = { showImpulseDialog = true }) { Text("Добавить импульс") }
                }
            }
            items(impulses, key = { it.id }) { entry ->
                SectionCard {
                    Text(entry.description, style = MaterialTheme.typography.titleLarge)
                    Text("${formatDateTime(entry.timestamp)} • ${entry.category}")
                    entry.cost?.let { Text("Стоимость: ${it.roundToInt()}") }
                    ScoreBars(author = entry.authorScore, auto = entry.autoScore, trusted = entry.trustedScore)
                    if (entry.authorComment.isNotBlank()) Text("Комментарий: ${entry.authorComment}")
                    if (entry.trustedComment.isNotBlank()) Text("Близкий: ${entry.trustedComment}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { scoringEntry = entry }) { Text("Оценка близкого") }
                        TextButton(onClick = { onDeleteImpulse(entry) }) { Text("Удалить", color = RedAccent) }
                    }
                }
            }
        }

        if (section == "Сон") {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showMoodDialog = true }) { Text("Состояние") }
                    Button(onClick = { showSleepDialog = true }) { Text("Добавить сон") }
                }
            }
            items(sleepEntries, key = { it.id }) { entry ->
                SectionCard {
                    Text(entry.date, fontWeight = FontWeight.Bold)
                    Text("Сон: ${entry.asleepAt} - ${entry.wokeAt}, качество ${entry.quality}/10")
                    Text(splitPipe(entry.mixedStateMarkers).joinToString(", ").ifBlank { "Маркеры не отмечены" })
                }
            }
            items(moods, key = { "mood-${it.id}" }) { entry ->
                SectionCard {
                    MoodRow(entry)
                    if (entry.note.isNotBlank()) Text(entry.note, modifier = Modifier.padding(top = 8.dp))
                    TextButton(onClick = { onDeleteMood(entry) }) { Text("Удалить", color = RedAccent) }
                }
            }
        }

        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                onClick = onOpenSafetyPlan
            ) { Text("Помощь / Safety Plan") }
        }
    }

    if (showMoodDialog) {
        MoodEntryDialog(onDismiss = { showMoodDialog = false }, onSave = { mood, category, symptoms, note, timestamp ->
            onAddMood(mood, category, symptoms, note, timestamp)
            showMoodDialog = false
        })
    }
    if (showSleepDialog) {
        SleepDialog(onDismiss = { showSleepDialog = false }, onSave = { date, asleep, woke, quality, markers ->
            onAddSleep(date, asleep, woke, quality, markers)
            showSleepDialog = false
        })
    }
    if (showImpulseDialog) {
        ImpulseDialog(onDismiss = { showImpulseDialog = false }, onSave = { description, cost, category, score, comment ->
            onAddImpulse(description, cost, category, score, comment)
            showImpulseDialog = false
        })
    }
    scoringEntry?.let { entry ->
        TrustedScoreDialog(
            entry = entry,
            onDismiss = { scoringEntry = null },
            onSave = { score, comment ->
                onTrustedScore(entry, score, comment)
                scoringEntry = null
            }
        )
    }
}

@Composable
private fun MedicationsScreen(
    profile: ProfileEntity,
    medications: List<MedicationEntity>,
    intakes: List<MedicationIntakeEntity>,
    onAdd: (String, String, String, String, String) -> Unit,
    onTaken: (MedicationEntity) -> Unit,
    onMissed: (MedicationEntity) -> Unit,
    onDelete: (MedicationEntity) -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MascotSceneBanner(
                imageRes = R.drawable.mascot_bear_meds,
                title = "Медведь напоминает мягко",
                subtitle = "Приём лекарств — маленький заботливый шаг",
                accentColor = BearBrown
            )
            Spacer(Modifier.height(8.dp))
            Header("Лекарства", "Напоминания, календарь и пропуски")
            Button(onClick = { showDialog = true }) { Text("Добавить препарат") }
        }
        items(medications, key = { it.id }) { medication ->
            SectionCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(medication.name, style = MaterialTheme.typography.titleLarge)
                        Text("${medication.dosage} в ${medication.time}, ${medication.frequency}")
                        Text("Часовой пояс: ${medication.timeZone}")
                    }
                    if (medication.missedReminders >= 5) {
                        Text("Эскалация", color = RedAccent, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Пропущено напоминаний: ${medication.missedReminders}")
                medication.lastTakenAt?.let { Text("Последний прием: ${formatDateTime(it)}") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onTaken(medication) }) { Text("Принял(а)") }
                    OutlinedButton(onClick = { onMissed(medication) }) { Text("Пропуск") }
                    TextButton(onClick = { onDelete(medication) }) { Text("Удалить", color = RedAccent) }
                }
                AnimatedVisibility(medication.missedReminders >= 5) {
                    Text(
                        "[${profile.userName}] не принял(а) ${medication.name} уже ${(profile.reminderIntervalMinutes * 5) / 60.0} часа. Пожалуйста, напомни.",
                        color = RedAccent
                    )
                }
            }
        }
        item {
            SectionTitle("Календарь приемов")
        }
        items(intakes.take(20), key = { it.id }) { intake ->
            SectionCard {
                Text("${formatDateTime(intake.scheduledAt)} - ${intake.medicationName}")
                Text(
                    when (intake.status) {
                        "taken" -> "Принято"
                        "escalated" -> "Пропуск с эскалацией"
                        else -> "Пропущено"
                    },
                    color = if (intake.status == "taken") TurquoiseAccent else RedAccent
                )
            }
        }
    }
    if (showDialog) {
        MedicationDialog(
            defaultTimeZone = profile.timeZone,
            onDismiss = { showDialog = false },
            onSave = { name, dosage, time, frequency, timeZone ->
                onAdd(name, dosage, time, frequency, timeZone)
                showDialog = false
            }
        )
    }
}

@Composable
private fun ProfileScreen(
    profile: ProfileEntity,
    trustedPeople: List<TrustedPersonEntity>,
    exportCsv: () -> String,
    onSaveProfile: (ProfileEntity) -> Unit,
    onAddTrusted: (String, String) -> Unit,
    onRevokeTrusted: (TrustedPersonEntity) -> Unit,
    onRestoreTrusted: (TrustedPersonEntity) -> Unit,
    onDeleteTrusted: (TrustedPersonEntity) -> Unit,
    onOpenSafetyPlan: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var name by remember(profile) { mutableStateOf(profile.userName) }
    var birthday by remember(profile) { mutableStateOf(profile.birthday) }
    var dark by remember(profile) { mutableStateOf(profile.darkTheme) }
    var sound by remember(profile) { mutableStateOf(profile.soundEnabled) }
    var timeZone by remember(profile) { mutableStateOf(profile.timeZone) }
    var interval by remember(profile) { mutableStateOf(profile.reminderIntervalMinutes.toString()) }
    var doctor by remember(profile) { mutableStateOf(profile.doctorContact) }
    var crisis by remember(profile) { mutableStateOf(profile.crisisNumbers) }
    var strategies by remember(profile) { mutableStateOf(profile.copingStrategies.replace("|", "\n")) }
    var trustedName by rememberSaveable { mutableStateOf("") }
    var trustedPhone by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            MascotSceneBanner(
                imageRes = R.drawable.mascot_team_profile,
                title = "Мы на связи",
                subtitle = "Барсик, сова и медведь — твоя команда поддержки",
                accentColor = YellowAccent
            )
            Spacer(Modifier.height(8.dp))
            Header("Профиль", "Настройки, близкие и экспорт")
        }
        item {
            SectionCard {
                SectionTitle("Профиль пользователя")
                OutlinedTextField(name, { name = it }, label = { Text("Имя") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(birthday, { birthday = it }, label = { Text("Дата рождения, опционально") }, modifier = Modifier.fillMaxWidth())
                ToggleRow("Темная тема", dark) { dark = it }
                ToggleRow("Звуковые уведомления", sound) { sound = it }
                OutlinedTextField(timeZone, { timeZone = it }, label = { Text("Часовой пояс") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(interval, { interval = it.filter(Char::isDigit) }, label = { Text("Интервал напоминаний, мин") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    onSaveProfile(
                        profile.copy(
                            userName = name.ifBlank { "Пользователь" },
                            birthday = birthday,
                            darkTheme = dark,
                            soundEnabled = sound,
                            timeZone = timeZone.ifBlank { TimeZone.getDefault().id },
                            reminderIntervalMinutes = interval.toIntOrNull()?.coerceAtLeast(15) ?: 30
                        )
                    )
                }) { Text("Сохранить профиль") }
            }
        }
        item {
            SectionCard {
                SectionTitle("Safety Plan")
                OutlinedTextField(doctor, { doctor = it }, label = { Text("Контакт врача") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(crisis, { crisis = it }, label = { Text("Кризисные номера") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(strategies, { strategies = it }, label = { Text("Стратегии, каждая с новой строки") }, modifier = Modifier.fillMaxWidth().height(130.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        onSaveProfile(
                            profile.copy(
                                doctorContact = doctor,
                                crisisNumbers = crisis,
                                copingStrategies = strategies.lines().filter { it.isNotBlank() }.joinToString("|")
                            )
                        )
                    }) { Text("Сохранить") }
                    OutlinedButton(onClick = onOpenSafetyPlan) { Text("Открыть") }
                }
            }
        }
        item {
            SectionCard {
                SectionTitle("Доступ близких")
                OutlinedTextField(trustedName, { trustedName = it }, label = { Text("Имя близкого") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(trustedPhone, { trustedPhone = it }, label = { Text("Телефон") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    if (trustedName.isNotBlank()) {
                        onAddTrusted(trustedName, trustedPhone)
                        trustedName = ""
                        trustedPhone = ""
                    }
                }) { Text("Сгенерировать доступ") }
                Divider(Modifier.padding(vertical = 10.dp))
                trustedPeople.forEach { person ->
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text("${person.name} ${if (person.revoked) "(отозван)" else ""}", fontWeight = FontWeight.Bold)
                        Text("Токен: ${person.accessToken}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (person.revoked) {
                                TextButton(onClick = { onRestoreTrusted(person) }) { Text("Вернуть") }
                            } else {
                                TextButton(onClick = { onRevokeTrusted(person) }) { Text("Отозвать", color = RedAccent) }
                            }
                            TextButton(onClick = { onDeleteTrusted(person) }) { Text("Удалить") }
                        }
                    }
                }
            }
        }
        item {
            SectionCard {
                SectionTitle("Экспорт / резервная копия")
                Text("CSV формируется локально и может быть скопирован для врача или резервной копии.")
                Button(onClick = { clipboard.setText(AnnotatedString(exportCsv())) }) { Text("Скопировать CSV") }
            }
        }
    }
}

@Composable
private fun MoodEntryDialog(
    onDismiss: () -> Unit,
    onSave: (Double, String, List<String>, String, Long) -> Unit
) {
    val categories = listOf("депрессия", "гипомания", "мания", "смешанное", "нейтральное")
    val symptomOptions = listOf(
        "нарушение сна",
        "повышенная говорливость",
        "раздражительность",
        "чувство вины",
        "дереализация/деперсонализация",
        "ускорение мыслей",
        "снижение потребности во сне"
    )
    var mood by rememberSaveable { mutableStateOf(0f) }
    var category by rememberSaveable { mutableStateOf("нейтральное") }
    var note by rememberSaveable { mutableStateOf("") }
    var minutesAgo by rememberSaveable { mutableStateOf("0") }
    val symptoms = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Фиксация состояния") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.wrapContentHeight()) {
                Text("Настроение: ${mood.toDouble().prettyMood()}")
                Slider(value = mood, onValueChange = { mood = (it * 2).roundToInt() / 2f }, valueRange = -5f..5f, steps = 19)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach {
                        FilterChip(selected = category == it, onClick = { category = it }, label = { Text(it) })
                    }
                }
                OutlinedTextField(
                    value = minutesAgo,
                    onValueChange = { minutesAgo = it.filter(Char::isDigit) },
                    label = { Text("Сколько минут назад") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Симптомы", fontWeight = FontWeight.Bold)
                symptomOptions.forEach { symptom ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = symptoms.contains(symptom),
                            onCheckedChange = { checked -> if (checked) symptoms.add(symptom) else symptoms.remove(symptom) }
                        )
                        Text(symptom)
                    }
                }
                OutlinedTextField(note, { note = it }, label = { Text("Заметка") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val timestamp = System.currentTimeMillis() - (minutesAgo.toLongOrNull() ?: 0L) * 60_000L
                onSave(mood.toDouble(), category, symptoms.toList(), note, timestamp)
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun SleepDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, List<String>) -> Unit
) {
    val markers = listOf("раздражительность + грусть", "эйфория + заторможенность", "беспокойство + подавленность")
    val selected = remember { mutableStateListOf<String>() }
    var date by rememberSaveable { mutableStateOf(SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())) }
    var asleep by rememberSaveable { mutableStateOf("23:00") }
    var woke by rememberSaveable { mutableStateOf("07:00") }
    var quality by rememberSaveable { mutableStateOf(7f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Сон") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(date, { date = it }, label = { Text("Дата") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(asleep, { asleep = it }, label = { Text("Заснул(а)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(woke, { woke = it }, label = { Text("Проснулся(ась)") }, modifier = Modifier.fillMaxWidth())
                Text("Качество: ${quality.roundToInt()}/10")
                Slider(quality, { quality = it }, valueRange = 1f..10f, steps = 8)
                markers.forEach { marker ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(selected.contains(marker), { checked -> if (checked) selected.add(marker) else selected.remove(marker) })
                        Text(marker)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(date, asleep, woke, quality.roundToInt(), selected.toList()) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun ImpulseDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double?, String, Int, String) -> Unit
) {
    val categories = listOf(
        "Хобби/мелочи",
        "Одежда/косметика",
        "Техника/гаджеты",
        "Путешествия/билеты",
        "Кредиты/финансовые обязательства",
        "Отношения/крупные решения",
        "Другое"
    )
    var description by rememberSaveable { mutableStateOf("") }
    var cost by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(categories.first()) }
    var score by rememberSaveable { mutableStateOf(5f) }
    var comment by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый импульс") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(description, { description = it }, label = { Text("Что сделал/захотел") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(cost, { cost = it.filter { char -> char.isDigit() || char == '.' } }, label = { Text("Стоимость, опционально") }, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { FilterChip(selected = category == it, onClick = { category = it }, label = { Text(it) }) }
                }
                Text("Моя оценка: ${score.roundToInt()}/10")
                Slider(score, { score = it }, valueRange = 1f..10f, steps = 8)
                OutlinedTextField(comment, { comment = it }, label = { Text("Комментарий") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(enabled = description.isNotBlank(), onClick = {
                onSave(description, cost.toDoubleOrNull(), category, score.roundToInt(), comment)
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun TrustedScoreDialog(
    entry: ImpulseEntryEntity,
    onDismiss: () -> Unit,
    onSave: (Int, String) -> Unit
) {
    var score by rememberSaveable { mutableStateOf((entry.trustedScore ?: entry.authorScore).toFloat()) }
    var comment by rememberSaveable { mutableStateOf(entry.trustedComment) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Оценка близкого") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(entry.description)
                Text("Оценка: ${score.roundToInt()}/10")
                Slider(score, { score = it }, valueRange = 1f..10f, steps = 8)
                OutlinedTextField(comment, { comment = it }, label = { Text("Комментарий") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onSave(score.roundToInt(), comment) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun MedicationDialog(
    defaultTimeZone: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var dosage by rememberSaveable { mutableStateOf("") }
    var time by rememberSaveable { mutableStateOf("09:00") }
    var frequency by rememberSaveable { mutableStateOf("ежедневно") }
    var timeZone by rememberSaveable { mutableStateOf(defaultTimeZone) }
    val frequencies = listOf("ежедневно", "через день", "по расписанию")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Препарат") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(dosage, { dosage = it }, label = { Text("Дозировка") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(time, { time = it }, label = { Text("Время приема") }, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    frequencies.forEach { FilterChip(selected = frequency == it, onClick = { frequency = it }, label = { Text(it) }) }
                }
                OutlinedTextField(timeZone, { timeZone = it }, label = { Text("Часовой пояс") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(enabled = name.isNotBlank(), onClick = { onSave(name, dosage, time, frequency, timeZone) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun DiaryDialog(
    initialText: String = "",
    initialPhotos: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (String, List<String>) -> Unit
) {
    var text by rememberSaveable { mutableStateOf(initialText) }
    val photos = remember { mutableStateListOf<String>().apply { addAll(initialPhotos) } }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        photos.addAll(uris.map { it.toString() })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Запись дневника") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(text, { text = it }, label = { Text("Текст") }, modifier = Modifier.fillMaxWidth().height(150.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picker.launch("image/*") }) { Text("Фото из галереи") }
                    OutlinedButton(onClick = { text += "\nФото с камеры: добавьте снимок через галерею после съемки." }) { Text("Камера") }
                }
                Text("Фото: ${photos.size}")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    photos.forEach { AssistChip(onClick = { photos.remove(it) }, label = { Text("Удалить фото") }) }
                }
            }
        },
        confirmButton = { Button(enabled = text.isNotBlank() || photos.isNotEmpty(), onClick = { onSave(text, photos.toList()) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun SafetyPlanDialog(profile: ProfileEntity, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Safety Plan / Помощь") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Врач", fontWeight = FontWeight.Bold)
                Text(profile.doctorContact)
                Text("Кризисные номера", fontWeight = FontWeight.Bold)
                Text(profile.crisisNumbers)
                Text("Стратегии совладания", fontWeight = FontWeight.Bold)
                splitPipe(profile.copingStrategies).forEachIndexed { index, strategy ->
                    Text("${index + 1}. $strategy")
                }
                Text("Если есть риск причинить вред себе или другим, обратись за экстренной помощью.", color = RedAccent)
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Закрыть") } }
    )
}

@Composable
private fun Header(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun PaperCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(rotationZ = -0.5f),
        colors = CardDefaults.cardColors(containerColor = PaperColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 22.dp, bottomStart = 18.dp, bottomEnd = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .background(PaperColor)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun EmptyState(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
}

@Composable
private fun MoodRow(entry: MoodEntryEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(formatDateTime(entry.timestamp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(entry.category, fontWeight = FontWeight.Bold)
        }
        Text(entry.mood.prettyMood(), color = moodColor(entry.mood), style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun ProactiveHint(latestMood: Double, hasRecentSleepIssue: Boolean) {
    val hint = when {
        latestMood >= 4.0 -> "Сейчас много энергии — можно отложить крупные решения и свериться с близкими."
        latestMood <= -4.0 -> "Тяжёлый момент. Ты не один — рядом Safety Plan и люди, которым можно написать."
        hasRecentSleepIssue -> "Сон мог сбиться — это часто бывает. Небольшой отдых уже может помочь."
        else -> "Короткие записи помогают замечать тренды. Делай в своём темпе."
    }
    SectionCard(containerColor = TurquoiseAccent.copy(alpha = 0.14f)) {
        Text(hint)
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(value, color = color, fontWeight = FontWeight.Bold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MoodLineChart(entries: List<MoodEntryEntity>, compact: Boolean) {
    val points = entries.sortedBy { it.timestamp }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 140.dp else 240.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF121212))
            .padding(12.dp)
    ) {
        val graphWidth = size.width
        val graphHeight = size.height
        for (i in 0..4) {
            val y = graphHeight * i / 4f
            drawLine(Color(0xFF2A2A2A), Offset(0f, y), Offset(graphWidth, y), strokeWidth = 1.dp.toPx())
        }
        if (points.size < 2) {
            drawCircle(TurquoiseAccent, radius = 5.dp.toPx(), center = Offset(graphWidth / 2, graphHeight / 2))
            return@Canvas
        }
        val minTime = points.first().timestamp.toFloat()
        val maxTime = points.last().timestamp.toFloat().coerceAtLeast(minTime + 1)
        val offsets = points.map {
            val x = ((it.timestamp - minTime) / (maxTime - minTime)) * graphWidth
            val y = graphHeight - (((it.mood + 5.0) / 10.0).toFloat() * graphHeight)
            Offset(x, y)
        }
        offsets.zipWithNext().forEach { (a, b) ->
            drawLine(moodColor(points[offsets.indexOf(a)].mood), a, b, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        }
        offsets.forEachIndexed { index, offset ->
            drawCircle(moodColor(points[index].mood), radius = 5.dp.toPx(), center = offset)
        }
    }
}

@Composable
private fun MoodHeatMap(entries: List<MoodEntryEntity>) {
    val byDate = entries.groupBy { formatDate(it.timestamp) }
    val dates = (0 until 42).map { offset ->
        val time = System.currentTimeMillis() - (41 - offset) * 24L * 60L * 60L * 1000L
        formatDate(time)
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        dates.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { date ->
                    val average = byDate[date]?.map { it.mood }?.average()
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(average?.let { moodColor(it) } ?: Color.Black)
                            .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                    )
                }
            }
        }
        Text("Каждый квадрат - день. Черный цвет означает отсутствие записей.")
    }
}

@Composable
private fun ImpulseScatter(impulses: List<ImpulseEntryEntity>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF121212))
            .padding(8.dp)
    ) {
        val list = impulses.take(30).reversed()
        if (list.isEmpty()) return@Canvas
        list.forEachIndexed { index, entry ->
            val x = if (list.size == 1) size.width / 2 else index * size.width / (list.size - 1)
            val y = size.height - entry.authorScore / 10f * size.height
            drawCircle(if (entry.authorScore >= 7) RedAccent else YellowAccent, 6.dp.toPx(), Offset(x, y))
        }
    }
}

@Composable
private fun AdherenceBars(taken: Int, missed: Int) {
    val total = (taken + missed).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Bar("Принято", taken, total, TurquoiseAccent)
        Bar("Пропущено", missed, total, RedAccent)
    }
}

@Composable
private fun Bar(label: String, value: Int, total: Int, color: Color) {
    Column {
        Text("$label: $value")
        Box(Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFF2A2A2A))) {
            Box(Modifier.fillMaxWidth(value / total.toFloat()).height(18.dp).background(color))
        }
    }
}

@Composable
private fun ScoreBars(author: Int, auto: Int, trusted: Int?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Bar("Моя оценка", author, 10, YellowAccent)
        Bar("Автооценка", auto, 10, TurquoiseAccent)
        trusted?.let { Bar("Оценка близкого", it, 10, RedAccent) }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun UriPolaroid(uri: String, index: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(132.dp)
            .graphicsLayer(rotationZ = ((index % 5) - 2) * 1.8f)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        UriImage(
            uri = uri,
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(4.dp))
        )
        Text("Фото", color = Color.Black, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun UriImage(uri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var image by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri) {
        image = withContext(Dispatchers.IO) {
            runCatching {
                val stream = when {
                    uri.startsWith("/") -> File(uri).takeIf { it.exists() }?.inputStream()
                    else -> {
                        val parsed = Uri.parse(uri)
                        if (parsed.scheme.isNullOrBlank()) return@runCatching null
                        context.contentResolver.openInputStream(parsed)
                    }
                } ?: return@runCatching null
                stream.use { input ->
                    BitmapFactory.decodeStream(input)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    if (image != null) {
        Image(bitmap = image!!, contentDescription = "Фото дневника", contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        Box(modifier = modifier.background(Color(0xFFECECEC)), contentAlignment = Alignment.Center) {
            Text("Фото", color = Color.Black)
        }
    }
}

@Composable
private fun BipolarMoodTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val darkScheme = darkColorScheme(
        background = DeepBlack,
        surface = SurfaceBlack,
        surfaceVariant = Color(0xFF1E1E1E),
        primary = YellowAccent,
        secondary = TurquoiseAccent,
        tertiary = LavenderAccent,
        onBackground = Color.White,
        onSurface = Color.White,
        onSurfaceVariant = Color(0xFFB8B8B8)
    )
    val lightScheme = lightColorScheme(
        background = Color(0xFFF5F3EF),
        surface = Color.White,
        surfaceVariant = Color(0xFFEDE8E0),
        primary = Color(0xFF7A5C00),
        secondary = Color(0xFF006874),
        tertiary = Color(0xFF6B4C9A),
        onBackground = Color(0xFF101010),
        onSurface = Color(0xFF101010),
        onSurfaceVariant = Color(0xFF555555)
    )
    MaterialTheme(
        colorScheme = if (darkTheme) darkScheme else lightScheme,
        typography = Typography(),
        content = content
    )
}

private fun moodColor(mood: Double): Color {
    return if (mood < 0) {
        lerp(RedAccent, Color(0xFF777777), ((mood + 5.0) / 5.0).toFloat().coerceIn(0f, 1f))
    } else {
        lerp(Color(0xFF777777), YellowAccent, (mood / 5.0).toFloat().coerceIn(0f, 1f))
    }
}

private fun moodDescription(mood: Double): String = when {
    mood <= -4 -> "Сейчас непросто — это заметно"
    mood < -1 -> "Немного ниже обычного"
    mood >= 4 -> "Много подъёма и энергии"
    mood > 1 -> "Чуть выше обычного"
    else -> "Ровное, спокойное состояние"
}

private fun averageMood(entries: List<MoodEntryEntity>): Double {
    return entries.take(12).map { it.mood }.average().takeIf { !it.isNaN() } ?: 0.0
}

private fun splitPipe(value: String): List<String> {
    return value.split("|").map { it.trim() }.filter { it.isNotBlank() }
}

private val DeepBlack = Color(0xFF0B0B0B)
private val SurfaceBlack = Color(0xFF121212)
private val YellowAccent = Color(0xFFFFD700)
private val RedAccent = Color(0xFFFF3B30)
private val TurquoiseAccent = Color(0xFF4DD0E1)
private val LavenderAccent = Color(0xFFB39DDB)
private val BearBrown = Color(0xFF8D6E63)
private val PaperColor = Color(0xFFFFF8E7)
