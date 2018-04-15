package com.esperantajvortaroj.app

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.AsyncTask
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.SpannableString
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.activity_definition.*

class DefinitionActivity : AppCompatActivity(), View.OnTouchListener {
    private val textSize = 18f
    private var entriesList: ArrayList<Int> = arrayListOf()
    private var entryPosition = 0
    private var wordId = 0
    private var articleId = 0
    private var showArticle = false

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_definition)
        progressBar.visibility = View.GONE
        setSupportActionBar(appToolbar)
        supportActionBar?.setDefaultDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        wordId = intent.getIntExtra(WORD_ID, 0)
        articleId = intent.getIntExtra(ARTICLE_ID, 0)
        val entryPosition = intent.getIntExtra(ENTRY_POSITION, 0)
        val entriesList = intent.extras.getIntegerArrayList(ENTRIES_LIST)
        this.entryPosition = entryPosition
        this.entriesList = entriesList ?: arrayListOf()

        displayArticleAndWord(wordId)
        invalidateOptionsMenu()
    }

    override fun onTouch(view: View?, motionEvent: MotionEvent?): Boolean {
        if(motionEvent?.action != MotionEvent.ACTION_UP) {
            return false
        }
        if(view == null || view !is TextView){
            return false
        }

        /*
        val duration = motionEvent.eventTime - motionEvent.downTime
        if(duration > 1000){
            return false
        }
        */

        val layout = view.layout
        val line = layout.getLineForVertical(motionEvent.y.toInt())
        val offset = layout.getOffsetForHorizontal(line, motionEvent.x)

        // don't interfere with ClickableSpan
        val clickableSpans = (view.text as SpannableString).getSpans(offset, offset, ClickableSpan::class.java)
        if(clickableSpans.isNotEmpty()){
            return false
        }

        progressBar.visibility = View.VISIBLE

        val word = Utils.getWholeWord(view.text, offset)
        if(word != null){
            SearchWordTask(this).execute(word)
        }
        return true
    }

    fun hideProgressBar(){
        progressBar.visibility = View.GONE
    }

    private class SearchWordTask(val context: DefinitionActivity) : AsyncTask<String, Void, SearchResult?>() {
        var baseWord: String? = null

        override fun doInBackground(vararg params: String?): SearchResult? {
            if(params.isEmpty()) return null
            val word = params[0] ?: return null
            baseWord = word

            val words = Utils.getPossibleBaseWords(word)
            val databaseHelper = DatabaseHelper(context)
            try{
                for(possibleWord in words){
                    val results = databaseHelper.searchWords(possibleWord, true)
                    if(results.isNotEmpty()){
                        // TODO show popup to select between results
                        val result = results[0]
                        val intent = Intent(context, DefinitionActivity::class.java)
                        if(result.id > 0) {
                            intent.putExtra(DefinitionActivity.WORD_ID, result.id)
                            intent.putExtra(DefinitionActivity.ARTICLE_ID, result.articleId)
                            intent.putExtra(DefinitionActivity.ENTRY_POSITION, 0)
                            context.startActivity(intent)
                        }
                        return result
                    }
                }
            } finally {
                databaseHelper.close()
            }
            return null
        }

        override fun onPostExecute(result: SearchResult?) {
            if(result == null){
                Toast.makeText(context, "Vorto '$baseWord' ne trovita", Toast.LENGTH_SHORT).show()
            }
            context.hideProgressBar()
        }
    }

    private fun displayArticleAndWord(wordId: Int) {
        val wordInfo = loadWord(wordId)
        val wordView = wordInfo.first
        val articleId = wordInfo.second
        val articleViews = loadArticle(articleId, wordId)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.addView(wordView)
        if(articleViews.isNotEmpty()){
            layout.addView(articleHeader(wordId))
            if(showArticle){
                layout.addView(articleSeparator())
                for(view in articleViews){
                    layout.addView(view)
                }
            }
        }

        with(definitionScrollView){
            removeAllViews()
            addView(layout)
        }
    }

    private fun articleSeparator(): View {
        val view = View(this)
        view.minimumHeight = 1
        view.setBackgroundColor(Color.GRAY)
        return view
    }

    private fun articleHeader(wordId: Int): TextView {
        val textView = TextView(this)
        val text: SpannableString
        if(showArticle)
            text = SpannableString("\nKaŝi artikolon\n")
        else
            text = SpannableString("\nMontri artikolon\n")
        text.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, 0)
        textView.text = text
        textView.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
        textView.setOnClickListener {
            showArticle = !showArticle
            displayArticleAndWord(wordId)
        }
        return textView
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.entry_menu, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if(menu != null){
            val prevButton = menu.findItem(R.id.prev_entry)
            val nextButton = menu.findItem(R.id.next_entry)

            prevButton.isEnabled = entryPosition != 0
            nextButton.isEnabled = entryPosition < entriesList.size - 1
            prevButton.icon.alpha = if(prevButton.isEnabled) 255 else 130
            nextButton.icon.alpha = if(nextButton.isEnabled) 255 else 130
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.prev_entry -> {
                if(entryPosition == 0){
                    return true
                }
                entryPosition--
                displayArticleAndWord(entriesList[entryPosition])
                invalidateOptionsMenu()
                return true
            }
            R.id.next_entry -> {
                if(entryPosition >= entriesList.size - 1){
                    return true
                }
                entryPosition++
                displayArticleAndWord(entriesList[entryPosition])
                invalidateOptionsMenu()
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    private fun loadWord(wordId: Int): Pair<LinearLayout, Int> {
        val databaseHelper = DatabaseHelper(this)
        val wordResult = databaseHelper.wordById(wordId)

        val pair = getTextViewOfWord(wordResult)
        val textView = pair.first
        var content = pair.second

        content = addTranslations(databaseHelper, wordId, content)
        databaseHelper.close()
        textView.text = content

        textView.setOnTouchListener(this)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.addView(textView)
        return Pair(layout, wordResult?.articleId ?: 0)
    }

    private fun loadArticle(articleId: Int, wordId: Int): List<View> {
        val databaseHelper = DatabaseHelper(this)
        val results = databaseHelper.articleById(articleId)

        if (results.size == 1){
            return emptyList()
        }

        return results.map { res ->
            val pair = getTextViewOfWord(res)
            val textView = pair.first
            var content = pair.second

            content = addTranslations(databaseHelper, res.id, content)
            textView.text = TextUtils.concat(content, "\n")
            textView.setOnTouchListener(this)
            if(res.id == wordId) textView.setBackgroundColor(Color.parseColor("#dddddd"))
            textView
        }
    }

    private fun addTranslations(databaseHelper: DatabaseHelper, wordId: Int, content: CharSequence): CharSequence {
        var content1 = content
        val translationsByLang = getTranslations(databaseHelper, wordId)
        val langNames = databaseHelper.getLanguagesHash()
        if (translationsByLang.isNotEmpty()) {
            val translationsTitle = SpannableString("\n\nTradukoj")
            translationsTitle.setSpan(UnderlineSpan(), 0, translationsTitle.length, 0)
            content1 = TextUtils.concat(content1, translationsTitle)
            for (langEntry in langNames) {
                val translations = translationsByLang.get(langEntry.key)
                if (translations != null) {
                    val lang = langEntry.value
                    content1 = TextUtils.concat(
                            content1,
                            "\n\n• ", lang, "j: ",
                            translations.joinToString(", ") { it.translation })
                }

            }
        }
        return content1
    }

    private fun getTextViewOfWord(wordResult: SearchResult?): Pair<TextView, CharSequence> {
        var content : CharSequence = ""
        val textView = TextView(this)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
        textView.setTextColor(Color.BLACK)
        //textView.setTextIsSelectable(true)
        textView.movementMethod = LinkMovementMethod.getInstance()
        if (wordResult != null) {
            val word = SpannableString(wordResult.word)
            word.setSpan(StyleSpan(Typeface.BOLD), 0, wordResult.word.length, 0)
            content = TextUtils.concat(word, "\n", wordResult.formattedDefinition(this, {
                fako -> showDisciplineDialog(fako)
            }, { stilo -> showStyleDialog(stilo)}))
        }
        return Pair(textView, content)
    }

    private fun getTranslations(databaseHelper: DatabaseHelper, wordId: Int): LinkedHashMap<String, List<TranslationResult>> {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val langPrefs = sharedPref.getStringSet(SettingsActivity.KEY_LANGUAGES_PREFERENCE, null)

        val translationsByLang = LinkedHashMap<String, List<TranslationResult>>()
        for (lang in langPrefs) {
            val translations = databaseHelper.translationsByWordId(wordId, lang)
            if (translations.isNotEmpty()) {
                translationsByLang.put(lang, translations)
            }
        }
        return translationsByLang
    }

    private fun showDisciplineDialog(code: String) {
        val databaseHelper = DatabaseHelper(this)
        val description = databaseHelper.getDiscipline(code)
        val builder = AlertDialog.Builder(this)
        val dialog = builder.setMessage(description).setTitle(code)
                .setPositiveButton(R.string.close_dialog, null)
                .create()
        dialog.show()
    }

    private fun showStyleDialog(code: String) {
        val styles = hashMapOf(
            "FRAZ" to "frazaĵo",
            "FIG" to "figure",
            "VULG" to "vulgare",
            "RAR" to "malofte",
            "POE" to "poezie",
            "ARK" to "arkaismo",
            "EVI" to "evitinde",
            "KOMUNE" to "komune",
            "NEO" to "neologismo"
        )
        val description = styles.get(code.trim()) ?: code
        val builder = AlertDialog.Builder(this)
        val dialog = builder.setMessage(description).setTitle(code)
                .setPositiveButton(R.string.close_dialog, null)
                .create()
        dialog.show()
    }


    companion object {
        const val WORD_ID = "word_id"
        const val ARTICLE_ID = "article_id"
        const val ENTRY_POSITION = "entry_position"
        const val ENTRIES_LIST = "entries_list"
    }
}

