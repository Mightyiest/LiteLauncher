package app.olauncher.helper

import android.graphics.Typeface
import android.widget.TextView
import app.olauncher.data.Constants

object FontHelper {
    fun getTypeface(fontFamily: String, bold: Boolean = false): Typeface {
        val family = fontFamily.ifBlank { Constants.FontFamily.LIGHT }
        return Typeface.create(family, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    fun applyFont(fontFamily: String, vararg views: TextView?) {
        val tf = getTypeface(fontFamily, bold = false)
        for (view in views) {
            view?.typeface = tf
        }
    }
}
