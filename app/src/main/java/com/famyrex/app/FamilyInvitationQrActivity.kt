package com.famyrex.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

class FamilyInvitationQrActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val link = intent.getStringExtra(EXTRA_LINK).orEmpty()
        val childLabel = intent.getStringExtra(EXTRA_LABEL).orEmpty().ifBlank { "Perfil infantil" }
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty()
        if (link.isBlank()) {
            finish()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        root.addView(TextView(this).apply {
            text = "Vincular $childLabel"
            textSize = 24f
        })
        root.addView(TextView(this).apply {
            text = "En el móvil del menor, abre Famyrex y escanea este código QR. Después deberá aceptar la invitación."
            textSize = 16f
            setPadding(0, 12, 0, 20)
        })
        root.addView(ImageView(this).apply {
            setImageBitmap(createQr(link))
            adjustViewBounds = true
            contentDescription = "Código QR de vinculación de Famyrex"
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(Button(this).apply {
            text = "Enviar enlace por WhatsApp, correo…"
            setOnClickListener {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Invitación a la familia Famyrex · $childLabel")
                    putExtra(Intent.EXTRA_TEXT, message)
                }
                startActivity(Intent.createChooser(shareIntent, "Enviar invitación de Famyrex"))
            }
        })
        root.addView(TextView(this).apply {
            text = "También puedes compartir el enlace directamente:\n$link"
            textSize = 13f
            setPadding(0, 16, 0, 0)
        })
        setContentView(root)
    }

    private fun createQr(value: String): Bitmap {
        val size = 900
        val matrix: BitMatrix = MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    companion object {
        const val EXTRA_LINK = "extra_link"
        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_MESSAGE = "extra_message"
    }
}
