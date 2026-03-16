package org.example.additional

const val CORRECT_ANSWERS = 3
const val QUESTION_ANSWERS = 4
const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"
const val BOT_FILE_URL = "https://api.telegram.org/file/bot"
const val HELLO_TEXT = """
Привет! 👋🏻

Я - Квокка, и я здесь, чтобы помочь тебе учить английские слова!

Скорее нажимай на кнопку <b>"Учить слова 📚"</b> в основном меню!
"""
val LEARNED_WORD_RULE_TEXT = """
🤖 <b><u>Как работает бот:</u></b>

Я уже добавила несколько случайных слов, чтобы ты мог(ла) сразу начать учиться.
 
🌟 Слово считается выученным, когда ты правильно угадаешь его перевод $CORRECT_ANSWERS раза подряд.
📝 Для того, чтобы добавить новые слова, отправь в бот файл формата <code>.txt</code> со списком твоих слов.

📣 <i>Важно:</i> список слов в файле должен быть описан в виде: 
<code>слово|перевод|0</code>
""".trimIndent()
const val START_BUTTON = "/start"
const val UNDO_COMMAND = "/undo"
const val LEARN_WORDS_CALLBACK = "learn_word_click"
const val STATISTICS_CALLBACK = "statistics_click"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
const val ALL_WORDS_ARE_LEARNED = "Все слова в словаре выучены!"
const val DICTIONARY_EMPTY = "В словаре пока нет слов.\nОтправь боту файл <code>.txt</code> со словами в формате: <code>слово|перевод|0</code>"
const val CORRECT_ANSWER = "✅ Правильно!"
const val EXIT_BUTTON = "exit"
const val EXIT_TEXT = """
Отлично!
Отдыхать тоже нужно! 😌

Возвращайся, когда будешь готов(а) продолжить учиться.
    
Для старта просто нажми 👉🏻/start.
"""
const val PAUSE_IMAGE_FILENAME = "pause.png"
const val START_IMAGE_FILENAME = "start.png"
const val MENU_BUTTON = "menu"
const val MENU_BUTTON_TEXT = "⬅ Назад в меню"
const val RESET_PROGRESS_CALLBACK = "reset_progress"
const val RESET_PROGRESS_TEXT = "Прогресс изучения слов сброшен ✅"
const val CLEAR_DICTIONARY_CALLBACK = "clear_dictionary"
const val CLEAR_DICTIONARY_TEXT = "Словарь очищен и все слова удалены \uD83E\uDDF9\nМожешь загрузить новый файл со словами."
val DEFAULT_DICTIONARY_FILE: String =
    System.getenv("DEFAULT_DICTIONARY_FILE") ?: "word.txt"
const val WORD_MARK_LEARNED_PREFIX = "word_mark_learned|"
const val WORD_RESET_PREFIX = "word_reset|"
val IMAGES_FOLDER: String =
    System.getenv("IMAGES_FOLDER") ?: "images"
val IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png", "gif", "webp")
const val DICTIONARY_SEPARATOR = "|"
val DB_URL: String =
    System.getenv("DB_URL") ?: "jdbc:sqlite:data.db"