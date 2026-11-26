package com.jarvis.ai

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tts: TextToSpeech
    private lateinit var log: TextView
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private var tamil = true

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
    ) { r ->
        val t = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!t.isNullOrBlank()) handle(t)
    }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { s ->
            if (s == TextToSpeech.SUCCESS) tts.language = Locale("en", "IN")
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
        scroll = ScrollView(this).apply { addView(log) }
        input = EditText(this).apply {
            hint = "Type pannunga..."
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
                val t = input.text.toString().trim()
                input.setText("")
                if (t.isNotEmpty()) handle(t)
            }
        }
        val langBtn = Button(this).apply {
            text = "Mic: Tamil"
            setOnClickListener {
                tamil = !tamil
                text = if (tamil) "Mic: Tamil" else "Mic: English"
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
        setContentView(root)
        say("Hello Sir, naan Jarvis. Epdi help pannanum?")
    }

    private fun listen() {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (tamil) "ta-IN" else "en-IN")
        try {
            micLauncher.launch(i)
        } catch (e: Exception) {
            say("Intha phone-la speech support illa Sir. Type pannunga.")
        }
    }

    private fun say(t: String) {
        log.append("\nJarvis: $t\n")
        scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
        tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun mem(): MutableList<String> {
        val s = getSharedPreferences("jarvis", MODE_PRIVATE).getString("mem", "") ?: ""
        return s.split("|").filter { it.isNotBlank() }.toMutableList()
    }

    private fun saveMem(l: List<String>) {
        getSharedPreferences("jarvis", MODE_PRIVATE).edit()
            .putString("mem", l.joinToString("|")).apply()
    }

    private fun launch(i: Intent, name: String) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
            say("Sari Sir, $name open pannitten.")
        } catch (e: Exception) {
            say("$name open panna mudiyala Sir.")
        }
    }

    private fun handle(q: String) {
        log.append("\nYou: $q\n")
        val l = q.lowercase(Locale.getDefault()).replace(Regex("[.?!,]"), "").trim()

        Regex("""(?:remember|ninaivula vechuko|note panniko)\s+(?:that\s+)?(.+)""").matchEntire(l)?.let {
            val m = mem()
            m.add(it.groupValues[1])
            saveMem(m)
            say("Sari Sir, ninaivula vechukkaren: ${it.groupValues[1]}")
            return
        }
        if (Regex("""(forget everything|clear memory|memory clear|ellam marandhidu)""").containsMatchIn(l)) {
            saveMem(emptyList())
            say("Memory ellam clear pannitten Sir.")
            return
        }
        if (Regex("""(what do you remember|show memory|memory kaattu|enna ninaivula)""").containsMatchIn(l)) {
            val m = mem()
            say(if (m.isEmpty()) "Memory empty Sir." else "Ninaivula irukku: " + m.joinToString(". "))
            return
        }
        if (Regex("""(what('s| is)? (the )?(time|date|day)|what time|current (time|date)|time enna|date enna|neram enna|eththana mani|indru enna|inniku enna)""").containsMatchIn(l)) {
            val f = SimpleDateFormat("EEEE, d MMMM yyyy, h:mm a", Locale.ENGLISH)
            say("Ippo " + f.format(Date()) + " Sir.")
            return
        }
        Regex("""(?:call|dial)\s+([\d\s+]{6,15})""").matchEntire(l)?.let {
            launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + it.groupValues[1].replace(" ", ""))), "Dialer")
            return
        }
        Regex("""(?:play|paadu)\s+(.+)""").matchEntire(l)?.let {
            val u = "https://www.youtube.com/results?search_query=" + Uri.encode(it.groupValues[1])
            launch(Intent(Intent.ACTION_VIEW, Uri.parse(u)), "YouTube")
            return
        }
        Regex("""(?:search|google)\s+(.+)""").matchEntire(l)?.let {
            val u = "https://www.google.com/search?q=" + Uri.encode(it.groupValues[1])
            launch(Intent(Intent.ACTION_VIEW, Uri.parse(u)), "Google search")
            return
        }
        val m = Regex("""(?:open|launch|start)\s+(.+)""").matchEntire(l)
            ?: Regex("""(.+?)\s+(?:open pannu|open pannunga|thira|thirandhu)""").matchEntire(l)
        if (m != null) {
            val app = m.groupValues[1].removePrefix("the ").removeSuffix(" app").trim()
            when {
                app == "settings" -> launch(Intent(Settings.ACTION_SETTINGS), "Settings")
                app == "camera" -> launch(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), "Camera")
                app == "phone" || app == "dialer" -> launch(Intent(Intent.ACTION_DIAL), "Dialer")
                apps.containsKey(app) -> launch(Intent(Intent.ACTION_VIEW, Uri.parse(apps[app])), app)
                else -> say("Antha app enaku theriyala Sir. \"search $app\" nu sollunga.")
            }
            return
        }
        say("Idhu enaku theriyala Sir. AI brain (Claude) Phase 2-la add pannuvom. Ippo time, date, open app, call, play, search, remember ellam work aagum.")
    }

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }
}
