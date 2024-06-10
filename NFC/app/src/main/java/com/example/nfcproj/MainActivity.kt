package com.example.nfcproj

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.nio.charset.StandardCharsets
import java.util.*

class MainActivity : AppCompatActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var nfcStatus: TextView
    private lateinit var nfcContent: EditText
    private lateinit var writeButton: Button
    private var writeMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nfcStatus = findViewById(R.id.nfcStatus)
        nfcContent = findViewById(R.id.nfcContent)
        writeButton = findViewById(R.id.writeButton)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            nfcStatus.text = "NFC is not available on this device."
            return
        }

        writeButton.setOnClickListener {
            writeMode = true
            nfcStatus.text = "Tap NFC tag to write."
        }
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, filters, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (writeMode && NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            tag?.let {
                if (writeNfcTag(it, nfcContent.text.toString())) {
                    nfcStatus.text = "NFC tag written!"
                } else {
                    nfcStatus.text = "Failed to write NFC tag."
                }
            }
            writeMode = false
        }
    }

    private fun writeNfcTag(tag: Tag, data: String): Boolean {
        val ndef = Ndef.get(tag) ?: return false

        val ndefRecord = createTextRecord("en", data)
        val ndefMessage = NdefMessage(arrayOf(ndefRecord))

        return try {
            ndef.connect()
            if (!ndef.isWritable) {
                nfcStatus.text = "NFC tag is not writable!"
                return false
            }
            if (ndef.maxSize < ndefMessage.toByteArray().size) {
                nfcStatus.text = "NFC tag does not have enough space!"
                return false
            }
            ndef.writeNdefMessage(ndefMessage)
            true
        } catch (e: Exception) {
            false
        } finally {
            ndef.close()
        }
    }

    private fun createTextRecord(language: String, text: String): NdefRecord {
        val languageBytes = language.toByteArray(StandardCharsets.US_ASCII)
        val textBytes = text.toByteArray(StandardCharsets.UTF_8)
        val payload = ByteArray(1 + languageBytes.size + textBytes.size)

        payload[0] = languageBytes.size.toByte()
        System.arraycopy(languageBytes, 0, payload, 1, languageBytes.size)
        System.arraycopy(textBytes, 0, payload, 1 + languageBytes.size, textBytes.size)

        return NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), payload)
    }
}