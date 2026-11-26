package app.olauncher.helper

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.UserHandle
import android.util.DisplayMetrics
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MonochromeIconHelper {

    private val iconCache = LruCache<String, Bitmap>(32)

    suspend fun loadMonochromeIcon(
        context: Context,
        packageName: String,
        userHandle: UserHandle,
        isDarkTheme: Boolean,
        sizePx: Int,
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (packageName.isBlank() || sizePx <= 0) return@withContext null

        val cacheKey = "$packageName|$userHandle|$isDarkTheme|$sizePx"
        iconCache.get(cacheKey)?.let { cached ->
            if (!cached.isRecycled) return@withContext cached
        }

        val drawable = resolveAppDrawable(context, packageName, userHandle) ?: return@withContext null
        val fgColor = if (isDarkTheme) Color.WHITE else Color.BLACK

        val rendered = renderMonochromeBitmap(drawable, fgColor, isDarkTheme, sizePx)
        iconCache.put(cacheKey, rendered)
        rendered
    }

    private fun resolveAppDrawable(
        context: Context,
        packageName: String,
        userHandle: UserHandle,
    ): Drawable? {
        return try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val activityInfo = launcherApps.getActivityList(packageName, userHandle).firstOrNull()
            activityInfo?.getIcon(DisplayMetrics.DENSITY_XXXHIGH)
                ?: context.packageManager.getApplicationIcon(packageName)
        } catch (_: Exception) {
            try {
                context.packageManager.getApplicationIcon(packageName)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun renderMonochromeBitmap(
        drawable: Drawable,
        fgColor: Int,
        isDarkTheme: Boolean,
        sizePx: Int,
    ): Bitmap {
        // Tier 1: Native Android 13+ AdaptiveIconDrawable monochrome vector
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && drawable is AdaptiveIconDrawable) {
            val monoLayer = drawable.monochrome
            if (monoLayer != null) {
                val monoCopy = monoLayer.constantState?.newDrawable()?.mutate() ?: monoLayer.mutate()
                val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                canvas.save()
                canvas.scale(1.45f, 1.45f, sizePx / 2f, sizePx / 2f)
                monoCopy.setBounds(0, 0, sizePx, sizePx)
                monoCopy.colorFilter = PorterDuffColorFilter(fgColor, PorterDuff.Mode.SRC_IN)
                monoCopy.draw(canvas)
                canvas.restore()
                return output
            }
        }

        // Tier 2: High-contrast OLED B&W fallback for apps/games without a native monochrome layer
        val adaptiveFg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            drawable.foreground
        } else {
            null
        }

        val temp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        try {
            val tempCanvas = Canvas(temp)
            if (adaptiveFg != null) {
                val fgCopy = adaptiveFg.constantState?.newDrawable()?.mutate() ?: adaptiveFg.mutate()
                tempCanvas.save()
                tempCanvas.scale(1.35f, 1.35f, sizePx / 2f, sizePx / 2f)
                fgCopy.setBounds(0, 0, sizePx, sizePx)
                fgCopy.draw(tempCanvas)
                tempCanvas.restore()
            } else {
                val iconCopy = drawable.constantState?.newDrawable()?.mutate() ?: drawable.mutate()
                iconCopy.setBounds(0, 0, sizePx, sizePx)
                iconCopy.draw(tempCanvas)
            }

            val satMatrix = ColorMatrix().apply { setSaturation(0f) }
            val contrast = 2.0f
            val brightnessShift = if (isDarkTheme) 24f else -20f
            val translate = (-0.5f * contrast + 0.5f) * 255f + brightnessShift
            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            satMatrix.postConcat(contrastMatrix)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(satMatrix)
            }

            val outCanvas = Canvas(output)
            if (adaptiveFg == null) {
                val clipPath = Path().apply {
                    addCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, Path.Direction.CW)
                }
                outCanvas.save()
                outCanvas.clipPath(clipPath)
                outCanvas.drawBitmap(temp, 0f, 0f, paint)
                outCanvas.restore()
            } else {
                outCanvas.drawBitmap(temp, 0f, 0f, paint)
            }
        } finally {
            temp.recycle()
        }
        return output
    }
}
