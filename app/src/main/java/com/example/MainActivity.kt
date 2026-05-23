package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.ui.theme.MyApplicationTheme

enum class AppTab {
    EVALUATION, // التقييم الذاتي
    LIBRARY     // ركن الرقية
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
                    MainScreen(viewModel)
                }
            }
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
                            imageVector = if (viewModel.currentTab == AppTab.EVALUATION) Icons.Default.Search else Icons.Default.Search,
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
                            imageVector = Icons.Default.Menu,
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
            }
        }
    }
}

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
            // Quiz step
            val question = RuqyahData.questions[viewModel.currentQuestionIndex]
            val totalQuestions = RuqyahData.questions.size

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    // Header Card with beautiful Natural Tones styling
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Badge matching mockup
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = "تقييم الحالة",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    letterSpacing = 1.sp
                                )
                            }
                            
                            Text(
                                text = "ميزان السكينة والتقييم الشرعي",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 21.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "أجب بصدق لنساعدك في تحديد الرقية الشرعية الأنسب لحالتك لحماية نفسك وعائلتك دون دجالين.",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                item {
                    // Progress Indicator Row
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "السؤال ${viewModel.currentQuestionIndex + 1} من $totalQuestions",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "التقدم: ${((viewModel.currentQuestionIndex + 1) * 100 / totalQuestions)}%",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (viewModel.currentQuestionIndex + 1).toFloat() / totalQuestions },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.tertiary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                }

                item {
                    // Question box with styled Islamic-like arch border
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .testTag("question_card_${viewModel.currentQuestionIndex}"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .drawBehind {
                                    // Custom visual border accents in the top corners to signify authentic Islamic arts
                                    val size = 20f
                                    drawPath(
                                        path = Path().apply {
                                            moveTo(0f, size)
                                            lineTo(0f, 0f)
                                            lineTo(size, 0f)
                                        },
                                        color = Color(0xFFC59F4B),
                                        style = Stroke(width = 3f)
                                    )
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier
                                    .size(36.dp)
                                    .padding(bottom = 6.dp)
                            )

                            Text(
                                text = question.text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                lineHeight = 26.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }
                }

                item {
                    // Interactive Answers
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val choices = listOf(
                            Pair("دائماً وبشدة", 3),
                            Pair("أحياناً بوضوح", 2),
                            Pair("نادراً جداً", 1),
                            Pair("لا أعاني منه مطلقاً", 0)
                        )

                        choices.forEach { (optionLabel, points) ->
                            Button(
                                onClick = {
                                    viewModel.answerQuestion(points)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("choice_btn_$points"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                border = borderStrokeForOption(points)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = optionLabel,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .border(
                                                1.2.dp,
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            )
                                            .padding(3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // A simple stylish core indicator
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    // Back Action
                    if (viewModel.currentQuestionIndex > 0) {
                        TextButton(
                            onClick = { viewModel.previousQuestion() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("back_question_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "السابق"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "الرجوع للسؤال السابق", fontWeight = FontWeight.Bold)
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
        "remove_red_eye" -> Icons.Default.Star
        "gavel" -> Icons.Default.Check
        "spa" -> Icons.Default.ThumbUp
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


