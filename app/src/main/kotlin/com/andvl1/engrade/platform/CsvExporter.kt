package com.andvl1.engrade.platform

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.andvl1.engrade.domain.model.FencerRanking
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates a CSV file of pool rankings and shares it via the system share sheet.
 *
 * Uses the same FileProvider authority and cache directory as [PdfExporter] so no
 * additional FileProvider configuration is required.
 */
class CsvExporter(private val context: Context) {

    /**
     * Writes the final rankings to a CSV file in the app cache and returns it.
     *
     * Columns: Place, Name, V, M, TD, TR, Ind
     * Fields that contain commas, double-quotes, or newlines are properly quoted per RFC 4180.
     */
    fun exportRankingsCsv(rankings: List<FencerRanking>): File {
        val csvContent = buildString {
            appendLine("Place,Name,V,M,TD,TR,Ind")
            rankings.forEach { r ->
                append(r.place)
                append(',')
                append(csvEscape(r.name))
                append(',')
                append(r.victories)
                append(',')
                append(r.matches)
                append(',')
                append(r.touchesDelivered)
                append(',')
                append(r.touchesReceived)
                append(',')
                appendLine(r.index)
            }
        }

        val csvDir = File(context.cacheDir, "csv")
        if (!csvDir.exists()) {
            csvDir.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val csvFile = File(csvDir, "pool_rankings_$timestamp.csv")
        csvFile.writeText(csvContent, Charsets.UTF_8)

        return csvFile
    }

    /**
     * Shares the given CSV file using the system share sheet.
     *
     * Reuses the same FileProvider authority used by [PdfExporter].
     */
    fun shareCsv(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share Rankings CSV")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(chooserIntent)
    }

    /**
     * Escapes a CSV field per RFC 4180: wraps in double-quotes when the value
     * contains a comma, double-quote, carriage return, or newline, and doubles
     * any internal double-quote characters.
     */
    private fun csvEscape(value: String): String {
        return if (value.contains(',') || value.contains('"') ||
            value.contains('\n') || value.contains('\r')
        ) {
            '"' + value.replace("\"", "\"\"") + '"'
        } else {
            value
        }
    }
}
