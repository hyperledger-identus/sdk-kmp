package org.hyperledger.identus.walletsdk.sampleapp.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import org.hyperledger.identus.walletsdk.db.AppDatabase
import org.hyperledger.identus.walletsdk.db.DatabaseClient
import org.hyperledger.identus.walletsdk.edgeagent.EdgeAgent
import org.hyperledger.identus.walletsdk.sampleapp.Sdk
import org.hyperledger.identus.walletsdk.sampleapp.databinding.ActivityMainBinding
import org.hyperledger.identus.walletsdk.sampleapp.db.Message
import org.hyperledger.identus.walletsdk.ui.main.SectionsPagerAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewPager: ViewPager
    private lateinit var tabs: TabLayout
    private lateinit var sectionsPagerAdapter: SectionsPagerAdapter
    private val db: AppDatabase = DatabaseClient.getInstance()
    private var pendingBackupJwe: String? = null
    private val maxRestoreBytes: Long = 25L * 1024L * 1024L // 25 MB safety cap
    private val openBackupFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) {
                Snackbar.make(binding.root, "No file selected", Snackbar.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            restoreFromUri(uri)
        }
    private val saveBackupFile =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/jose")) { uri: Uri? ->
            val jwe = pendingBackupJwe
            pendingBackupJwe = null
            if (uri == null || jwe.isNullOrBlank()) {
                Snackbar.make(binding.root, "Backup cancelled", Snackbar.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(jwe.toByteArray(Charsets.UTF_8))
                        }
                    }
                    val sizeKb = (jwe.length / 1024)
                    Snackbar.make(
                        binding.root,
                        "Backup saved (${sizeKb} KB)",
                        Snackbar.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Snackbar.make(
                        binding.root,
                        "Backup save failed: ${e.message ?: "Unknown error"}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        viewPager = binding.viewPager
        tabs = binding.tabs
        Sdk.getInstance().agentStatusStream().observe(this) {
            if (it == EdgeAgent.State.RUNNING) {
                Snackbar.make(binding.root, "Agent state: $it", Snackbar.LENGTH_LONG).show()
                agentStartedShowViews()
            }
        }

        binding.plutoRestore.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Restore wallet")
                .setMessage("Choose how to restore")
                .setPositiveButton("Select file") { _, _ ->
                    openBackupFile.launch(arrayOf("*/*"))
                }
                .setNeutralButton("Paste JWE") { _, _ ->
                    showRestorePasteDialog()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.plutoBackup.setOnClickListener {
            val progressDialog = AlertDialog.Builder(this@MainActivity)
                .setTitle("Backing up")
                .setMessage("Please wait...")
                .setCancelable(false)
                .show()

            lifecycleScope.launch {
                try {
                    val jweString = withContext(Dispatchers.IO) {
                        Sdk.getInstance().agent.backupWallet()
                    }
                    if (isFinishing || isDestroyed) return@launch
                    progressDialog.dismiss()
                    val sizeKb = (jweString.length / 1024)
                    pendingBackupJwe = jweString
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Backup created")
                        .setMessage("Size: ${sizeKb} KB\nChoose where to save the file.")
                        .setNeutralButton("Copy") { _, _ ->
                            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Backup JWE", jweString))
                            Snackbar.make(binding.root, "Copied to clipboard", Snackbar.LENGTH_LONG).show()
                        }
                        .setPositiveButton("Save") { _, _ ->
                            saveBackupFile.launch("wallet-backup.jwe")
                        }
                        .setNegativeButton("Show") { _, _ ->
                            showBackupTextDialog(jweString)
                        }
                        .setCancelable(true)
                        .show()
                } catch (e: Exception) {
                    if (!isFinishing && !isDestroyed) {
                        progressDialog.dismiss()
                        Snackbar.make(
                            binding.root,
                            "Backup failed: ${e.message ?: "Unknown error"}",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun agentStartedShowViews() {
        viewPager.adapter = sectionsPagerAdapter
        tabs.setupWithViewPager(viewPager)
        val tabIcons = intArrayOf(
            android.R.drawable.ic_menu_call,
            android.R.drawable.ic_menu_myplaces,
            android.R.drawable.ic_dialog_email,
            android.R.drawable.ic_lock_lock,
            android.R.drawable.ic_menu_agenda
        )
        for (i in 0 until tabs.tabCount) {
            tabs.getTabAt(i)?.let { tab ->
                tab.setIcon(tabIcons.getOrNull(i) ?: 0)
                tab.text = null
            }
        }
        binding.agentView.visibility = View.GONE
        binding.viewPager.visibility = View.VISIBLE
    }

    private fun restoreFromUri(uri: Uri) {
        val progressDialog = AlertDialog.Builder(this@MainActivity)
            .setTitle("Restoring")
            .setMessage("Please wait...")
            .setCancelable(false)
            .show()

        lifecycleScope.launch {
            try {
                val jwe = withContext(Dispatchers.IO) {
                    readBackupTextFromUri(uri).trim()
                }
                if (jwe.isBlank()) {
                    progressDialog.dismiss()
                    Snackbar.make(binding.root, "Backup file is empty", Snackbar.LENGTH_LONG).show()
                    return@launch
                }
                withContext(Dispatchers.IO) {
                    Sdk.getInstance().startAgentForBackup(application)
                    Sdk.getInstance().agent.recoverWallet(jwe = jwe)
                    Sdk.getInstance().pluto.getAllMessages().first().forEach {
                        db.messageDao().insertMessage(Message(messageId = it.id, isRead = true))
                    }
                    Sdk.getInstance().stopAgent()
                    startAgentWithStoredMediator()
                }
                if (!isFinishing && !isDestroyed) {
                    progressDialog.dismiss()
                    Snackbar.make(binding.root, "Wallet restored", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Throwable) {
                if (isDatabaseAlreadyRunningError(e)) {
                    if (!isFinishing && !isDestroyed) {
                        progressDialog.dismiss()
                        Snackbar.make(binding.root, "Wallet restored", Snackbar.LENGTH_LONG).show()
                    }
                    return@launch
                }
                if (!isFinishing && !isDestroyed) {
                    progressDialog.dismiss()
                    Snackbar.make(
                        binding.root,
                        "Restore failed: ${e.message ?: "Unknown error"}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showRestorePasteDialog() {
        val input = EditText(this).apply {
            hint = "Paste JWE"
            minLines = 6
        }
        AlertDialog.Builder(this)
            .setTitle("Restore wallet")
            .setView(input)
            .setPositiveButton("Restore") { _, _ ->
                val jwe = input.text.toString().trim()
                if (jwe.isBlank()) {
                    Snackbar.make(binding.root, "JWE is required", Snackbar.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                restoreFromJwe(jwe)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun restoreFromJwe(jwe: String) {
        val progressDialog = AlertDialog.Builder(this@MainActivity)
            .setTitle("Restoring")
            .setMessage("Please wait...")
            .setCancelable(false)
            .show()

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    Sdk.getInstance().startAgentForBackup(application)
                    Sdk.getInstance().agent.recoverWallet(jwe = jwe)
                    Sdk.getInstance().pluto.getAllMessages().first().forEach {
                        db.messageDao().insertMessage(Message(messageId = it.id, isRead = true))
                    }
                    Sdk.getInstance().stopAgent()
                    startAgentWithStoredMediator()
                }
                if (!isFinishing && !isDestroyed) {
                    progressDialog.dismiss()
                    Snackbar.make(binding.root, "Wallet restored", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Throwable) {
                if (isDatabaseAlreadyRunningError(e)) {
                    if (!isFinishing && !isDestroyed) {
                        progressDialog.dismiss()
                        Snackbar.make(binding.root, "Wallet restored", Snackbar.LENGTH_LONG).show()
                    }
                    return@launch
                }
                if (!isFinishing && !isDestroyed) {
                    progressDialog.dismiss()
                    Snackbar.make(
                        binding.root,
                        "Restore failed: ${e.message ?: "Unknown error"}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun readBackupTextFromUri(uri: Uri): String {
        contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val size = pfd.statSize
            if (size > 0 && size > maxRestoreBytes) {
                throw IllegalStateException("Backup is too large (${size / 1024} KB)")
            }
        }
        val builder = StringBuilder()
        val buffer = CharArray(8 * 1024)
        var total = 0L
        contentResolver.openInputStream(uri)?.reader(Charsets.UTF_8)?.use { reader ->
            while (true) {
                val read = reader.read(buffer)
                if (read <= 0) break
                total += read
                if (total > maxRestoreBytes) {
                    throw IllegalStateException("Backup is too large (${total / 1024} KB)")
                }
                builder.append(buffer, 0, read)
            }
        }
        return builder.toString()
    }

    private suspend fun startAgentWithStoredMediator() {
        val sdk = Sdk.getInstance()
        try {
            sdk.startPluto(application)
        } catch (e: Throwable) {
            if (e.javaClass.name !=
                "org.hyperledger.identus.walletsdk.domain.models.PlutoError\$DatabaseServiceAlreadyRunning"
            ) {
                throw e
            }
        }
        val mediator = sdk.pluto.getAllMediators().first().firstOrNull()
            ?: throw IllegalStateException("No mediator stored in wallet")
        sdk.startAgent(mediator.mediatorDID.toString(), application)
    }

    private fun isDatabaseAlreadyRunningError(error: Throwable): Boolean {
        if (error.javaClass.name ==
            "org.hyperledger.identus.walletsdk.domain.models.PlutoError\$DatabaseServiceAlreadyRunning"
        ) {
            return true
        }
        return error.message?.contains("Database service already running", ignoreCase = true) == true
    }

    private fun showBackupTextDialog(jwe: String) {
        val textView = TextView(this@MainActivity).apply {
            text = jwe
            setTextIsSelectable(true)
            movementMethod = ScrollingMovementMethod()
            minLines = 6
        }
        val scrollView = ScrollView(this@MainActivity).apply {
            addView(textView)
        }
        AlertDialog.Builder(this@MainActivity)
            .setTitle("Backup JWE")
            .setView(scrollView)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        Sdk.getInstance().stopAgent()
    }
}
