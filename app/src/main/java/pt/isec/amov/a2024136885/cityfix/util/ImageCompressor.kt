package pt.isec.amov.a2024136885.cityfix.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.min

data class CompressedMediaResult(
    val mainFile: File,
    val thumbnailFile: File
)

object ImageCompressor {

    private const val MAX_IMAGE_DIMENSION = 1280
    private const val THUMBNAIL_DIMENSION = 256
    private const val JPEG_QUALITY = 80
    private const val THUMB_QUALITY = 70

    /**
     * Processa a imagem a partir de uma Uri:
     * - Corrige orientação EXIF
     * - Comprime a foto principal
     * - Cria uma miniatura quadrada (thumbnail)
     */
    suspend fun compressAndGenerateThumbnail(
        context: Context,
        imageUri: Uri,
        ticketId: String
    ): Result<CompressedMediaResult> = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            val mainOutputFile = File(cacheDir, "ticket_${ticketId}_img.jpg")
            val thumbOutputFile = File(cacheDir, "ticket_${ticketId}_thumb.jpg")

            // 1. Decodificar dimensões sem carregar todo o bitmap para a memória
            val bounds = decodeImageDimensions(context, imageUri)
                ?: return@withContext Result.failure(Exception("Não foi possível ler as dimensões da imagem."))

            // 2. Calcular sample size adequado para a foto principal
            val sampleSize = calculateInSampleSize(bounds.first, bounds.second, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)

            // 3. Decodificar bitmap com subamostragem
            var originalBitmap = decodeSampledBitmap(context, imageUri, sampleSize)
                ?: return@withContext Result.failure(Exception("Falha ao processar o ficheiro de imagem."))

            // 4. Corrigir orientação EXIF (para não ficar de lado em fotos tiradas com o telemóvel)
            originalBitmap = rotateBitmapIfRequired(context, imageUri, originalBitmap)

            // 5. Escalonar imagem principal respeitando proporções
            val mainScaled = scaleBitmapKeepRatio(originalBitmap, MAX_IMAGE_DIMENSION)
            saveBitmapToJpeg(mainScaled, mainOutputFile, JPEG_QUALITY)

            // 6. Gerar thumbnail quadrado central
            val thumbBitmap = createSquareThumbnail(originalBitmap, THUMBNAIL_DIMENSION)
            saveBitmapToJpeg(thumbBitmap, thumbOutputFile, THUMB_QUALITY)

            // Limpeza de memória
            if (originalBitmap != mainScaled) originalBitmap.recycle()
            mainScaled.recycle()
            thumbBitmap.recycle()

            Result.success(CompressedMediaResult(mainFile = mainOutputFile, thumbnailFile = thumbOutputFile))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun decodeImageDimensions(context: Context, uri: Uri): Pair<Int, Int>? {
        var input: InputStream? = null
        return try {
            input = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(input, null, options)
            if (options.outWidth > 0 && options.outHeight > 0) {
                Pair(options.outWidth, options.outHeight)
            } else null
        } catch (e: Exception) {
            null
        } finally {
            input?.close()
        }
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri, sampleSize: Int): Bitmap? {
        var input: InputStream? = null
        return try {
            input = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeStream(input, null, options)
        } catch (e: Exception) {
            null
        } finally {
            input?.close()
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun scaleBitmapKeepRatio(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int

        if (width > height) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt()
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun createSquareThumbnail(bitmap: Bitmap, targetSize: Int): Bitmap {
        val dimension = min(bitmap.width, bitmap.height)
        val xOffset = (bitmap.width - dimension) / 2
        val yOffset = (bitmap.height - dimension) / 2

        val square = Bitmap.createBitmap(bitmap, xOffset, yOffset, dimension, dimension)
        val scaled = Bitmap.createScaledBitmap(square, targetSize, targetSize, true)
        if (square != bitmap && square != scaled) square.recycle()
        return scaled
    }

    private fun rotateBitmapIfRequired(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        var input: InputStream? = null
        return try {
            input = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(input)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            if (rotationDegrees != 0f) {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                bitmap.recycle()
                rotated
            } else {
                bitmap
            }
        } catch (e: Exception) {
            bitmap
        } finally {
            input?.close()
        }
    }

    private fun saveBitmapToJpeg(bitmap: Bitmap, destination: File, quality: Int) {
        FileOutputStream(destination).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.flush()
        }
    }
}