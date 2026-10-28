package com.ideasi.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private val ChallengeNavy = Color(0xFF061426)
private val ChallengeGreen = Color(0xFF19D7A0)
private val ChallengeYellow = Color(0xFFFFD43B)
private val ChallengeWhite = Color(0xFFFFFFFF)
private val ChallengeMuted = Color(0xFFB8C7D9)
private val ChallengeSurface = Color(0xFF10243B)
private val ChallengeBorder = Color(0xFF7890AA)

@Composable
fun MathChallengeScreen(
    difficulty: MathDifficulty,
    onCorrectAnswer: () -> Unit
) {
    var question by remember {
        mutableStateOf(generateMathQuestion(difficulty))
    }

    var answerText by remember {
        mutableStateOf("")
    }

    var wrongAnswer by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Motivational mathematics background image.
        Image(
            painter = painterResource(
                id = R.drawable.math_challenge_bg
            ),
            contentDescription = "Motivational mathematics background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Transparent navy overlay to improve text readability.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    ChallengeNavy.copy(alpha = 0.25f)
                )
        )

        // 3. Challenge controls displayed above the background.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Solve to continue",
                style = MaterialTheme.typography.headlineSmall,
                color = ChallengeWhite,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = question.question,
                style = MaterialTheme.typography.headlineMedium,
                color = ChallengeYellow,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 24.dp)
            )

            OutlinedTextField(
                value = answerText,
                onValueChange = { input ->
                    // Accept digits and an optional leading minus sign.
                    if (
                        input.isEmpty() ||
                        input == "-" ||
                        input.matches(Regex("-?\\d+"))
                    ) {
                        answerText = input
                        wrongAnswer = false
                    }
                },
                label = {
                    Text("Answer")
                },
                placeholder = {
                    Text("Enter your answer")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ChallengeWhite,
                    unfocusedTextColor = ChallengeWhite,

                    focusedContainerColor = ChallengeSurface,
                    unfocusedContainerColor = ChallengeSurface,

                    cursorColor = ChallengeGreen,
                    focusedBorderColor = ChallengeGreen,
                    unfocusedBorderColor = ChallengeBorder,

                    focusedLabelColor = ChallengeGreen,
                    unfocusedLabelColor = ChallengeMuted,

                    focusedPlaceholderColor = ChallengeMuted,
                    unfocusedPlaceholderColor = ChallengeMuted
                )
            )

            if (wrongAnswer) {
                Text(
                    text = "Wrong answer. Try another question.",
                    color = Color(0xFFFF7777),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Button(
                onClick = {
                    val userAnswer = answerText.toIntOrNull()

                    if (
                        userAnswer != null &&
                        userAnswer == question.answer
                    ) {
                        onCorrectAnswer()
                    } else {
                        question = generateMathQuestion(difficulty)
                        answerText = ""
                        wrongAnswer = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChallengeGreen,
                    contentColor = ChallengeNavy
                )
            ) {
                Text(
                    text = "Submit",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
