package com.andvl1.engrade.platform

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.andvl1.engrade.data.PoolBoutWithNames
import com.andvl1.engrade.data.PoolFencerWithName
import com.andvl1.engrade.data.db.entity.PoolEntity
import com.andvl1.engrade.domain.model.FencerRanking
import com.andvl1.engrade.domain.model.MatrixCell
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Generates PDF protocol for pool results using Android PDF API.
 */
class PdfExporter(private val context: Context) {

    companion object {
        private const val PAGE_WIDTH = 595  // A4 width in points (8.27 inches)
        private const val PAGE_HEIGHT = 842 // A4 height in points (11.69 inches)
        private const val MARGIN = 40f
        private const val TITLE_SIZE = 24f
        private const val SUBTITLE_SIZE = 16f
        private const val BODY_SIZE = 10f
        private const val TABLE_HEADER_SIZE = 12f
    }

    /**
     * Export pool protocol to PDF file.
     *
     * @return File in app's cache directory
     */
    fun exportPoolProtocol(
        pool: PoolEntity,
        fencers: List<PoolFencerWithName>,
        bouts: List<PoolBoutWithNames>,
        rankings: List<FencerRanking>,
        matrix: List<List<MatrixCell?>>
    ): File {
        val pdfDocument = PdfDocument()

        // Page 1: Title, pool info, matrix, rankings
        val page1Info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page1 = pdfDocument.startPage(page1Info)

        var yPosition = drawPage1(
            canvas = page1.canvas,
            pool = pool,
            fencers = fencers,
            matrix = matrix,
            rankings = rankings
        )

        pdfDocument.finishPage(page1)

        // Page 2: Full bout list (if needed)
        if (bouts.isNotEmpty()) {
            val page2Info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create()
            val page2 = pdfDocument.startPage(page2Info)

            drawBoutsList(page2.canvas, bouts)

            pdfDocument.finishPage(page2)
        }

        // Save to cache directory
        val pdfDir = File(context.cacheDir, "pdf")
        if (!pdfDir.exists()) {
            pdfDir.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val pdfFile = File(pdfDir, "pool_protocol_$timestamp.pdf")

        FileOutputStream(pdfFile).use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }

        pdfDocument.close()

        return pdfFile
    }

    /**
     * Share PDF file using Intent.
     */
    fun sharePdf(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share Pool Protocol")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(chooserIntent)
    }

    private fun drawPage1(
        canvas: Canvas,
        pool: PoolEntity,
        fencers: List<PoolFencerWithName>,
        matrix: List<List<MatrixCell?>>,
        rankings: List<FencerRanking>
    ): Float {
        var y = MARGIN

        val titlePaint = Paint().apply {
            textSize = TITLE_SIZE
            isFakeBoldText = true
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            textSize = SUBTITLE_SIZE
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            textSize = BODY_SIZE
            isAntiAlias = true
        }

        // Title
        canvas.drawText("Протокол группы / Pool Protocol", MARGIN, y, titlePaint)
        y += 40f

        // Pool info
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date(pool.createdAt))

        canvas.drawText("Дата / Date: $dateStr", MARGIN, y, bodyPaint)
        y += 20f

        val weaponStr = if (pool.weapon == "SABRE") "Сабля / Sabre" else "Рапира-Шпага / Foil-Epee"
        canvas.drawText("Оружие / Weapon: $weaponStr", MARGIN, y, bodyPaint)
        y += 20f

        canvas.drawText("Режим / Mode: до ${pool.mode} / to ${pool.mode}", MARGIN, y, bodyPaint)
        y += 30f

        // FIE Matrix
        canvas.drawText("Матрица результатов / Results Matrix", MARGIN, y, subtitlePaint)
        y += 25f

        y = drawMatrix(canvas, matrix, fencers, y)
        y += 30f

        // Rankings
        canvas.drawText("Итоговое ранжирование / Final Rankings", MARGIN, y, subtitlePaint)
        y += 25f

        y = drawRankings(canvas, rankings, y)

        return y
    }

    private fun drawMatrix(
        canvas: Canvas,
        matrix: List<List<MatrixCell?>>,
        fencers: List<PoolFencerWithName>,
        startY: Float
    ): Float {
        var y = startY
        val cellSize = 40f
        val nameColWidth = 120f

        val headerPaint = Paint().apply {
            textSize = TABLE_HEADER_SIZE
            isFakeBoldText = true
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            textSize = BODY_SIZE
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val diagonalPaint = Paint().apply {
            style = Paint.Style.FILL
            color = android.graphics.Color.LTGRAY
            isAntiAlias = true
        }

        val cellHeight = 20f

        // Header row
        var x = MARGIN + nameColWidth
        for (col in matrix.indices) {
            canvas.drawRect(x, y, x + cellSize, y + cellHeight, linePaint)
            canvas.drawText("${col + 1}", x + 14f, y + 15f, headerPaint)
            x += cellSize
        }
        y += cellHeight

        // Matrix rows
        for (rowIndex in matrix.indices) {
            x = MARGIN

            // Fencer name
            val fencerName = fencers.getOrNull(rowIndex)?.fencerName ?: "?"
            canvas.drawText("${rowIndex + 1}. $fencerName", x, y + 15f, bodyPaint)
            x += nameColWidth

            // Cells
            for (colIndex in matrix[rowIndex].indices) {
                val cell = matrix[rowIndex][colIndex]

                // Diagonal cell (fencer vs self)
                if (cell == null) {
                    canvas.drawRect(x, y, x + cellSize, y + cellHeight, diagonalPaint)
                }

                // Draw cell border
                canvas.drawRect(x, y, x + cellSize, y + cellHeight, linePaint)

                // Draw cell content
                if (cell != null && cell.leftScore != null && cell.rightScore != null) {
                    val label = if (cell.isVictory == true) "V" else "D"
                    val text = "$label${cell.leftScore}"
                    canvas.drawText(text, x + 5f, y + 15f, bodyPaint)
                }

                x += cellSize
            }

            y += cellHeight
        }

        return y
    }

    private fun drawRankings(
        canvas: Canvas,
        rankings: List<FencerRanking>,
        startY: Float
    ): Float {
        var y = startY

        val headerPaint = Paint().apply {
            textSize = TABLE_HEADER_SIZE
            isFakeBoldText = true
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            textSize = BODY_SIZE
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        // Header
        val colWidths = listOf(30f, 120f, 30f, 50f, 30f, 30f, 40f)
        val headers = listOf("#", "Имя / Name", "V", "V/M%", "TD", "TR", "Ind")

        var x = MARGIN
        for (i in headers.indices) {
            canvas.drawText(headers[i], x, y + 15f, headerPaint)
            x += colWidths[i]
        }
        y += 20f

        // Horizontal line
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 5f

        // Rankings rows
        for (ranking in rankings) {
            x = MARGIN

            canvas.drawText("${ranking.place}", x, y + 15f, bodyPaint)
            x += colWidths[0]

            canvas.drawText(ranking.name, x, y + 15f, bodyPaint)
            x += colWidths[1]

            canvas.drawText("${ranking.victories}", x, y + 15f, bodyPaint)
            x += colWidths[2]

            canvas.drawText(String.format("%.1f", ranking.vmPercent), x, y + 15f, bodyPaint)
            x += colWidths[3]

            canvas.drawText("${ranking.touchesDelivered}", x, y + 15f, bodyPaint)
            x += colWidths[4]

            canvas.drawText("${ranking.touchesReceived}", x, y + 15f, bodyPaint)
            x += colWidths[5]

            val indexSign = if (ranking.index >= 0) "+" else ""
            canvas.drawText("$indexSign${ranking.index}", x, y + 15f, bodyPaint)

            y += 20f
        }

        return y
    }

    private fun drawBoutsList(
        canvas: Canvas,
        bouts: List<PoolBoutWithNames>
    ) {
        var y = MARGIN

        val titlePaint = Paint().apply {
            textSize = SUBTITLE_SIZE
            isFakeBoldText = true
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            textSize = BODY_SIZE
            isAntiAlias = true
        }

        canvas.drawText("Список всех боёв / Full Bout List", MARGIN, y, titlePaint)
        y += 30f

        for (bout in bouts) {
            val boutInfo = "#${bout.bout.boutOrder}: ${bout.leftFencerName} vs ${bout.rightFencerName}"

            val result = if (bout.bout.leftScore != null && bout.bout.rightScore != null) {
                val leftLabel = if (bout.bout.leftScore!! > bout.bout.rightScore!!) "V" else "D"
                val rightLabel = if (bout.bout.rightScore!! > bout.bout.leftScore!!) "V" else "D"
                " — $leftLabel${bout.bout.leftScore} / $rightLabel${bout.bout.rightScore}"
            } else {
                " — Pending"
            }

            canvas.drawText(boutInfo + result, MARGIN, y, bodyPaint)
            y += 18f

            if (y > PAGE_HEIGHT - MARGIN) {
                break // Prevent overflow
            }
        }
    }
}
