package com.famyrex.app

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
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
        val code = intent.getStringExtra(EXTRA_CODE).orEmpty()
        val token = intent.getStringExtra(EXTRA_TOKEN).orEmpty()
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
            text = "Abre Famyrex en el móvil de $childLabel y escanea este código. Los dos móviles deben estar juntos."
            textSize = 16f
            setPadding(0, 12, 0, 20)
        })
        root.addView(ImageView(this).apply {
            setImageBitmap(createQr(link))
            adjustViewBounds = true
            contentDescription = "Código QR de vinculación de Famyrex"
        }, LinearLayout.LayoutParams(-1, 0, 1f))

        if (code.isNotBlank() || token.isNotBlank()) {
            root.addView(TextView(this).apply {
                text = "Si no puedes escanearlo, introduce estos datos manualmente en el otro móvil:\n\nCódigo: $code\nToken: $token"
                textSize = 14f
                setPadding(0, 16, 0, 0)
            })
        }
        root.addView(TextView(this).apply {
            text = "No compartas estos datos por mensajes o redes. Úsalos solo entre los dos móviles que se van a vincular."
            textSize = 13f
            setPadding(0, 12, 0, 0)
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
        const val EXTRA_CODE = "extra_code"
        const val EXTRA_TOKEN = "extra_token"
    }
}
