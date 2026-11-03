package com.coverit.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import com.coverit.app.data.Record
import com.coverit.app.doc.DocType
import com.coverit.app.ui.App
import com.coverit.app.ui.AppLanguage
import com.coverit.app.ui.AppActions
import com.coverit.app.ui.LocalAppLanguage
import com.coverit.app.ui.MainViewModel
import com.coverit.app.ui.exportName
import com.coverit.app.ui.theme.CoverItTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private val pickPdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::open) }

    private val pickToClean = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { vm.cleanHidden(it) }

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let(vm::open) }

    private var pendingSave: File? = null

    private var language by mutableStateOf(AppLanguage.SYSTEM)

    private val savers = DocType.entries.associateWith { type ->
        registerForActivityResult(ActivityResultContracts.CreateDocument(type.mime)) { uri -> writeSaved(uri) }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        language = AppLanguage.read(this)
        if (savedInstanceState == null) handleIncoming(intent)
        val actions = AppActions(
            openPdf = { pickPdf.launch(DocType.pickable) },
            openPhoto = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            cleanFiles = { pickToClean.launch(DocType.pickable) },
            share = ::share,
            shareMany = ::shareMany,
            save = ::save,
            openUrl = ::openUrl,
            setLanguage = { language ->
                AppLanguage.write(this, language)
                this.language = language
            },
        )
        setContent {
            // Like the theme: the screen as it was fades out over the same screen in the new
            // language, so words change softly and nothing else moves.
            var shown by remember { mutableStateOf(language) }
            val layer = rememberGraphicsLayer()
            var before by remember { mutableStateOf<ImageBitmap?>(null) }
            val fade = remember { Animatable(0f) }
            LaunchedEffect(language) {
                if (shown == language) return@LaunchedEffect
                before = runCatching { layer.toImageBitmap() }.getOrNull()
                fade.snapTo(1f)
                shown = language
                fade.animateTo(0f, tween(320, easing = LinearOutSlowInEasing))
                before = null
            }
            val context = remember(shown) { AppLanguage.localized(this, shown) }
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides context.resources.configuration,
                LocalResources provides context.resources,
                LocalAppLanguage provides shown,
            ) {
                CoverItTheme(vm.prefs.theme) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        Box(
                            Modifier.fillMaxSize().drawWithContent {
                                layer.record { this@drawWithContent.drawContent() }
                                drawLayer(layer)
                            },
                        ) {
                            App(vm, actions)
                        }
                        before?.let { old ->
                            Image(old, null, Modifier.fillMaxSize().graphicsLayer { alpha = fade.value }, contentScale = ContentScale.FillBounds)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncoming(intent)
    }

    private fun handleIncoming(intent: Intent?) {
        val uri = when (intent?.action) {
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            Intent.ACTION_VIEW -> intent.data
            else -> null
        }
        uri?.let(vm::open)
    }

    private fun share(record: Record, original: Boolean) {
        val source = if (original) vm.library.original(record) else vm.library.clean(record)
        if (source == null) return
        val dir = File(cacheDir, "share").apply { deleteRecursively(); mkdirs() }
        val name = if (original) record.name else exportName(record)
        val file = source.copyTo(File(dir, name.replace('/', '_')), overwrite = true)
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType(record.type.mime)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(send, null))
    }

    private fun shareMany(records: List<Record>) {
        if (records.isEmpty()) return
        if (records.size == 1) return share(records[0], false)
        val dir = File(cacheDir, "share").apply { deleteRecursively(); mkdirs() }
        val uris = ArrayList<Uri>()
        val used = HashSet<String>()
        for (record in records) {
            val source = vm.library.clean(record)
            var name = exportName(record).replace('/', '_')
            var n = 2
            while (!used.add(name)) name = exportName(record).replace('/', '_').let { it.substringBeforeLast('.') + " ($n)." + it.substringAfterLast('.') }.also { n++ }
            uris += FileProvider.getUriForFile(this, "$packageName.files", source.copyTo(File(dir, name), overwrite = true))
        }
        val mime = records.map { it.type.mime }.distinct().singleOrNull() ?: "*/*"
        val send = Intent(Intent.ACTION_SEND_MULTIPLE)
            .setType(mime)
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(send, null))
    }

    private fun save(record: Record) {
        pendingSave = vm.library.clean(record)
        savers.getValue(record.type).launch(exportName(record))
    }

    private fun writeSaved(uri: Uri?) {
        val file = pendingSave ?: return
        pendingSave = null
        if (uri == null) return
        contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
        Toast.makeText(this, AppLanguage.localized(this, language).getString(R.string.saved_toast), Toast.LENGTH_SHORT).show()
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
        }
    }
}
