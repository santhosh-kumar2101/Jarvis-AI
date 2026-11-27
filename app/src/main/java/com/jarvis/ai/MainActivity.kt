package com.jarvis.ai

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val workerUrl =
        "https://empty-cloud-9e93.santhoshkumars0625.workers.dev"

    private val appToken = "J5rv#s_TN62!Ai_6kp@01"

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var tts: TextToSpeech
    private lateinit var log: TextView
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText

    private var tamil = true
    private var provider = "openai"

    private val apps = mapOf(
        "whatsapp" to "https://wa.me/",
        "youtube" to "https://www.youtube.com/",
        "instagram" to "https://www.instagram.com/",
        "maps" to "https://maps.google.com/",
        "gmail" to "https://mail.google.com/",
        "spotify" to "https://open.spotify.com/",
        "telegram" to "https://t.me/",
        "facebook" to "https://www.facebook.com/",
        "amazon" to "https://www.amazon.in/",
        "flipkart" to "https://www.flipkart.com/",
        "google" to "https://www.google.com/",
        "drive" to "https://drive.google.com/",
        "photos" to "https://photos.google.com/",
        "classroom" to "https://classroom.google.com/"
    )

    private val micLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val text = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()

        if (!text.isNullOrBlank()) handle(text)
    }

    private fun dp(n: Int) =
        (n * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale("en", "IN")
            }
        }

        val match = LinearLayout.LayoutParams.MATCH_PARENT

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#050b14"))
            setPadding(dp(12), dp(40), dp(12), dp(12))
        }

        val title = TextView(this).apply {
            text = "J.A.R.V.I.S"
            textSize = 24f
            setTextColor(Color.parseColor("#27d3ff"))
            gravity = Gravity.CENTER
        }

        log = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(dp(4), dp(12), dp(4), dp(12))
        }

        scroll = ScrollView(this).apply {
            addView(log)
        }

        input = EditText(this).apply {
            hint = "Ask Jarvis anything..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
        }

        val micBtn = Button(this).apply {
            text = "Speak"
            setOnClickListener { listen() }
        }

        val sendBtn = Button(this).apply {
            text = "Send"
            setOnClickListener {
                val q = input.text.toString().trim()
                input.setText("")
                if (q.isNotEmpty()) handle(q)
            }
        }

        val langBtn = Button(this).apply {
            text = "Mic: Tamil"
            setOnClickListener {
                tamil = !tamil
                text = if (tamil) "Mic: Tamil" else "Mic: English"
            }
        }

        val aiBtn = Button(this).apply {
            text = "AI: ChatGPT"
            setOnClickListener {
                provider = if (provider == "openai") "claude" else "openai"
                text = if (provider == "openai") "AI: ChatGPT" else "AI: Claude"
                say("AI provider changed.")
            }
        }

        val row = LinearLayout(this).apply {
            addView(micBtn)
            addView(sendBtn)
            addView(langBtn)
        }

        root.addView(title)
        root.addView(scroll, LinearLayout.LayoutParams(match, 0, 1f))
        root.addView(input)
        root.addView(row)
        root.addView(aiBtn)

        setContentView(root)

        say("Hello Sir, naan Jarvis. Epdi help pannanum?")
    }

    private fun listen() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            .putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                if (tamil) "ta-IN" else "en-IN"
            )

        try {
            micLauncher.launch(intent)
        } catch (e: Exception) {
            say("Speech support illa Sir. Type pannunga.")
        }
    }

    private fun say(text: String) {
        log.append("\nJarvis: $text\n")
        scroll.post {
            scroll.fullScroll(ScrollView.FOCUS_DOWN)
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun mem(): MutableList<String> {
        val saved = getSharedPreferences("jarvis", MODE_PRIVATE)
            .getString("mem", "") ?: ""

        return saved.split("|")
            .filter { it.isNotBlank() }
            .toMutableList()
    }

    private fun saveMem(list: List<String>) {
        getSharedPreferences("jarvis", MODE_PRIVATE)
            .edit()
            .putString("mem", list.joinToString("|"))
            .apply()
    }

    private fun launch(intent: Intent, name: String) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            say("Sari Sir, $name open pannitten.")
        } catch (e: Exception) {
            say("$name open panna mudiyala Sir.")
        }
    }

    private fun askAI(question: String) {
        val selectedProvider = provider

        say("Oru nimisham Sir, yosikkiren...")

        executor.execute {
            var connection: HttpURLConnection? = null

            try {
                connection = URL(workerUrl).openConnection()
                    as HttpURLConnection

                connection.requestMethod = "POST"
                connection.connectTimeout = 20000
                connection.readTimeout = 60000
                connection.doOutput = true

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $appToken"
                )

                val body = JSONObject()
                    .put("message", question)
                    .put("provider", selectedProvider)
                    .toString()

                connection.outputStream.use {
                    it.write(body.toByteArray(Charsets.UTF_8))
                }

                val status = connection.responseCode

                val responseText = if (status in 200..299) {
                    connection.inputStream.bufferedReader().use {
                        it.readText()
                    }
                } else {
                    connection.errorStream?.bufferedReader()?.use {
                        it.readText()
                    } ?: "Server error: $status"
                }

                val data = JSONObject(responseText)

                val reply = if (status in 200..299) {
                    data.optString("reply", "No reply received.")
                } else {
                    "AI error: " + data.optString("error", "HTTP $status")
                }

                handler.post {
                    say(reply)
                }

            } catch (e: Exception) {
                handler.post {
                    say("Connection problem Sir: ${e.message}")
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun handle(question: String) {
        log.append("\nYou: $question\n")

        var q = question.lowercase(Locale.getDefault())
            .replace(Regex("[.?!,]"), "")
            .trim()

        if (q.startsWith("claude:")) {
            provider = "claude"
            q = q.removePrefix("claude:").trim()
        } else if (q.startsWith("chatgpt:")) {
            provider = "openai"
            q = q.removePrefix("chatgpt:").trim()
        }

        if (q.isBlank()) return

        Regex(
            """(?:remember|ninaivula vechuko|note panniko)\s+(?:that\s+)?(.+)"""
        ).matchEntire(q)?.let {
            val list = mem()
            list.add(it.groupValues[1])
            saveMem(list)
            say("Sari Sir, ninaivula vechukkaren.")
            return
        }

        if (Regex(
            """(forget everything|clear memory|memory clear|ellam marandhidu)"""
        ).containsMatchIn(q)) {
            saveMem(emptyList())
            say("Memory ellam clear pannitten Sir.")
            return
        }

        if (Regex(
            """(what do you remember|show memory|memory kaattu|enna ninaivula)"""
        ).containsMatchIn(q)) {
            val list = mem()
            say(
                if (list.isEmpty()) "Memory empty Sir."
                else "Ninaivula irukku: " + list.joinToString(". ")
            )
            return
        }

        if (Regex(
            """(what('s| is)? (the )?(time|date|day)|what time|current (time|date)|time enna|date enna|neram enna|eththana mani|indru enna|inniku enna)"""
        ).containsMatchIn(q)) {
            val format = SimpleDateFormat(
                "EEEE, d MMMM yyyy, h:mm a",
                Locale.ENGLISH
            )
            say("Ippo " + format.format(Date()) + " Sir.")
            return
        }

        Regex("""(?:call|dial)\s+([\d\s+]{6,15})""")
            .matchEntire(q)?.let {
                launch(
                    Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse("tel:" + it.groupValues[1].replace(" ", ""))
                    ),
                    "Dialer"
                )
                return
            }

        Regex("""(?:play|paadu)\s+(.+)""")
            .matchEntire(q)?.let {
                val url = "https://www.youtube.com/results?search_query=" +
                    Uri.encode(it.groupValues[1])

                launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)), "YouTube")
                return
            }

        Regex("""(?:search|google)\s+(.+)""")
            .matchEntire(q)?.let {
                val url = "https://www.google.com/search?q=" +
                    Uri.encode(it.groupValues[1])

                launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)), "Google")
                return
            }

        val match = Regex("""(?:open|launch|start)\s+(.+)""")
            .matchEntire(q)
            ?: Regex(
                """(.+?)\s+(?:open pannu|open pannunga|thira|thirandhu)"""
            ).matchEntire(q)

        if (match != null) {
            val app = match.groupValues[1]
                .removePrefix("the ")
                .removeSuffix(" app")
                .trim()

            when {
                app == "settings" ->
                    launch(Intent(Settings.ACTION_SETTINGS), "Settings")

                app == "camera" ->
                    launch(
                        Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
                        "Camera"
                    )

                app == "phone" || app == "dialer" ->
                    launch(Intent(Intent.ACTION_DIAL), "Dialer")

                apps.containsKey(app) ->
                    launch(
                        Intent(Intent.ACTION_VIEW, Uri.parse(apps[app])),
                        app
                    )

                else -> {
                    say("Antha app theriyala Sir.")
                }
            }
            return
        }

        askAI(question)
    }

    override fun onDestroy() {
        executor.shutdown()
        tts.shutdown()
        super.onDestroy()
    }
}
