package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.example.survey.*
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers="w360dp-h780dp-mdpi",sdk=[36])
class SurveyTest {
    @get:Rule val compose=createComposeRule()
    private fun fullAnswers() = defaults()+mapOf("Q01" to "35","Q02" to "Не указывать","Q03" to "11–15 лет",
        "Q04" to "20","Q05" to "2","Q06" to "Иногда","Q07" to "240,50","Q10" to "Через 6–15 минут",
        "Q13" to "3–5 раз","Q14" to "1–2 раза","Q15" to "Определюсь позже")
    @Test fun validation_and_conditional_answers() {
        val a=fullAnswers()
        (0..6).forEach { assertTrue("Page $it",errors(it,a).isEmpty()) }
        assertTrue(errors(0,a+mapOf("Q01" to "8")).containsKey("Q03"))
        assertTrue(errors(1,a-"Q06").containsKey("Q06"))
        assertTrue(errors(1,(a-"Q06")+("Q05" to "1")).isEmpty())
        assertTrue(errors(2,a+("Q07" to "0")).containsKey("Q07"))
        assertTrue(errors(5,a+("Q14" to "Более 10 раз")).containsKey("Q14"))
        assertTrue(errors(6,a+("Q15" to "Сейчас")).containsKey("Q15"))
        assertTrue(errors(3,a+mapOf("wake" to "23:00","sleep" to "07:00")).isEmpty())
    }
    @Test fun all_groups_next_back_and_finish() {
        val state = androidx.compose.runtime.mutableStateOf(SurveyState(fullAnswers(), loaded=true))
        var completed=false
        compose.setContent { SurveyTheme {
            SurveyContent(state.value,{_,_->},{_,_->},{state.value=state.value.copy(page=it)},{completed=true},{})
        } }
        for (p in 0..5) {
            compose.onRoot().captureRoboImage("build/reports/survey/group-${p+1}.png")
            compose.onNodeWithText("Далее").performClick()
            compose.runOnIdle { assertEquals(p+1,state.value.page) }
        }
        compose.onNodeWithText("Назад").performClick()
        compose.runOnIdle { assertEquals(5,state.value.page) }
        compose.onNodeWithText("Далее").performClick()
        compose.onRoot().captureRoboImage("build/reports/survey/date.png")
        compose.onNodeWithText("Завершить").performClick()
        compose.runOnIdle { assertTrue(completed) }
    }
    @Test fun database_survives_close_and_reopen() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val name="survey-persistence-test.db"
        context.deleteDatabase(name)
        var db=Room.databaseBuilder(context,SurveyDatabase::class.java,name).build()
        val draft=SurveyState(fullAnswers(),page=4,loaded=true)
        db.survey().save(encode(draft)); db.close()
        db=Room.databaseBuilder(context,SurveyDatabase::class.java,name).build()
        assertEquals(draft.answers,decode(db.survey().load()).answers)
        assertEquals(4,decode(db.survey().load()).page)
        db.survey().save(encode(draft.copy(page=7,completed=true))); db.close()
        db=Room.databaseBuilder(context,SurveyDatabase::class.java,name).build()
        assertTrue(decode(db.survey().load()).completed)
        assertEquals(15,questions.size)
        db.close();context.deleteDatabase(name); Unit
    }
    @Test fun first_page_validation_and_layout() {
        var destination=-1
        compose.setContent { SurveyTheme {
            SurveyContent(SurveyState(loaded=true),{_,_->},{_,_->},{destination=it},{},{})
        } }
        compose.onNodeWithText("Далее").performClick()
        compose.onNodeWithText("Введите число от 1 до 120").assertExists()
        assertEquals(-1,destination)
        compose.onRoot().captureRoboImage("build/reports/survey/first.png")
    }
    @Test fun summary_lists_answers_and_edit() {
        var destination=-1
        compose.setContent { SurveyTheme {
            SurveyContent(SurveyState(fullAnswers(),7,true,true),{_,_->},{_,_->},{destination=it},{},{})
        } }
        compose.onNodeWithText("Ответы сохранены на устройстве").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/reports/survey/summary.png")
        compose.onNodeWithText("Изменить ответы").performClick()
        assertEquals(0,destination)
    }
}
