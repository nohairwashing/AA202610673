
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.abs

data class Evaluation(
    val id: Int,
    val name: String,
    val weight: String,
    val score: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                AppEntry()
            }
        }
    }
}

// 앱 시작 화면
@Composable
fun AppEntry() {
    val auth = remember { FirebaseAuth.getInstance() }

    var isLoggedIn by remember {
        mutableStateOf(auth.currentUser != null)
    }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            isLoggedIn = firebaseAuth.currentUser != null
        }

        auth.addAuthStateListener(listener)

        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    if (isLoggedIn) {
        Column(modifier = Modifier.fillMaxSize()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { auth.signOut() }
                ) {
                    Text("로그아웃")
                }
            }

            Box(
                modifier = Modifier.weight(1f)
            ) {
                GradeCalculatorApp()
            }
        }
    } else {
        LoginScreen(auth)
    }
}

// 로그인 및 회원가입 화면
@Composable
fun LoginScreen(auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "A~ 포기할 거에이요?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "아직 포기하기엔 이르다. 남은 점수를 계산해 보자!"
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = if (isRegisterMode) "회원가입" else "로그인",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                message = ""
            },
            label = { Text("이메일") },
            placeholder = { Text("example@email.com") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                message = ""
            },
            label = { Text("비밀번호") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    message = "이메일과 비밀번호를 입력해 주세요."
                } else if (password.length < 6) {
                    message = "비밀번호는 6자 이상 입력해 주세요."
                } else {
                    isLoading = true
                    message = ""

                    val task = if (isRegisterMode) {
                        auth.createUserWithEmailAndPassword(
                            email.trim(),
                            password
                        )
                    } else {
                        auth.signInWithEmailAndPassword(
                            email.trim(),
                            password
                        )
                    }

                    task.addOnCompleteListener { result ->
                        isLoading = false

                        if (!result.isSuccessful) {
                            message = result.exception?.localizedMessage
                                ?: "요청을 처리하지 못했습니다."
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(
                if (isRegisterMode) "회원가입하기" else "로그인하기"
            )
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        TextButton(
            onClick = {
                isRegisterMode = !isRegisterMode
                message = ""
            },
            enabled = !isLoading
        ) {
            Text(
                if (isRegisterMode) {
                    "이미 계정이 있나요? 로그인"
                } else {
                    "계정이 없나요? 회원가입"
                }
            )
        }
    }
}

// 성적 계산 화면
@Composable
fun GradeCalculatorApp() {

    var courseName by remember { mutableStateOf("") }
    var targetGrade by remember { mutableStateOf("A+") }
    var targetScore by remember { mutableStateOf("95") }
    var aRatio by remember { mutableStateOf("40") }

    var nextId by remember { mutableIntStateOf(5) }

    val evaluations = remember {
        mutableStateListOf(
            Evaluation(1, "출석", "20"),
            Evaluation(2, "정기평가", "20"),
            Evaluation(3, "수시평가", "20"),
            Evaluation(4, "실험·실습성과", "40")
        )
    }

    var result by remember { mutableStateOf("") }
    var resultColor by remember { mutableStateOf(false) }

    val totalWeight = evaluations.sumOf {
        it.weight.toDoubleOrNull() ?: 0.0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Text(
            text = "A~ 포기할 거에이요?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "아직 포기하기엔 이르다. 남은 점수를 계산해 보자!",
            style = MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider()

        // 강의 정보
        Text(
            text = "01. 강의 정보",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = courseName,
            onValueChange = {
                courseName = it
                result = ""
            },
            label = { Text("강의 이름") },
            placeholder = { Text("예: 객체지향 프로그래밍") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // 평가 항목
        Text(
            text = "02. 평가 항목 및 반영 비율",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "강의계획서에 표시된 평가 항목과 비율을 입력하세요."
        )

        evaluations.forEachIndexed { index, item ->

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "평가 ${index + 1}",
                            fontWeight = FontWeight.Bold
                        )

                        TextButton(
                            onClick = {
                                evaluations.removeAt(index)
                                result = ""
                            }
                        ) {
                            Text("삭제")
                        }
                    }

                    OutlinedTextField(
                        value = item.name,
                        onValueChange = {
                            evaluations[index] = item.copy(name = it)
                            result = ""
                        },
                        label = { Text("평가 항목 이름") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = item.weight,
                        onValueChange = {
                            evaluations[index] = item.copy(weight = it)
                            result = ""
                        },
                        label = { Text("반영 비율 (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = item.score,
                        onValueChange = {
                            evaluations[index] = item.copy(score = it)
                            result = ""
                        },
                        label = {
                            Text("현재 점수 (모르면 비워두기)")
                        },
                        placeholder = { Text("0~100점") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true
                    )

                    if (item.score.isBlank()) {
                        Text(
                            text = "아직 점수가 확인되지 않았어요.",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = {
                evaluations.add(
                    Evaluation(
                        nextId++,
                        "새 평가 항목",
                        "0"
                    )
                )
                result = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ 평가 항목 추가")
        }

        Text(
            text = "현재 반영 비율 합계: ${"%.1f".format(totalWeight)}%",
            fontWeight = FontWeight.Bold,
            color = if (abs(totalWeight - 100.0) < 0.001)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error
        )

        HorizontalDivider()

        // 목표 학점
        Text(
            text = "03. 목표 학점 설정",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text("목표 학점")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("A+", "A0", "B+", "B0", "C+", "C0").forEach { grade ->

                FilterChip(
                    selected = targetGrade == grade,
                    onClick = {
                        targetGrade = grade
                        result = ""
                    },
                    label = { Text(grade) }
                )
            }
        }

        OutlinedTextField(
            value = targetScore,
            onValueChange = {
                targetScore = it
                result = ""
            },
            label = { Text("목표 학점의 총점 기준") },
            placeholder = { Text("예: 95") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            singleLine = true
        )

        OutlinedTextField(
            value = aRatio,
            onValueChange = {
                aRatio = it
                result = ""
            },
            label = { Text("A학점 부여 비율 (%)") },
            placeholder = { Text("예: 40") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            singleLine = true
        )

        Text(
            text = "※ A학점 부여 비율은 참고 정보이며, 실제 상대평가 결과를 확정하지 않습니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )

        HorizontalDivider()

        // 목표 점수 계산
        Button(
            onClick = {

                val target = targetScore.toDoubleOrNull()
                val ratio = aRatio.toDoubleOrNull()

                val validScores = evaluations.all {
                    it.score.isBlank() ||
                            (it.score.toDoubleOrNull()?.let { score ->
                                score in 0.0..100.0
                            } == true)
                }

                val validWeights = evaluations.all {
                    it.weight.toDoubleOrNull()?.let { weight ->
                        weight in 0.0..100.0
                    } == true
                }

                if (courseName.isBlank()) {
                    result = "강의 이름을 입력해 주세요."
                    resultColor = false

                } else if (evaluations.isEmpty()) {
                    result = "평가 항목을 하나 이상 추가해 주세요."
                    resultColor = false

                } else if (!validWeights || !validScores) {
                    result = "반영 비율과 점수는 0~100 사이의 숫자로 입력해 주세요."
                    resultColor = false

                } else if (evaluations.any { it.name.isBlank() }) {
                    result = "평가 항목 이름을 입력해 주세요."
                    resultColor = false

                } else if (abs(totalWeight - 100.0) > 0.001) {
                    result = "평가 항목의 반영 비율 합계를 100%로 맞춰 주세요."
                    resultColor = false

                } else if (
                    target == null || target !in 0.0..100.0 ||
                    ratio == null || ratio !in 0.0..100.0
                ) {
                    result = "목표 점수와 A학점 비율을 올바르게 입력해 주세요."
                    resultColor = false

                } else {

                    val knownScore = evaluations.sumOf { item ->

                        val weight = item.weight.toDoubleOrNull() ?: 0.0
                        val score = item.score.toDoubleOrNull() ?: 0.0

                        if (item.score.isBlank()) 0.0
                        else weight * score / 100.0
                    }

                    val remainingWeight = evaluations
                        .filter { it.score.isBlank() }
                        .sumOf {
                            it.weight.toDoubleOrNull() ?: 0.0
                        }

                    if (remainingWeight == 0.0) {

                        val message =
                            if (knownScore >= target) {
                                "축하해요! 입력한 점수 기준으로 목표 학점을 달성했어요."
                            } else {
                                "현재 입력된 점수로는 목표 학점에 도달하지 못했어요."
                            }

                        result = """
                            $message

                            현재 총점: ${"%.2f".format(knownScore)}점
                            목표 점수: ${"%.2f".format(target)}점
                        """.trimIndent()

                        resultColor = knownScore >= target

                    } else {

                        val required =
                            (target - knownScore) / (remainingWeight / 100.0)

                        val message = when {
                            required <= 0.0 ->
                                "현재 확인된 점수만으로도 목표 점수에 도달했어요!"

                            required > 100.0 ->
                                "남은 평가에서 모두 100점을 받아도 목표 점수에 도달하기 어려워요."

                            else ->
                                "남은 평가에서 평균 ${"%.2f".format(required)}점이 필요해요."
                        }

                        result = """
                            $courseName

                            목표 학점: $targetGrade
                            목표 총점: ${"%.2f".format(target)}점

                            현재 확보 점수: ${"%.2f".format(knownScore)}점
                            남은 평가 비율: ${"%.1f".format(remainingWeight)}%

                            $message

                            [남은 평가별 목표]
                            ${
                            evaluations
                                .filter { it.score.isBlank() }
                                .joinToString("\n") {
                                    "${it.name}: ${"%.2f".format(required)}점"
                                }
                        }

                            ※ 남은 평가에서 동일한 점수를 받는다고 가정한 예상치입니다.
                        """.trimIndent()

                        resultColor = required <= 100.0
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("목표 학점 계산하기")
        }

        // 계산 결과
        if (result.isNotBlank()) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (resultColor)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "계산 결과",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(result)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
