package com.example.survey

val groups = listOf("О вас", "Ваше курение", "Расходы", "Режим дня", "Ритм курения", "Попытки отказа", "Дата отказа")
val attempts = listOf("Ни разу", "1–2 раза", "3–5 раз", "6–10 раз", "Более 10 раз")
val questions = linkedMapOf(
    "Q01" to "Ваш возраст?", "Q02" to "Ваш пол?", "Q03" to "Сколько лет курите?",
    "Q04" to "Сколько сигарет вы курите в день?", "Q05" to "Сколько сигарет в среднем выкуриваете за раз?",
    "Q06" to "Как часто курите несколько сигарет за раз?", "Q07" to "Сколько стоит пачка сигарет?",
    "Q08" to "Сколько сигарет содержится в вашей пачке?", "Q09" to "Ваше время бодрствования?",
    "Q10" to "Через какое время после пробуждения вы закуриваете первую сигарету?",
    "Q11" to "В какое время дня вы курите больше?", "Q12" to "На сколько процентов больше?",
    "Q13" to "Сколько раз вы пробовали бросить курить?", "Q14" to "Сколько попыток за последний год?",
    "Q15" to "Укажите дату отказа от курения для старта счётчиков")
val years = listOf("1 год", "2–3 года", "4–5 лет", "6–10 лет", "11–15 лет", "16–20 лет", "Более 20 лет")
fun errors(page: Int, a: Map<String,String>): Map<String,String> {
    val e = mutableMapOf<String,String>()
    fun required(k:String) { if(a[k].isNullOrBlank()) e[k] = "Выберите ответ" }
    fun number(k:String, range:IntRange) { if(a[k]?.toIntOrNull() !in range) e[k] = "Введите число от ${range.first} до ${range.last}" }
    when(page) {
        0 -> { number("Q01",1..120); required("Q02"); required("Q03")
            val minimum = listOf(1,2,4,6,11,16,21).getOrNull(years.indexOf(a["Q03"]))
            if(minimum != null && (a["Q01"]?.toIntOrNull() ?: 120) <= minimum) e["Q03"] = "Стаж должен быть меньше возраста"
        }
        1 -> { number("Q04",1..200); required("Q05"); if(a["Q05"] != "1") required("Q06")
            if((a["Q05"]?.toIntOrNull() ?: 0) > (a["Q04"]?.toIntOrNull() ?: 200)) e["Q05"] = "Количество за раз не должно превышать количество за день"
        }
        2 -> { val price = a["Q07"]?.replace(',','.')?.toBigDecimalOrNull(); if(price == null || price.signum() <= 0 || price > java.math.BigDecimal("1000000") || price.scale()>2) e["Q07"] = "Введите цену больше 0, не более двух знаков после запятой"; number("Q08",1..200) }
        3 -> { required("Q10"); if(a["wake"] == a["sleep"]) e["Q09"] = "Начало и конец бодрствования должны отличаться" }
        4 -> { required("Q11"); if(a["Q11"] != "Равномерно в течение дня") required("Q12") }
        5 -> { required("Q13"); required("Q14"); if(attempts.indexOf(a["Q14"]) > attempts.indexOf(a["Q13"])) e["Q14"] = "За год не может быть больше попыток, чем всего" }
        6 -> { required("Q15"); if(a["Q15"] != "Определюсь позже" && a["quitInstant"].isNullOrBlank()) e["Q15"] = "Укажите дату и время" }
    }
    return e
}
fun summaryValue(key:String,a:Map<String,String>):String = when(key) {
    "Q06" -> if(a["Q05"]=="1") "Не требуется (по одной сигарете)" else a[key].orEmpty()
    "Q07" -> "${a[key].orEmpty()} ${a["currency"]}"
    "Q09" -> "${a["wake"]}–${a["sleep"]}"
    "Q12" -> if(a["Q11"]=="Равномерно в течение дня") "Не требуется (равномерно)" else "${a["Q11"]}: на ${a[key]} больше"
    "Q15" -> if(a[key]=="Определюсь позже") a[key]!! else a["quitInstant"]?.toLongOrNull()?.let { java.text.SimpleDateFormat("dd.MM.yyyy HH:mm",java.util.Locale("ru")).format(java.util.Date(it)) } ?: "Не указано"
    else -> a[key].orEmpty().ifBlank { "Не указано" }
}
