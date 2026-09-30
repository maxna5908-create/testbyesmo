package com.example.survey

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Calendar
import kotlinx.coroutines.launch

private val Red = Color(0xFFFF2B3D)
private val Panel = Color(0xFF373F47)
@Composable
fun SurveyScreen(vm: SurveyViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    SurveyTheme {
        SurveyContent(state, vm::answer, vm::dateAnswer, vm::page, vm::finish, vm::retry)
    }
}
@Composable
fun SurveyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary=Red, background=Color(0xFF292D32),surface=Panel,
        onPrimary=Color.White,onBackground=Color.White,onSurface=Color.White)) {
        CompositionLocalProvider(LocalContentColor provides Color.White) { content() }
    }
}

@Composable
fun SurveyContent(state:SurveyState, answer:(String,String)->Unit, dateAnswer:(String,String?)->Unit,
                  page:(Int)->Unit, finish:()->Unit, retry:()->Unit) {
    val context=LocalContext.current
    val focus=LocalFocusManager.current
    var attempted by remember(state.page) { mutableStateOf(false) }
    val scroll=rememberScrollState()
    val scope=rememberCoroutineScope()
    LaunchedEffect(state.page) { scroll.scrollTo(0) }
    val a=state.answers
    val issues=if(attempted) errors(state.page,a) else emptyMap()
    val progress by animateFloatAsState(if(state.page==7) 1f else state.page / 7f,label="survey progress")
    BackHandler(enabled=state.loaded && state.page in 1..6) { focus.clearFocus();page(state.page-1) }
    fun chooseTime(key:String) {
        val parts=a[key].orEmpty().split(':')
        TimePickerDialog(context,{_,h,m->answer(key,"%02d:%02d".format(h,m))},
            parts.getOrNull(0)?.toIntOrNull()?:7,parts.getOrNull(1)?.toIntOrNull()?:0,true).show()
    }
    fun chooseDate(mode:String) {
        val c=Calendar.getInstance()
        a["quitInstant"]?.toLongOrNull()?.let { c.timeInMillis=it }
        if(mode=="Завтра") { c.timeInMillis=System.currentTimeMillis(); c.add(Calendar.DAY_OF_YEAR,1) }
        val time={ TimePickerDialog(context,{_,h,m ->
            c.set(Calendar.HOUR_OF_DAY,h);c.set(Calendar.MINUTE,m);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0)
            dateAnswer(mode,c.timeInMillis.toString())
        },c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),true).show(); Unit }
        if(mode=="Завтра") time() else DatePickerDialog(context,{_,y,m,d ->
            c.set(y,m,d);time()
        },c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show()
    }
    Column(Modifier.fillMaxSize().background(Color(0xFF292D32)).safeDrawingPadding().imePadding().padding(horizontal=24.dp,vertical=16.dp)) {
        Text("Первичный опрос",style=MaterialTheme.typography.titleLarge,modifier=Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().height(6.dp),color=Red,trackColor=Panel)
        Spacer(Modifier.height(10.dp))
        Text(if(state.page==7) "Все ответы" else "Шаг ${state.page+1} из 7 · ${groups[state.page]}",color=Color(0xFFD1D3D6))
        state.error?.let { Text(it,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(top=12.dp)); if(state.loaded) TextButton(onClick=retry){Text("Повторить сохранение")} }
        if(!state.loaded) { Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){ if(state.error==null) CircularProgressIndicator(color=Red) };return@Column }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(vertical=16.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            if(state.page==7) {
                Text(if(state.saving) "Сохраняем ответы…" else if(state.error==null) "Ответы сохранены на устройстве" else "Ответы пока не сохранены",color=Color(0xFFD1D3D6))
                questions.forEach { (key,title) ->
                    Surface(shape=RoundedCornerShape(18.dp),color=Panel,modifier=Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text(title,style=MaterialTheme.typography.titleMedium)
                            Text(summaryValue(key,a),color=Color(0xFFD1D3D6))
                        }
                    }
                }
            } else when(state.page) {
                0 -> {
                    Question("Q01",issues) { NumberField("Q01",a,answer,"Полных лет") }
                    Question("Q02",issues, "Только для аватара. Если пол не указан, используется мужской аватар.") { Choices("Q02",listOf("Мужской","Женский","Не указывать"),a,answer) }
                    Question("Q03",issues) { Choices("Q03",years,a,answer) }
                }
                1 -> {
                    Question("Q04",issues) { NumberField("Q04",a,answer,"Сигарет в день") }
                    Question("Q05",issues) { Choices("Q05",listOf("1","2","3"),a,answer) }
                    if(a["Q05"] in listOf("2","3")) Question("Q06",issues) { Choices("Q06",listOf("Всегда","Иногда","Периодически","Редко"),a,answer) }
                }
                2 -> {
                    Question("Q07",issues) {
                        NumberField("Q07",a,answer,"Цена пачки",true)
                        Spacer(Modifier.height(8.dp))
                        Choices("currency",listOf("RUB","EUR","USD","BYN","KZT"),a,answer)
                    }
                    Question("Q08",issues) { NumberField("Q08",a,answer,"Сигарет в пачке") }
                }
                3 -> {
                    Question("Q09",issues,"Можно указать ночной режим: окончание раньше начала означает следующий день.") {
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick={chooseTime("wake")},modifier=Modifier.weight(1f)){Text("С ${a["wake"]}")}
                            OutlinedButton(onClick={chooseTime("sleep")},modifier=Modifier.weight(1f)){Text("До ${a["sleep"]}")}
                        }
                    }
                    Question("Q10",issues) { Choices("Q10",listOf("Сразу после пробуждения","Через 1–5 минут","Через 6–15 минут","Через 16–30 минут","Через 31–60 минут","Более чем через 1 час"),a,answer) }
                }
                4 -> {
                    Question("Q11",issues) { Choices("Q11",listOf("Утром","Днём","Вечером","Равномерно в течение дня"),a,answer) }
                    if(a["Q11"]!="Равномерно в течение дня") Question("Q12",issues,
                        "${a["Q11"]}: на сколько больше по сравнению с остальными частями дня?") { Choices("Q12",listOf("10%","15%","20%","25%","30%","40%","50%"),a,answer) }
                }
                5 -> {
                    Question("Q13",issues) { Choices("Q13",attempts,a,answer) }
                    Question("Q14",issues) { Choices("Q14",attempts.take((attempts.indexOf(a["Q13"])+1).coerceAtLeast(1)),a,answer) }
                }
                6 -> Question("Q15",issues,"Можно выбрать прошедшую дату. Дата и время будут сохранены для счётчиков.") {
                    Choices("Q15",listOf("Сейчас","Завтра","Указать дату","Определюсь позже"),a) {_,v ->
                        when(v) {
                            "Сейчас" -> dateAnswer(v,System.currentTimeMillis().toString())
                            "Определюсь позже" -> dateAnswer(v,null)
                            else -> chooseDate(v)
                        }
                    }
                    if(a["quitInstant"]!=null) Text(summaryValue("Q15",a),modifier=Modifier.padding(top=12.dp),color=Color.White)
                }
            }
        }
        if(state.page==7) {
            OutlinedButton(onClick={page(0)},modifier=Modifier.fillMaxWidth()){Text("Изменить ответы")}
        } else Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            if(state.page>0) OutlinedButton(onClick={focus.clearFocus();page(state.page-1)},modifier=Modifier.weight(1f)){Text("Назад")}
            Button(onClick={
                focus.clearFocus();attempted=true
                if(errors(state.page,a).isEmpty()) { if(state.page==6) finish() else page(state.page+1) }
                else scope.launch { scroll.animateScrollTo(0) }
            },modifier=Modifier.weight(1f),enabled=state.error==null,colors=ButtonDefaults.buttonColors(containerColor=Red,contentColor=Color.White)) {
                Text(if(state.page==6) "Завершить" else "Далее")
            }
        }
    }
}
@Composable private fun Question(key:String, errors:Map<String,String>,hint:String?=null,content:@Composable ColumnScope.()->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(questions.getValue(key),style=MaterialTheme.typography.titleMedium)
        hint?.let { Text(it,style=MaterialTheme.typography.bodySmall,color=Color(0xFFBEC4CB)) }
        content()
        errors[key]?.let { Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall) }
    }
}
@Composable private fun NumberField(key:String,a:Map<String,String>,change:(String,String)->Unit,label:String,decimal:Boolean=false) {
    OutlinedTextField(value=a[key].orEmpty(),onValueChange={v->
        if(v.length<=10 && v.all { it.isDigit() || (decimal && (it=='.'||it==',')) }) change(key,v)
    },modifier=Modifier.fillMaxWidth(),label={Text(label)},singleLine=true,
        keyboardOptions=KeyboardOptions(keyboardType=if(decimal) KeyboardType.Decimal else KeyboardType.Number))
}
@Composable private fun Choices(key:String,options:List<String>,a:Map<String,String>,change:(String,String)->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            Surface(shape=RoundedCornerShape(14.dp),color=if(a[key]==option) Color(0xFF50343C) else Panel,
                modifier=Modifier.fillMaxWidth().clickable {change(key,option)}) {
                Row(Modifier.padding(horizontal=8.dp,vertical=2.dp),verticalAlignment=Alignment.CenterVertically) {
                    RadioButton(selected=a[key]==option,onClick=null,modifier=Modifier.padding(10.dp))
                    Text(option,modifier=Modifier.padding(top=10.dp,bottom=10.dp,end=12.dp))
                }
            }
        }
    }
}
