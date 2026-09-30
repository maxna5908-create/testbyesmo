package com.example.survey

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

@Entity(tableName = "assessment_session")
data class SurveyRecord(
    @PrimaryKey val id: String = "primary",
    val questionnaireVersion: Int = 1,
    val answersJson: String = "{}",
    val page: Int = 0,
    val completed: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)
@Dao interface SurveyDao {
    @Query("SELECT * FROM assessment_session WHERE id = 'primary'") suspend fun load(): SurveyRecord?
    @Upsert suspend fun save(record: SurveyRecord)
}
@Database(entities = [SurveyRecord::class], version = 1, exportSchema = true)
abstract class SurveyDatabase : RoomDatabase() {
    abstract fun survey(): SurveyDao
    companion object {
        @Volatile private var instance: SurveyDatabase? = null
        fun get(context: Context): SurveyDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, SurveyDatabase::class.java, "byesmo.db")
                .build().also { instance = it }
        }
    }
}
data class SurveyState(
    val answers: Map<String, String> = defaults(), val page: Int = 0,
    val completed: Boolean = false, val loaded: Boolean = false,
    val saving: Boolean = false, val error: String? = null,
)
fun defaults() = mapOf("Q08" to "20", "wake" to "07:00", "sleep" to "23:00", "currency" to "RUB", "Q11" to "Равномерно в течение дня")
fun decode(record: SurveyRecord?): SurveyState {
    if (record == null) return SurveyState(loaded = true)
    val json = JSONObject(record.answersJson)
    val answers = defaults().toMutableMap()
    json.keys().forEach { answers[it] = json.getString(it) }
    return SurveyState(answers, record.page.coerceIn(0, 7), record.completed, loaded = true)
}
fun encode(state: SurveyState) = SurveyRecord(answersJson = JSONObject(state.answers).toString(), page = state.page, completed = state.completed)

class SurveyViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = SurveyDatabase.get(application).survey()
    private val mutable = MutableStateFlow(SurveyState())
    val state = mutable.asStateFlow()
    private val writes = Channel<SurveyState>(Channel.UNLIMITED)
    init {
        viewModelScope.launch {
            try { mutable.value = decode(dao.load()) }
            catch (_: Exception) { mutable.value = SurveyState(error = "Не удалось прочитать сохранённые ответы. Перезапустите приложение.") }
        }
        viewModelScope.launch {
            for (snapshot in writes) {
                try {
                    dao.save(encode(snapshot))
                    if (mutable.value.answers == snapshot.answers && mutable.value.page == snapshot.page && mutable.value.completed == snapshot.completed)
                        mutable.value = mutable.value.copy(saving = false, error = null)
                } catch (_: Exception) {
                    mutable.value = mutable.value.copy(saving = false, error = "Не удалось сохранить ответы. Повторите сохранение.")
                }
            }
        }
    }
    private fun update(value: SurveyState) {
        mutable.value = value.copy(saving = true, error = null)
        writes.trySend(value)
    }
    fun answer(key: String, value: String) {
        val answers = mutable.value.answers.toMutableMap().apply { put(key, value) }
        if (key == "Q05" && value == "1") answers.remove("Q06")
        if (key == "Q11" && value == "Равномерно в течение дня") answers.remove("Q12")
        if (key == "Q13") {
            if (value == "Ни разу") answers["Q14"] = "Ни разу"
            else if (attempts.indexOf(answers["Q14"]) > attempts.indexOf(value)) answers.remove("Q14")
        }
        update(mutable.value.copy(answers = answers, completed = false))
    }
    fun dateAnswer(mode: String, instant: String?) {
        val a = mutable.value.answers.toMutableMap().apply {
            put("Q15", mode)
            if (instant == null) remove("quitInstant") else put("quitInstant", instant)
        }
        update(mutable.value.copy(answers = a, completed = false))
    }
    fun page(page: Int) = update(mutable.value.copy(page = page))
    fun finish() { if ((0..6).all { errors(it, mutable.value.answers).isEmpty() }) update(mutable.value.copy(page = 7, completed = true)) }
    fun retry() { if (mutable.value.loaded) update(mutable.value) }
}
