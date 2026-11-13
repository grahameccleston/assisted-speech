package com.assisted.speech.ui

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assisted.speech.data.*
import kotlinx.coroutines.delay

val BackgroundDark = Color(0xFF0B1120)
val CardDark = Color(0xFF1E293B)
val CardShortcutDark = Color(0xFF064E3B)
val BorderDark = Color(0xFF334155)
val AccentBlue = Color(0xFF2563EB)
val AccentCyan = Color(0xFF38BDF8)
val AccentGreen = Color(0xFF16A34A)
val AccentEmerald = Color(0xFF10B981)
val AccentAmber = Color(0xFFD97706)
val AccentGold = Color(0xFFF59E0B)
val AccentRed = Color(0xFFDC2626)
val TextLight = Color(0xFFF8FAFC)
val TextMuted = Color(0xFF94A3B8)

@Composable
fun MainScreen(viewModel: SpeechViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val layoutDir = if (uiState.languageMetadata.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val focusRequester = remember { FocusRequester() }

    // Auto-Scan Timer Loop when 1-Switch / Auto-Scan Mode is active
    LaunchedEffect(
        uiState.motorSettings.autoScanEnabled,
        uiState.motorSettings.scanIntervalMs,
        uiState.isCaregiverUnlocked,
        uiState.isSpellingOpen,
        uiState.currentPageIndex,
        uiState.navigationStack.size
    ) {
        if (uiState.motorSettings.autoScanEnabled && !uiState.isCaregiverUnlocked && !uiState.isSpellingOpen) {
            while (true) {
                delay(uiState.motorSettings.scanIntervalMs)
                viewModel.advanceScanFocus()
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    // Hardware Bluetooth / USB 1-Switch & 2-Switch support
                    if (event.type == KeyEventType.KeyDown && !uiState.isCaregiverUnlocked) {
                        when (event.key) {
                            Key.Spacebar, Key.Enter, Key.DirectionCenter -> {
                                if (uiState.motorSettings.autoScanEnabled) {
                                    viewModel.triggerScannedItem()
                                    true
                                } else {
                                    false
                                }
                            }
                            Key.Tab, Key.DirectionRight, Key.DirectionDown -> {
                                if (uiState.motorSettings.autoScanEnabled) {
                                    viewModel.advanceScanFocus()
                                    true
                                } else {
                                    false
                                }
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                },
            color = BackgroundDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. TOP HIGH-CONTRAST SENTENCE DISPLAY + 3s HOLD CAREGIVER LOCK
                TopSentenceStatusHeader(
                    uiState = uiState,
                    onToggleCaregiver = { viewModel.toggleCaregiverMode() }
                )

                // 2. BODY: CAREGIVER MODE OR ERGONOMIC REACH COMMUNICATION WORKSPACE
                if (uiState.isCaregiverUnlocked) {
                    CaregiverManagementView(
                        uiState = uiState,
                        onSwitchLanguage = { viewModel.switchLanguage(it) },
                        onLaunchTtsInstaller = { viewModel.launchTtsInstaller() },
                        onPreviewVoice = { viewModel.previewVoice() },
                        onUpdateMotorSettings = { settings, preview ->
                            viewModel.updateMotorSettings(settings, preview)
                        },
                        onAddCustomNode = { text, parentId, emoji, pinned ->
                            viewModel.addCustomPhraseNode(text, parentId, emoji, pinned)
                        },
                        onTogglePin = { viewModel.toggleNodePinned(it) },
                        onDeleteNode = { viewModel.deleteCustomNode(it) },
                        onExportProfile = { viewModel.exportProfileToJson() },
                        onImportProfile = { viewModel.importProfileFromJson(it) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    ErgonomicWorkspace(
                        uiState = uiState,
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun TopSentenceStatusHeader(
    uiState: SpeechUiState,
    onToggleCaregiver: () -> Unit
) {
    val displayText = uiState.displaySentence
    val lastSpokenText = uiState.lastSpokenSentence
    val breadcrumbs = uiState.breadcrumbLabels
    val strings = uiState.languageMetadata.uiStrings
    val scrollState = rememberScrollState()

    LaunchedEffect(displayText) {
        if (displayText.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(2.dp, if (displayText.isNotEmpty()) AccentCyan else BorderDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusLabel = when {
                    uiState.statusBannerMessage != null -> "✨ ${uiState.statusBannerMessage}"
                    breadcrumbs.isNotEmpty() -> "➜ " + breadcrumbs.joinToString(" ➜ ")
                    displayText.isNotEmpty() -> strings.buildingSentence
                    lastSpokenText.isNotEmpty() -> strings.lastSpokenPrompt
                    else -> "${uiState.languageMetadata.displayName.uppercase()} • ${strings.readyPrompt}"
                }
                Text(
                    text = statusLabel,
                    color = if (breadcrumbs.isNotEmpty() || uiState.statusBannerMessage != null) AccentCyan else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                CaregiverHoldLockButton(
                    isUnlocked = uiState.isCaregiverUnlocked,
                    onUnlockToggle = onToggleCaregiver
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(Color(0xFF060911), RoundedCornerShape(10.dp))
                    .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val textToShow = when {
                    displayText.isNotEmpty() -> displayText
                    lastSpokenText.isNotEmpty() -> "🔁 \"$lastSpokenText\""
                    else -> strings.tapShortcutPrompt
                }
                val textColor = when {
                    displayText.isNotEmpty() -> AccentCyan
                    lastSpokenText.isNotEmpty() -> Color(0xFF86EFAC)
                    else -> TextMuted
                }
                Text(
                    text = textToShow,
                    color = textColor,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    modifier = Modifier.horizontalScroll(scrollState)
                )
            }
        }
    }
}

@Composable
fun ErgonomicWorkspace(
    uiState: SpeechUiState,
    viewModel: SpeechViewModel,
    modifier: Modifier = Modifier
) {
    val handedness = uiState.motorSettings.handedness

    when (handedness) {
        Handedness.BOTTOM -> {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (uiState.isSpellingOpen) {
                        AccessibleSpellingView(
                            uiState = uiState,
                            onSelectWord = { viewModel.onPredictWordSelected(it) },
                            onTypeLetter = { viewModel.onLetterTyped(it) },
                            onSelectDiacritic = { viewModel.onDiacriticSelected(it) },
                            onSpace = { viewModel.onNextWordSpace() },
                            onSavePhrase = { viewModel.saveAndSpeakSpelledSentence() },
                            onNextKeyboardPage = { viewModel.nextKeyboardPage() }
                        )
                    } else {
                        ZeroScrollPhraseWorkspace(
                            uiState = uiState,
                            onSelectShortcut = { viewModel.onSmartShortcutSelected(it) },
                            onSelectNode = { viewModel.onTreeNodeSelected(it) },
                            onTriggerScan = { viewModel.triggerScannedItem() }
                        )
                    }
                }

                BottomReachControlDock(
                    uiState = uiState,
                    onSpeakOrRepeat = { viewModel.speakOrRepeatCurrentSentence() },
                    onBackOrHome = {
                        if (!uiState.isAtRoot) viewModel.onTreeNavigateUp()
                        else viewModel.clearSentence()
                    },
                    onResetRoot = { viewModel.onTreeResetToRoot() },
                    onUndo = { viewModel.backspaceSentence() },
                    onPrevPage = { viewModel.prevGridPage() },
                    onNextPage = { viewModel.nextGridPage() },
                    onToggleSpell = { viewModel.toggleSpellingMode() },
                    onBell = { viewModel.ringAttentionBell() }
                )
            }
        }

        Handedness.RIGHT, Handedness.LEFT -> {
            Row(
                modifier = modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (handedness == Handedness.LEFT) {
                    SideReachControlRail(
                        uiState = uiState,
                        onSpeakOrRepeat = { viewModel.speakOrRepeatCurrentSentence() },
                        onBackOrHome = {
                            if (!uiState.isAtRoot) viewModel.onTreeNavigateUp()
                            else viewModel.clearSentence()
                        },
                        onUndo = { viewModel.backspaceSentence() },
                        onNextPage = { viewModel.nextGridPage() },
                        onToggleSpell = { viewModel.toggleSpellingMode() },
                        onBell = { viewModel.ringAttentionBell() },
                        modifier = Modifier.width(126.dp).fillMaxHeight()
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (uiState.isSpellingOpen) {
                        AccessibleSpellingView(
                            uiState = uiState,
                            onSelectWord = { viewModel.onPredictWordSelected(it) },
                            onTypeLetter = { viewModel.onLetterTyped(it) },
                            onSelectDiacritic = { viewModel.onDiacriticSelected(it) },
                            onSpace = { viewModel.onNextWordSpace() },
                            onSavePhrase = { viewModel.saveAndSpeakSpelledSentence() },
                            onNextKeyboardPage = { viewModel.nextKeyboardPage() }
                        )
                    } else {
                        ZeroScrollPhraseWorkspace(
                            uiState = uiState,
                            onSelectShortcut = { viewModel.onSmartShortcutSelected(it) },
                            onSelectNode = { viewModel.onTreeNodeSelected(it) },
                            onTriggerScan = { viewModel.triggerScannedItem() }
                        )
                    }
                }

                if (handedness == Handedness.RIGHT) {
                    SideReachControlRail(
                        uiState = uiState,
                        onSpeakOrRepeat = { viewModel.speakOrRepeatCurrentSentence() },
                        onBackOrHome = {
                            if (!uiState.isAtRoot) viewModel.onTreeNavigateUp()
                            else viewModel.clearSentence()
                        },
                        onUndo = { viewModel.backspaceSentence() },
                        onNextPage = { viewModel.nextGridPage() },
                        onToggleSpell = { viewModel.toggleSpellingMode() },
                        onBell = { viewModel.ringAttentionBell() },
                        modifier = Modifier.width(126.dp).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
fun ZeroScrollPhraseWorkspace(
    uiState: SpeechUiState,
    onSelectShortcut: (SmartShortcut) -> Unit,
    onSelectNode: (PhraseNodeEntity) -> Unit,
    onTriggerScan: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val columns = if (isLandscape) 3 else 2
    val rows = if (isLandscape) 2 else 3
    val scanActive = uiState.motorSettings.autoScanEnabled
    val shortcutsCount = uiState.smartShortcuts.size

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. SMART SHORTCUTS STRIP (2 1-Tap Complete Sentences)
        if (uiState.smartShortcuts.isNotEmpty()) {
            SmartShortcutsStrip(
                shortcuts = uiState.smartShortcuts,
                oneTapLabel = uiState.languageMetadata.uiStrings.oneTapSpeak,
                motorSettings = uiState.motorSettings,
                scannedIndex = if (scanActive) uiState.scanFocusIndex else -1,
                onSelectShortcut = onSelectShortcut
            )
        }

        // 2. FIXED 6-SLOT PAGED GRID (Zero Vertical Scrolling)
        val pagedNodes = uiState.currentPagedNodes
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (r in 0 until rows) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (c in 0 until columns) {
                        val index = r * columns + c
                        val node = pagedNodes.getOrNull(index)
                        if (node != null) {
                            val hasChildren = uiState.nodesWithChildrenIds.contains(node.conceptId)
                            val resolvedLabel = SentenceComposer.resolveTemplate(
                                rawText = node.text,
                                metadata = uiState.languageMetadata,
                                gender = uiState.motorSettings.speakerGender,
                                formality = uiState.motorSettings.formality
                            )
                            val isScanned = scanActive && (uiState.scanFocusIndex == shortcutsCount + index)
                            PhraseTileButton(
                                node = node,
                                resolvedLabel = resolvedLabel,
                                hasChildren = hasChildren,
                                isScanHighlighted = isScanned,
                                motorSettings = uiState.motorSettings,
                                onClick = { onSelectNode(node) },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            }
        }

        // 3. 1-SWITCH SCAN TRIGGER BAR (Only shown when Auto-Scan is enabled)
        if (scanActive) {
            AccessibleCardButton(
                onClick = onTriggerScan,
                settings = uiState.motorSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                backgroundColor = Color(0xFF713F12),
                pressedColor = Color(0xFFA16207),
                borderColor = Color(0xFFFACC15),
                borderWidth = 3.dp
            ) {
                Text(
                    text = "⚡ ${uiState.languageMetadata.uiStrings.scanTrigger}",
                    color = Color(0xFFFEF08A),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun SmartShortcutsStrip(
    shortcuts: List<SmartShortcut>,
    oneTapLabel: String,
    motorSettings: MotorAccessSettings,
    scannedIndex: Int,
    onSelectShortcut: (SmartShortcut) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        shortcuts.forEachIndexed { idx, shortcut ->
            AccessibleCardButton(
                onClick = { onSelectShortcut(shortcut) },
                settings = motorSettings,
                isScanHighlighted = (scannedIndex == idx),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                backgroundColor = CardShortcutDark,
                pressedColor = Color(0xFF047857),
                borderColor = AccentEmerald,
                borderWidth = 2.5.dp,
                accessibilityLabel = "One tap speak: ${shortcut.fullText}"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${shortcut.badgeLabel} • 🔊 $oneTapLabel",
                        color = Color(0xFFA7F3D0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${shortcut.iconEmoji} ${shortcut.fullText}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun PhraseTileButton(
    node: PhraseNodeEntity,
    resolvedLabel: String,
    hasChildren: Boolean,
    isScanHighlighted: Boolean,
    motorSettings: MotorAccessSettings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        node.conceptId == "q_help" || node.conceptId == "n_toilet" -> Color(0xFFF87171)
        hasChildren -> AccentCyan
        node.isPinned -> AccentGold
        else -> Color(0xFF4ADE80)
    }
    val bgColor = when {
        node.conceptId == "q_help" -> Color(0xFF450A0A)
        hasChildren -> Color(0xFF172554)
        else -> CardDark
    }

    AccessibleCardButton(
        onClick = onClick,
        settings = motorSettings,
        isScanHighlighted = isScanHighlighted,
        modifier = modifier,
        backgroundColor = bgColor,
        borderColor = borderColor,
        borderWidth = 2.5.dp,
        accessibilityLabel = resolvedLabel
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = node.iconEmoji, fontSize = 28.sp)
                if (node.isPinned) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "★", color = AccentGold, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (hasChildren) "$resolvedLabel ➜" else resolvedLabel,
                color = TextLight,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BottomReachControlDock(
    uiState: SpeechUiState,
    onSpeakOrRepeat: () -> Unit,
    onBackOrHome: () -> Unit,
    onResetRoot: () -> Unit,
    onUndo: () -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onToggleSpell: () -> Unit,
    onBell: () -> Unit
) {
    val settings = uiState.motorSettings
    val strings = uiState.languageMetadata.uiStrings
    val hasActiveText = uiState.displaySentence.isNotBlank()
    val isNextPageScanned = settings.autoScanEnabled &&
        uiState.totalPages > 1 &&
        uiState.scanFocusIndex == (uiState.smartShortcuts.size + uiState.currentPagedNodes.size)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: Paging + Navigation + Spelling + Bell
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AccessibleCardButton(
                onClick = onPrevPage,
                settings = settings,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                backgroundColor = Color(0xFF1E293B),
                borderColor = if (uiState.totalPages > 1) AccentCyan else BorderDark
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous Page", tint = TextLight)
                    Text(
                        text = "${uiState.currentPageIndex + 1}/${uiState.totalPages}",
                        color = TextLight,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AccessibleCardButton(
                onClick = onNextPage,
                settings = settings,
                isScanHighlighted = isNextPageScanned,
                modifier = Modifier.weight(1.1f).fillMaxHeight(),
                backgroundColor = Color(0xFF1E293B),
                borderColor = if (uiState.totalPages > 1) AccentCyan else BorderDark
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = strings.more,
                        color = TextLight,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next Page", tint = TextLight)
                }
            }

            AccessibleCardButton(
                onClick = onToggleSpell,
                settings = settings,
                modifier = Modifier.weight(1.1f).fillMaxHeight(),
                backgroundColor = if (uiState.isSpellingOpen) AccentBlue else Color(0xFF1E293B),
                borderColor = if (uiState.isSpellingOpen) Color.White else AccentBlue
            ) {
                Text(
                    text = if (uiState.isSpellingOpen) "💬 ${strings.tree}" else "🔤 ${strings.spell}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            AccessibleCardButton(
                onClick = onBell,
                settings = settings,
                modifier = Modifier.weight(0.75f).fillMaxHeight(),
                backgroundColor = Color(0xFF3B0764),
                borderColor = Color(0xFFC084FC),
                accessibilityLabel = "Ring attention bell"
            ) {
                Text(text = "🔔", fontSize = 22.sp)
            }
        }

        // Row 2: Primary Action Bar (SPEAK / REPEAT, BACK / HOME, UNDO) - Closest to bottom edge!
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccessibleCardButton(
                onClick = onSpeakOrRepeat,
                settings = settings,
                modifier = Modifier.weight(2.1f).fillMaxHeight(),
                backgroundColor = AccentGreen,
                pressedColor = Color(0xFF15803D),
                borderColor = Color(0xFF4ADE80),
                borderWidth = 3.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Speak",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasActiveText) strings.speakNow else strings.repeat,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            if (!uiState.isAtRoot) {
                AccessibleCardButton(
                    onClick = onBackOrHome,
                    settings = settings,
                    modifier = Modifier.weight(1.1f).fillMaxHeight(),
                    backgroundColor = Color(0xFF334155),
                    borderColor = AccentCyan
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.back, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            AccessibleCardButton(
                onClick = onUndo,
                settings = settings,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                backgroundColor = AccentAmber,
                borderColor = Color(0xFFFBBF24)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Undo", tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.undo, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            if (hasActiveText) {
                AccessibleCardButton(
                    onClick = onResetRoot,
                    settings = settings,
                    modifier = Modifier.weight(0.75f).fillMaxHeight(),
                    backgroundColor = AccentRed,
                    borderColor = Color(0xFFF87171),
                    accessibilityLabel = "Home"
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Start Over", tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
        }
    }
}

@Composable
fun SideReachControlRail(
    uiState: SpeechUiState,
    onSpeakOrRepeat: () -> Unit,
    onBackOrHome: () -> Unit,
    onUndo: () -> Unit,
    onNextPage: () -> Unit,
    onToggleSpell: () -> Unit,
    onBell: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.motorSettings
    val strings = uiState.languageMetadata.uiStrings
    val hasActiveText = uiState.displaySentence.isNotBlank()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AccessibleCardButton(
            onClick = onSpeakOrRepeat,
            settings = settings,
            modifier = Modifier.fillMaxWidth().weight(1.4f),
            backgroundColor = AccentGreen,
            borderColor = Color(0xFF4ADE80)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color.White)
                Text(
                    text = if (hasActiveText) strings.speakNow else strings.repeat,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }
        }

        AccessibleCardButton(
            onClick = onNextPage,
            settings = settings,
            modifier = Modifier.fillMaxWidth().weight(1f),
            backgroundColor = CardDark,
            borderColor = AccentCyan
        ) {
            Text(
                text = "${strings.more} ${uiState.currentPageIndex + 1}/${uiState.totalPages} ▶",
                color = TextLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }

        AccessibleCardButton(
            onClick = onBackOrHome,
            settings = settings,
            modifier = Modifier.fillMaxWidth().weight(1f),
            backgroundColor = Color(0xFF334155),
            borderColor = AccentCyan
        ) {
            Text(
                text = if (!uiState.isAtRoot) "⬅ ${strings.back}" else "✕ CLEAR",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        AccessibleCardButton(
            onClick = onUndo,
            settings = settings,
            modifier = Modifier.fillMaxWidth().weight(1f),
            backgroundColor = AccentAmber,
            borderColor = Color(0xFFFBBF24)
        ) {
            Text("⌫ ${strings.undo}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }

        AccessibleCardButton(
            onClick = onToggleSpell,
            settings = settings,
            modifier = Modifier.fillMaxWidth().weight(1f),
            backgroundColor = if (uiState.isSpellingOpen) AccentBlue else CardDark,
            borderColor = AccentBlue
        ) {
            Text(
                text = if (uiState.isSpellingOpen) "💬 ${strings.tree}" else "🔤 ${strings.spell}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        AccessibleCardButton(
            onClick = onBell,
            settings = settings,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            backgroundColor = Color(0xFF3B0764),
            borderColor = Color(0xFFC084FC)
        ) {
            Text("🔔 BELL", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Unified Accessible Spelling & Next-Word Predictor View with:
 * - Top 4 oversized word predictions
 * - 1-Tap Diacritic Upgrade Strip when the last typed letter has accented variants (e.g. Á, É, Ñ, Ç)
 * - Paged 12-key large-target keyboard with dynamic Next-Letter probability highlighting
 * - 1-Tap "SAVE PHRASE" button to pin newly spelled sentences to the Main Screen
 */
@Composable
fun AccessibleSpellingView(
    uiState: SpeechUiState,
    onSelectWord: (String) -> Unit,
    onTypeLetter: (String) -> Unit,
    onSelectDiacritic: (String) -> Unit,
    onSpace: () -> Unit,
    onSavePhrase: () -> Unit,
    onNextKeyboardPage: () -> Unit
) {
    val settings = uiState.motorSettings
    val strings = uiState.languageMetadata.uiStrings
    val topPredictions = uiState.predictions.take(4)
    val pageLetters = uiState.currentKeyboardPageLetters
    val diacritics = uiState.activeDiacriticVariants

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. 4 Oversized Word Predictions (2x2 Grid)
        Column(
            modifier = Modifier.fillMaxWidth().height(122.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (r in 0 until 2) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (c in 0 until 2) {
                        val word = topPredictions.getOrNull(r * 2 + c)
                        if (word != null) {
                            AccessibleCardButton(
                                onClick = { onSelectWord(word) },
                                settings = settings,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                backgroundColor = Color(0xFF1E3A8A),
                                borderColor = Color(0xFF60A5FA),
                                borderWidth = 2.dp
                            ) {
                                Text(
                                    text = word,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            }
        }

        // 2. Diacritic Variant Bar (Appears dynamically when typing a letter with accents like A, E, I, O, U, N, C)
        if (diacritics.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACCENTS:",
                    color = AccentGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                diacritics.forEach { accented ->
                    AccessibleCardButton(
                        onClick = { onSelectDiacritic(accented) },
                        settings = settings,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        backgroundColor = Color(0xFF451A03),
                        borderColor = AccentGold,
                        borderWidth = 2.dp
                    ) {
                        Text(
                            text = accented,
                            color = Color(0xFFFDE68A),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // 3. Paged Large-Target Letter Grid (3 rows x 4 columns = 12 large buttons)
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val rows = 3
            val cols = 4
            for (r in 0 until rows) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (c in 0 until cols) {
                        val letter = pageLetters.getOrNull(r * cols + c)
                        if (letter != null) {
                            val highlight = uiState.letterHighlights[letter]
                            val isTopLikely = highlight?.isTopLikely == true
                            val isPossible = highlight?.isPossible != false

                            val bgColor = when {
                                isTopLikely -> Color(0xFF1E3A8A)
                                isPossible -> CardDark
                                else -> Color(0xFF0F172A)
                            }
                            val borderColor = when {
                                isTopLikely -> AccentGold
                                isPossible -> AccentCyan
                                else -> Color(0xFF1E293B)
                            }
                            val textColor = when {
                                isTopLikely -> Color(0xFFFDE68A)
                                isPossible -> TextLight
                                else -> Color(0xFF475569)
                            }

                            AccessibleCardButton(
                                onClick = { onTypeLetter(letter) },
                                settings = settings,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                backgroundColor = bgColor,
                                borderColor = borderColor,
                                borderWidth = if (isTopLikely) 3.dp else 1.5.dp
                            ) {
                                Text(
                                    text = letter,
                                    color = textColor,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            }
        }

        // 4. Spacebar + Letter Page Switcher + 1-Tap Save Phrase Row
        Row(
            modifier = Modifier.fillMaxWidth().height(54.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AccessibleCardButton(
                onClick = onNextKeyboardPage,
                settings = settings,
                modifier = Modifier.weight(1.05f).fillMaxHeight(),
                backgroundColor = Color(0xFF334155),
                borderColor = AccentCyan
            ) {
                Text(
                    text = "🔤 ${uiState.keyboardPageIndex + 1}/${uiState.totalKeyboardPages} ▶",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            AccessibleCardButton(
                onClick = onSpace,
                settings = settings,
                modifier = Modifier.weight(1.4f).fillMaxHeight(),
                backgroundColor = AccentBlue,
                borderColor = Color(0xFF93C5FD)
            ) {
                Text(
                    text = "␣ ${strings.addWordSpace}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (uiState.displaySentence.isNotBlank()) {
                AccessibleCardButton(
                    onClick = onSavePhrase,
                    settings = settings,
                    modifier = Modifier.weight(1.1f).fillMaxHeight(),
                    backgroundColor = CardShortcutDark,
                    borderColor = AccentEmerald
                ) {
                    Text(
                        text = "⭐ ${strings.savePhrase}",
                        color = Color(0xFFA7F3D0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Caregiver & Clinical Configuration View (Protected behind 3-second hold lock):
 * 1. Multi-Language Pack Selector + Offline TTS Voice Verification & Grammar Profile (Gender & Formality)
 * 2. Motor Accessibility, Handedness & 1-Switch Auto-Scan Settings
 * 3. Add Custom Phrase directly to Root or ANY Starter Branch + Pin as Priority
 * 4. Profile Backup Export & Restore (JSON Clipboard / Share)
 * 5. Pin / Unpin or Delete Existing Nodes
 */
@Composable
fun CaregiverManagementView(
    uiState: SpeechUiState,
    onSwitchLanguage: (String) -> Unit,
    onLaunchTtsInstaller: () -> Unit,
    onPreviewVoice: () -> Unit,
    onUpdateMotorSettings: (MotorAccessSettings, Boolean) -> Unit,
    onAddCustomNode: (String, String?, String, Boolean) -> Unit,
    onTogglePin: (PhraseNodeEntity) -> Unit,
    onDeleteNode: (PhraseNodeEntity) -> Unit,
    onExportProfile: () -> String,
    onImportProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var phraseText by remember { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf<String?>(null) }
    var selectedEmoji by remember { mutableStateOf("💬") }
    var pinNewPhrase by remember { mutableStateOf(true) }
    var backupJsonInput by remember { mutableStateOf("") }
    var backupStatus by remember { mutableStateOf<String?>(null) }

    val starterBranches = remember(uiState.allNodes) {
        uiState.allNodes.filter { it.parentConceptId == null && it.nodeType == NodeType.STARTER_BRANCH }
    }

    val settings = uiState.motorSettings

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // SECTION 1: LANGUAGE PACK, GRAMMAR PROFILE & OFFLINE TTS VOICE
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, AccentEmerald)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. LANGUAGE PACK, GRAMMAR & VOICE",
                        color = Color(0xFFA7F3D0),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text("Active Language Pack (Shares Concept-ID History):", color = TextMuted, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.availableLocales.forEach { (code, name) ->
                            FilterChip(
                                selected = uiState.locale == code,
                                onClick = { onSwitchLanguage(code) },
                                label = { Text("$name (${code.uppercase()})", fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.isTtsVoiceReady) "✅ TTS Engine Ready (${uiState.availableVoices.size} voices)" else "⚠️ Offline Voice Missing for ${uiState.locale.uppercase()}",
                            color = if (uiState.isTtsVoiceReady) Color(0xFF4ADE80) else AccentGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = onPreviewVoice,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                        ) {
                            Text("🔊 Test Voice", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedButton(onClick = onLaunchTtsInstaller) {
                            Text("📥 Voices", color = TextLight, fontSize = 12.sp)
                        }
                    }

                    Text("Speaker Voice & Grammatical Gender (Auto-Selects Male/Female Voice + Grammar):", color = TextMuted, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpeakerGender.entries.forEach { g ->
                            FilterChip(
                                selected = settings.speakerGender == g,
                                onClick = {
                                    // Reset explicit voice override so engine automatically picks matching Male/Female voice & plays preview
                                    onUpdateMotorSettings(
                                        settings.copy(speakerGender = g, selectedVoiceId = null),
                                        true
                                    )
                                },
                                label = { Text(g.label, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    if (uiState.availableVoices.isNotEmpty()) {
                        Text("Installed Device Voices (Tap any voice to switch & preview):", color = TextMuted, fontSize = 13.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = settings.selectedVoiceId == null,
                                    onClick = {
                                        onUpdateMotorSettings(settings.copy(selectedVoiceId = null), true)
                                    },
                                    label = { Text("✨ Auto (${settings.speakerGender.label})") }
                                )
                            }
                            items(uiState.availableVoices, key = { it.id }) { voiceOpt ->
                                FilterChip(
                                    selected = settings.selectedVoiceId == voiceOpt.id ||
                                        (settings.selectedVoiceId == null && uiState.activeVoiceId == voiceOpt.id),
                                    onClick = {
                                        onUpdateMotorSettings(
                                            settings.copy(
                                                selectedVoiceId = voiceOpt.id,
                                                speakerGender = if (voiceOpt.detectedGender != SpeakerGender.NEUTRAL) voiceOpt.detectedGender else settings.speakerGender
                                            ),
                                            true
                                        )
                                    },
                                    label = { Text(voiceOpt.label) }
                                )
                            }
                        }
                    }

                    Text("Voice Pitch / Tone Depth (Works on all devices even with 1 voice file):", color = TextMuted, fontSize = 13.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(
                            listOf(
                                0.72f to "Deep Male (0.72x)",
                                0.85f to "Low (0.85x)",
                                1.0f to "Normal (1.0x)",
                                1.15f to "Higher (1.15x)"
                            )
                        ) { (pitchVal, label) ->
                            FilterChip(
                                selected = kotlin.math.abs(settings.speechPitch - pitchVal) < 0.05f,
                                onClick = {
                                    onUpdateMotorSettings(settings.copy(speechPitch = pitchVal), true)
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    Text("Tone / Formality:", color = TextMuted, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Formality.entries.forEach { f ->
                            FilterChip(
                                selected = settings.formality == f,
                                onClick = { onUpdateMotorSettings(settings.copy(formality = f), false) },
                                label = { Text(f.label) }
                            )
                        }
                    }

                    Text("Voice Speed:", color = TextMuted, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.70f to "Slow (0.7x)", 0.85f to "Clear (0.85x)", 1.05f to "Natural (1.05x)").forEach { (rate, label) ->
                            FilterChip(
                                selected = kotlin.math.abs(settings.speechRate - rate) < 0.05f,
                                onClick = { onUpdateMotorSettings(settings.copy(speechRate = rate), true) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        // SECTION 2: MOTOR ACCESSIBILITY, REACH ZONE & 1-SWITCH SCANNING
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, AccentAmber)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "2. MOTOR ACCESSIBILITY, REACH & SWITCH SCANNING",
                        color = Color(0xFFFDE68A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text("Primary Reach Zone (Handedness):", color = TextMuted, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Handedness.entries.forEach { hand ->
                            FilterChip(
                                selected = settings.handedness == hand,
                                onClick = { onUpdateMotorSettings(settings.copy(handedness = hand), false) },
                                label = { Text(hand.label) }
                            )
                        }
                    }

                    Text("Tremor Double-Tap Lockout (Debounce):", color = TextMuted, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(150L to "Light (150ms)", 350L to "Medium (350ms)", 650L to "Strong (650ms)").forEach { (ms, label) ->
                            FilterChip(
                                selected = settings.debounceMs == ms,
                                onClick = { onUpdateMotorSettings(settings.copy(debounceMs = ms), false) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Text("Accidental Brush Protection (Minimum Press Hold):", color = TextMuted, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0L to "Instant (0ms)", 70L to "Standard (70ms)", 180L to "Firm Hold (180ms)").forEach { (ms, label) ->
                            FilterChip(
                                selected = settings.minHoldMs == ms,
                                onClick = { onUpdateMotorSettings(settings.copy(minHoldMs = ms), false) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-Speak Complete Phrases on 1 Tap", color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = settings.autoSpeakComplete,
                            onCheckedChange = { onUpdateMotorSettings(settings.copy(autoSpeakComplete = it), false) }
                        )
                    }

                    HorizontalDivider(color = BorderDark)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("1-Switch / Auto-Scan Highlight Mode", color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Cycles focus ring automatically for single-button or switch users", color = TextMuted, fontSize = 12.sp)
                        }
                        Switch(
                            checked = settings.autoScanEnabled,
                            onCheckedChange = { onUpdateMotorSettings(settings.copy(autoScanEnabled = it), false) }
                        )
                    }

                    if (settings.autoScanEnabled) {
                        Text("Auto-Scan Step Speed:", color = TextMuted, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1500L to "Fast (1.5s)", 2200L to "Medium (2.2s)", 3200L to "Slow (3.2s)").forEach { (ms, label) ->
                                FilterChip(
                                    selected = settings.scanIntervalMs == ms,
                                    onClick = { onUpdateMotorSettings(settings.copy(scanIntervalMs = ms), false) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // SECTION 3: ADD CUSTOM PHRASE TO ANY TREE BRANCH
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, AccentCyan)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "3. ADD CUSTOM PHRASE TO TREE OR MAIN SCREEN",
                        color = AccentCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    OutlinedTextField(
                        value = phraseText,
                        onValueChange = { phraseText = it },
                        label = { Text("Phrase or branch continuation text") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = BorderDark
                        )
                    )

                    Text("Where should this phrase appear?", color = TextMuted, fontSize = 13.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedParentId == null,
                                onClick = { selectedParentId = null },
                                label = { Text("🏠 Main Screen (1-Tap)") }
                            )
                        }
                        items(starterBranches) { branch ->
                            FilterChip(
                                selected = selectedParentId == branch.conceptId,
                                onClick = { selectedParentId = branch.conceptId },
                                label = { Text("${branch.iconEmoji} ${branch.text}…") }
                            )
                        }
                    }

                    Text("Choose Icon:", color = TextMuted, fontSize = 13.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("💬", "❤️", "💧", "💊", "🚻", "🛏️", "🪑", "📞", "🍲", "📺", "🆘")) { emoji ->
                            FilterChip(
                                selected = selectedEmoji == emoji,
                                onClick = { selectedEmoji = emoji },
                                label = { Text(emoji, fontSize = 18.sp) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("★ Pin as Priority (Boost to Page 1)", color = TextLight, fontSize = 14.sp)
                        Switch(
                            checked = pinNewPhrase,
                            onCheckedChange = { pinNewPhrase = it }
                        )
                    }

                    Button(
                        onClick = {
                            if (phraseText.isNotBlank()) {
                                onAddCustomNode(phraseText, selectedParentId, selectedEmoji, pinNewPhrase)
                                phraseText = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("➕ SAVE PHRASE TO VOCABULARY", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
            }
        }

        // SECTION 4: BACKUP & RESTORE USER PROFILE
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "4. BACKUP & RESTORE USER PROFILE (JSON)",
                        color = TextLight,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val json = onExportProfile()
                                clipboard.setText(AnnotatedString(json))
                                backupStatus = "Copied profile backup JSON to clipboard!"
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                        ) {
                            Text("📋 Copy Backup", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val json = onExportProfile()
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_TEXT, json)
                                    putExtra(Intent.EXTRA_SUBJECT, "Assisted Speech AAC Profile Backup")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share AAC Profile Backup"))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                        ) {
                            Text("📤 Share Backup", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedTextField(
                        value = backupJsonInput,
                        onValueChange = { backupJsonInput = it },
                        label = { Text("Paste backup JSON here to restore") },
                        modifier = Modifier.fillMaxWidth().height(84.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = BorderDark
                        )
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                clipboard.getText()?.text?.let { backupJsonInput = it }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Paste from Clipboard", color = TextLight, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (backupJsonInput.isNotBlank()) {
                                    onImportProfile(backupJsonInput)
                                    backupStatus = "Profile restored!"
                                    backupJsonInput = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                        ) {
                            Text("📥 Restore JSON", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (backupStatus != null) {
                        Text(text = backupStatus!!, color = Color(0xFF4ADE80), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // SECTION 5: PIN / MANAGE EXISTING PHRASES
        item {
            Text(
                text = "5. VOCABULARY & PRIORITY PINS (TAP ★ TO BOOST)",
                color = TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(uiState.allNodes, key = { it.id }) { node ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (node.isPinned) AccentGold else BorderDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${node.iconEmoji} ${node.text}",
                            color = TextLight,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Used ${node.usageCount}x • Parent: ${node.parentConceptId ?: "Root"}",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onTogglePin(node) }) {
                            Icon(
                                imageVector = if (node.isPinned) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Toggle Pin",
                                tint = if (node.isPinned) AccentGold else TextMuted
                            )
                        }
                        if (node.isCustom) {
                            IconButton(onClick = { onDeleteNode(node) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = AccentRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
