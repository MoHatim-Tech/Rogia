package com.mohatim.rogia

import android.os.Bundle
import android.widget.Toast
import android.content.Context
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mohatim.rogia.ui.theme.MyApplicationTheme
import com.mohatim.rogia.ui.theme.TajawalFontFamily

enum class AppTab {
    EVALUATION, // التقييم الذاتي
    LIBRARY,    // ركن الرقية
    DEVELOPER   // المطور نبذة عن التطبيق
}

class MainViewModel : ViewModel() {
    var currentTab by mutableStateOf(AppTab.EVALUATION)

    // Quiz states
    var currentQuestionIndex by mutableStateOf(0)
    val answers = mutableStateMapOf<Int, Int>() // Question ID -> Selected Point (0 to 3)
    var isEvaluationCompleted by mutableStateOf(false)

    // Results calculations
    var topRecommendationCategory by mutableStateOf("")
    var recommendationTitle by mutableStateOf("")
    var recommendationReasoning by mutableStateOf("")
    var scoreEyeEnvy by mutableStateOf(0)
    var scoreMagicTouch by mutableStateOf(0)
    var scoreStressSad by mutableStateOf(0)
    var totalScore by mutableStateOf(0)

    // Library tracking: item_id -> current progress count
    val readingProgress = mutableStateMapOf<String, Int>()
    var selectedRuqyahGroup by mutableStateOf<RuqyahGroup?>(null)

    fun resetEvaluation() {
        answers.clear()
        currentQuestionIndex = 0
        isEvaluationCompleted = false
        topRecommendationCategory = ""
        recommendationTitle = ""
        recommendationReasoning = ""
        scoreEyeEnvy = 0
        scoreMagicTouch = 0
        scoreStressSad = 0
        totalScore = 0
    }

    fun answerQuestion(points: Int) {
        val question = RuqyahData.questions[currentQuestionIndex]
        answers[question.id] = points

        if (currentQuestionIndex < RuqyahData.questions.size - 1) {
            currentQuestionIndex++
        } else {
            calculateResult()
        }
    }

    fun previousQuestion() {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--
        }
    }

    private fun calculateResult() {
        var localEye = 0
        var localMagic = 0
        var localStress = 0
        var localTotal = 0

        for (question in RuqyahData.questions) {
            val pts = answers[question.id] ?: 0
            localTotal += pts
            localEye += pts * (question.categoryWeights[SymptomCategory.EYE_ENVY] ?: 0)
            localMagic += pts * (question.categoryWeights[SymptomCategory.MAGIC_TOUCH] ?: 0)
            localStress += pts * (question.categoryWeights[SymptomCategory.STRESS_SAD] ?: 0)
        }

        scoreEyeEnvy = localEye
        scoreMagicTouch = localMagic
        scoreStressSad = localStress
        totalScore = localTotal

        // Determine recommendation
        val highestScore = maxOf(localEye, localMagic, localStress)
        when {
            highestScore == 0 -> {
                topRecommendationCategory = "general"
                recommendationTitle = "الرقية الشرعية العامة وآيات السكينة"
                recommendationReasoning = "أجوبتك تدل على استقرار حالتك الروحية بفضل الله، نوصيك بالاستمرار على أذكار الصباح والمساء والتحصينات اليومية العامة للمحافظة على هذا السلام النفسي."
            }
            localMagic >= localEye && localMagic >= localStress -> {
                topRecommendationCategory = "magic"
                recommendationTitle = "رقية السحر والمس الروحي"
                recommendationReasoning = "تشير الأعراض إلى وجود بعض التقلبات والثقل الروحي. ننصحك بالتركيز الشديد على رقية السحر والمس بنية طرد العارض وحل العُقد المعطلة، والمداومة على سورة البقرة."
            }
            localEye >= localMagic && localEye >= localStress -> {
                topRecommendationCategory = "envy"
                recommendationTitle = "رقية العين والحسد"
                recommendationReasoning = "أجوبتك تشير إلى تأثرك ببعض أعراض النظرة أو الحسد المتراكم في المال أو الجسد، ننصحك برقية العين والحسد مع النفث والدهان بزيت الزيتون المرقي."
            }
            else -> {
                topRecommendationCategory = "sakinah"
                recommendationTitle = "آيات الطمأنينة وفك الكرب"
                recommendationReasoning = "تظهر إجاباتك وجود حالة من الضيق النفسي، الهموم، أو التشتت الفكري. نوصيك بآيات السكينة وشرح الصدر والاستغفار لتفريج هذا الهم والغم فوراً بإذن الله."
            }
        }
        isEvaluationCompleted = true
    }

    fun resetReadingProgress() {
        readingProgress.clear()
    }

    fun incrementProgress(item: RuqyahItem) {
        val current = readingProgress[item.id] ?: 0
        if (current < item.targetCount) {
            readingProgress[item.id] = current + 1
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Outer controller to enforce Right-to-Left (RTL) Arabic layout dir
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val viewModel: MainViewModel = viewModel()
                    var showSplash by remember { mutableStateOf(true) }

                    if (showSplash) {
                        SplashScreen { showSplash = false }
                    } else {
                        MainScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000L)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F5A47)), // Premium Islamic Deep Emerald Green
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Golden circular emblem
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .border(2.dp, Color(0xFFC59F4B), CircleShape)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "رُقية",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC59F4B), // Elegant Gold
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "الرُّقْيَةُ الْوَاقِيَةُ",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "علي منهج الكتاب والسُنَّة",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = Color(0xFFC59F4B),
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Creative header element matching Natural Tones mockup custom avatar/badge
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "م",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 18.sp
                            )
                        }
                        Column {
                            Text(
                                text = "الرُقية الواقية",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 17.sp,
                                lineHeight = 20.sp
                            )
                            Text(
                                text = "منهج الكتاب والسنة",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(
                        modifier = Modifier
                            .size(40.dp)
                            .border(1.dp, MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                            .background(Color.White, CircleShape),
                        onClick = {
                            viewModel.resetEvaluation()
                            viewModel.resetReadingProgress()
                            viewModel.currentTab = AppTab.EVALUATION
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "إعادة التهيئة",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = viewModel.currentTab == AppTab.EVALUATION,
                    onClick = { viewModel.currentTab = AppTab.EVALUATION },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "التقييم تفاعلي"
                        )
                    },
                    label = { Text("التقييم التفاعلي") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationBarItem(
                    selected = viewModel.currentTab == AppTab.LIBRARY,
                    onClick = { viewModel.currentTab = AppTab.LIBRARY },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "ركن الرقية"
                        )
                    },
                    label = { Text("ركن الرقية") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationBarItem(
                    selected = viewModel.currentTab == AppTab.DEVELOPER,
                    onClick = { viewModel.currentTab = AppTab.DEVELOPER },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "معلومات التطبيق"
                        )
                    },
                    label = { Text("معلومات التطبيق") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (viewModel.currentTab) {
                AppTab.EVALUATION -> EvaluationTabScreen(viewModel)
                AppTab.LIBRARY -> LibraryTabScreen(viewModel)
                AppTab.DEVELOPER -> DeveloperTabScreen(viewModel)
            }
        }
    }
}

// Helper class for Option Styling
class OptionDesign(
    val icon: ImageVector,
    val tintColor: Color,
    val bgColors: List<Color>,
    val strokeColor: Color
)

// ==========================================
// SCREEN 1: EVALUATION (Interactive Quiz)
// ==========================================
@Composable
fun EvaluationTabScreen(viewModel: MainViewModel) {
    val context = LocalContext.current

    AnimatedContent(
        targetState = viewModel.isEvaluationCompleted,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "quizTransition"
    ) { completed ->
        if (completed) {
            EvaluationResultScreen(viewModel)
        } else {
            val question = RuqyahData.questions[viewModel.currentQuestionIndex]
            val totalQuestions = RuqyahData.questions.size

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Step & Progress Indicator
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title Row representing deep diagnostic purpose
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "ميزان السكينة والتقييم الشرعي",
                                fontFamily = TajawalFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Premium Step-by-Step Custom Nodes (Horizontal Segmented Bars)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (index in 0 until totalQuestions) {
                                val isActive = index == viewModel.currentQuestionIndex
                                val isCompleted = index < viewModel.currentQuestionIndex
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            when {
                                                isActive -> MaterialTheme.colorScheme.tertiary
                                                isCompleted -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                            }
                                        )
                                )
                            }
                        }

                        // Compact textual statistics
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "السؤال ${viewModel.currentQuestionIndex + 1} من $totalQuestions",
                                fontFamily = TajawalFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "التقدم: ${((viewModel.currentQuestionIndex + 1) * 100 / totalQuestions)}%",
                                fontFamily = TajawalFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // The Question Container Card (Beautiful Spiritual Frame with Gold Accents)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .testTag("question_card_${viewModel.currentQuestionIndex}"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    // Custom visual border accents in the top corners representing luxury Islamic calligraphy art templates
                                    val size = 20f
                                    drawPath(
                                        path = Path().apply {
                                            moveTo(0f, size)
                                            lineTo(0f, 0f)
                                            lineTo(size, 0f)
                                        },
                                        color = Color(0xFFD97706), // Authentic gold
                                        style = Stroke(width = 2.5f)
                                    )
                                    drawPath(
                                        path = Path().apply {
                                            moveTo(this@drawBehind.size.width, size)
                                            lineTo(this@drawBehind.size.width, 0f)
                                            lineTo(this@drawBehind.size.width - size, 0f)
                                        },
                                        color = Color(0xFFD97706),
                                        style = Stroke(width = 2.5f)
                                    )
                                }
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                                            Color.White
                                        )
                                    )
                                )
                                .padding(horizontal = 20.dp, vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Spiritual crest / star icon
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                        CircleShape
                                    )
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = question.text,
                                fontFamily = TajawalFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                lineHeight = 25.sp
                            )
                        }
                    }
                }

                // Interactive Modern Option Cards (Subtle intensity tints)
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val choices = listOf(
                            Triple("دائماً وبشدة", 3, OptionDesign(
                                icon = Icons.Default.Warning,
                                tintColor = Color(0xFFDC2626), // Sharp warning color
                                bgColors = listOf(Color(0xFFFEF2F2), Color(0xFFFEE2E2)),
                                strokeColor = Color(0xFFFCA5A5)
                            )),
                            Triple("أحياناً بوضوح", 2, OptionDesign(
                                icon = Icons.Default.Notifications,
                                tintColor = Color(0xFFD97706), // Warm Alert gold
                                bgColors = listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7)),
                                strokeColor = Color(0xFFFDE68A)
                            )),
                            Triple("نادراً جداً", 1, OptionDesign(
                                icon = Icons.Default.Info,
                                tintColor = Color(0xFF2563EB), // Cool Informative Blue
                                bgColors = listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE)),
                                strokeColor = Color(0xFFBFDBFE)
                            )),
                            Triple("لا أعاني منه مطلقاً", 0, OptionDesign(
                                icon = Icons.Default.CheckCircle,
                                tintColor = Color(0xFF059669), // Serene emerald green (Peace)
                                bgColors = listOf(Color(0xFFECFDF5), Color(0xFFD1FAE5)),
                                strokeColor = Color(0xFFA7F3D0)
                            ))
                        )

                        choices.forEach { (optionLabel, points, design) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.answerQuestion(points)
                                    }
                                    .testTag("choice_btn_$points"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                border = BorderStroke(1.2.dp, design.strokeColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.horizontalGradient(colors = design.bgColors)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Visual Severity Badge Icon
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(Color.White.copy(alpha = 0.85f), CircleShape)
                                                .border(1.dp, design.strokeColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = design.icon,
                                                contentDescription = null,
                                                tint = design.tintColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Text(
                                            text = optionLabel,
                                            fontFamily = TajawalFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Spiritual glowing ring checkmark anchor
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .border(
                                                1.5.dp,
                                                design.tintColor.copy(alpha = 0.5f),
                                                CircleShape
                                            )
                                            .padding(3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Selection inner indicator
                                    }
                                }
                            }
                        }
                    }
                }

                // Back Navigation Button
                item {
                    if (viewModel.currentQuestionIndex > 0) {
                        OutlinedButton(
                            onClick = { viewModel.previousQuestion() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("back_question_btn"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "السابق",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "الرجوع للسؤال السابق",
                                fontFamily = TajawalFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun borderStrokeForOption(points: Int) = androidx.compose.foundation.BorderStroke(
    width = 1.dp,
    color = when (points) {
        3 -> MaterialTheme.colorScheme.primary
        2 -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.primaryContainer
    }
)

// ==========================================
// RESULTS SCREEN (Smart Evaluation outcome)
// ==========================================
@Composable
fun EvaluationResultScreen(viewModel: MainViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            // Elegant Dome shape Canvas or visual header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .padding(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "اكتمل تقييم حالتك الروحية بنجاح",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "النتيجة مبنية على فقه الأعراض الشرعية من الكتاب والسنة الدقيقة",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        item {
            // Diagnostics Breakdown Chart Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "مؤشرات التأثر الروحي والبدني",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Progress bar 1: Eye & Envy
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "العين والحسد", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${viewModel.scoreEyeEnvy} نقاط",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val progressEye = (viewModel.scoreEyeEnvy.toFloat() / 15f).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progressEye },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = Color(0xFFC59F4B),
                            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    }

                    // Progress bar 2: Magic & Touch
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "السحر والمس الروحي", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${viewModel.scoreMagicTouch} نقاط",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val progressMagic = (viewModel.scoreMagicTouch.toFloat() / 15f).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progressMagic },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    }

                    // Progress bar 3: Stress & anxiety
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "الضيق العارض والهموم", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${viewModel.scoreStressSad} نقاط",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val progressStress = (viewModel.scoreStressSad.toFloat() / 12f).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progressStress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }

        item {
            // Recommendation Action Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                ),
                border = borderStrokeForOption(3)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "الرقية الموصى بها لحالتك",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontSize = 14.sp
                    )

                    Text(
                        text = viewModel.recommendationTitle,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 19.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = viewModel.recommendationReasoning,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val targetId = viewModel.topRecommendationCategory
                            val foundGroup = RuqyahData.ruqyahGroups.find { it.id == targetId }
                            if (foundGroup != null) {
                                viewModel.selectedRuqyahGroup = foundGroup
                            } else {
                                viewModel.selectedRuqyahGroup = RuqyahData.ruqyahGroups.first()
                            }
                            viewModel.currentTab = AppTab.LIBRARY
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("go_to_recommended_ruqyah_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "ابدأ الرقة الموصى بها الآن", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            // Self sufficiency and warning reminder
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "تذكير إيماني غاية في الأهمية",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "الرقية ليست حكراً على شيوخ أو أشخاص بعينهم. رقيتك لنفسك هي الأوثق والأكثر قبولاً لصدق لجوئك وتضرعك لله رب العالمين.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            // Re-take quiz Button
            OutlinedButton(
                onClick = { viewModel.resetEvaluation() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("retake_evaluation_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "إجراء الفحص من جديد", fontWeight = FontWeight.Bold)
            }
        }
    }
}


// ==========================================
// SCREEN 2: ALL RUQYAH DIRECT LINKS (Library)
// ==========================================
@Composable
fun LibraryTabScreen(viewModel: MainViewModel) {
    AnimatedContent(
        targetState = viewModel.selectedRuqyahGroup,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "libraryTransition"
    ) { group ->
        if (group == null) {
            // Top library overview list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Title Banner with warm Islamic vector decoration drawn via canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                    )
                                )
                            )
                            .drawBehind {
                                // Draw some stylish Islamic geometric circles decoration behind text
                                val radius = 110f
                                drawCircle(
                                    color = Color(0xFFC59F4B).copy(alpha = 0.15f),
                                    radius = radius,
                                    center = Offset(size.width - 50f, size.height / 2f)
                                )
                                drawCircle(
                                    color = Color(0xFFC59F4B).copy(alpha = 0.12f),
                                    radius = radius - 30f,
                                    center = Offset(size.width - 50f, size.height / 2f)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "حصن المسلم ورقيتك الذاتية",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "تصفح واقرأ الآيات والأدعية الصحيحة على نفسك وأحبائك بكل ثقة وطمأنينة.",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "تصنيفات الرقية الشرعية المتاحة:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Grid/List of categories
                items(RuqyahData.ruqyahGroups) { itemGroup ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectedRuqyahGroup = itemGroup
                            }
                            .testTag("group_card_${itemGroup.id}")
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(24.dp)
                            ),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Circular icon container
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = fetchCategoryIcon(itemGroup.iconName),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = itemGroup.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = itemGroup.subtitle,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "عدد آيات وأوراد القسم: ${itemGroup.items.size}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "تصفح",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Viewing active Ruqyah verses inside selected group
            ActiveReadingScreen(group, viewModel)
        }
    }
}

fun fetchCategoryIcon(name: String): ImageVector {
    return when (name) {
        "healing" -> Icons.Default.Favorite
        "remove_red_eye" -> Icons.Default.Search
        "gavel" -> Icons.Default.Check
        "spa" -> Icons.Default.ThumbUp
        "flame" -> Icons.Default.Warning
        "shield" -> Icons.Default.Lock
        "wb_sunny" -> Icons.Default.Notifications
        else -> Icons.Default.Home
    }
}

// ==========================================
// DETAILED READING SCREEN WITH COUNTERS
// ==========================================
@Composable
fun ActiveReadingScreen(group: RuqyahGroup, viewModel: MainViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Back button and Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = { viewModel.selectedRuqyahGroup = null },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            CircleShape
                        )
                        .testTag("back_to_library_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "عودة",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = group.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "اقرأ وانفث على كفيك أو المريض والماء مباشرة",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        item {
            // Simple informational instruction banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "اضغط على الأزرار الذهبية المخصصة لتسجيل التكرار لكل آية كمسبحة متابعة حتى تتمها بنجاح.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // List each verse/item
        items(group.items) { item ->
            val count = viewModel.readingProgress[item.id] ?: 0
            val isCompleted = count >= item.targetCount

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ruqyah_item_${item.id}"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCompleted) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isCompleted) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Item top info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = item.title,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Complete tick or state string
                        if (isCompleted) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "مكتمل",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "تم",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "متبقي: ${item.targetCount - count} مرات",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Quran text in stylized frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                0.5.dp,
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.content,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Source and instructions footer
                    Text(
                        text = "المصدر: ${item.source}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 14.sp
                    )

                    // Interactive Counter Button of the individual item
                    Button(
                        onClick = {
                            viewModel.incrementProgress(item)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("indicator_btn_${item.id}"),
                        enabled = !isCompleted,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            disabledContentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تمت القراءة بالكامل ($count من ${item.targetCount})",
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "اضغط لتسجيل قراءة ($count من ${item.targetCount})",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==========================================
// SCREEN 3: DEVELOPER PROFILE & APP SUMMARY
// ==========================================
@Composable
fun DeveloperTabScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("rogia_prefs", Context.MODE_PRIVATE) }
    var notificationsEnabled by remember {
        mutableStateOf(sharedPref.getBoolean("notifications_enabled", false))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            notificationsEnabled = true
            sharedPref.edit().putBoolean("notifications_enabled", true).apply()
            NotificationReceiver.scheduleNextNotification(context)
            Toast.makeText(context, "تم تفعيل التنبيهات بنجاح كل ١٠ دقائق", Toast.LENGTH_SHORT).show()
        } else {
            notificationsEnabled = false
            sharedPref.edit().putBoolean("notifications_enabled", false).apply()
            Toast.makeText(context, "يجب إعطاء الإذن لتلقي التنبيهات الدورية", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Aesthetic Top Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "عن التطبيق والمطور",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 19.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "منهج الكتاب والسنة النبوية الشريفة",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Toggles / Notification Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("notifications_timer_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "تنبيهات الذكر والصلاة على النبي",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "تنبيه تلقائي هادئ كل ١٠ دقائق",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val isGranted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (isGranted) {
                                            notificationsEnabled = true
                                            sharedPref.edit().putBoolean("notifications_enabled", true).apply()
                                            NotificationReceiver.scheduleNextNotification(context)
                                            Toast.makeText(context, "تم تفعيل التنبيهات بنجاح كل ١٠ دقائق", Toast.LENGTH_SHORT).show()
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    } else {
                                        notificationsEnabled = true
                                        sharedPref.edit().putBoolean("notifications_enabled", true).apply()
                                        NotificationReceiver.scheduleNextNotification(context)
                                        Toast.makeText(context, "تم تفعيل التنبيهات بنجاح كل ١٠ دقائق", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    notificationsEnabled = false
                                    sharedPref.edit().putBoolean("notifications_enabled", false).apply()
                                    NotificationReceiver.cancelNotifications(context)
                                    Toast.makeText(context, "تم إيقاف التنبيهات التلقائية", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    Text(
                        text = "يعمل هذا التنبيه في الخلفية ليقوم بعرض ذكر أو إستغفار أو صلاة على النبي ﷺ ليعمر قلبك ولسانك بذكر الله عز وجل بصورة دورية.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }

        // App Summary (نبذة عن التطبيق) Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "نبذة عن التطبيق",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Divider(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    Text(
                        text = "تطبيق الرُقية الواقية هو رفيقك الإيماني الشرعي ومجموعتك الشاملة لتحصين النفس والبيت وعلاج الأمراض الروحية والبدنية بالاستعانة بالقرآن الكريم والأذكار الثابتة عن النبي صلى الله عليه وسلم. يتميز التطبيق بتقديم تقييم تفاعلي لحالتك لمساعدتك في معرفة الرقية المناسبة لظروفك، وتشجيع المداومة على الذكر عبر نظام عداد وسير مسبحة وتنبيهات دورية هادئة كل ١٠ دقائق.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Justify
                    )
                    Text(
                        text = "غايتنا الكبرى هي نشر منهج السنة الصحيحة والتحصين الشرعي الذاتي لكل مسلم ومسلمة، وتثقيف المجتمع ليرقي كل مريض نفسه بيقين كامل وثقة تامة بالله دون الحاجة للجوء للسحرة والدجالين والمشعوذين.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Justify
                    )
                }
            }
        }

        // Developer Info Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("developer_info_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "بيانات المطور والاتصال",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Divider(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))

                    // Name row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "الاسم",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "اسم المطور",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "عبدالمنعم حاتم",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Email row (Clickable)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                        data = android.net.Uri.parse("mailto:info@mohatim.tech")
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "تطبيق الرقية الواقية")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "لا يوجد تطبيق بريد إلكتروني مثبت", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "البريد الإلكتروني",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "البريد الإلكتروني",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "info@mohatim.tech",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary, // highlighted interaction color
                                modifier = Modifier.testTag("dev_email_text")
                            )
                        }
                    }

                    // Phone row (Clickable)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                        data = android.net.Uri.parse("tel:+966544451878")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "عذراً، تعذر فتح لوحة الاتصال", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "رقم الجوال",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "رقم الجوال",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "+966 544451878",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary, // highlighted interaction color
                                modifier = Modifier.testTag("dev_phone_text")
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}


