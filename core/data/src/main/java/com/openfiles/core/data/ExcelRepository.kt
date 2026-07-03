package com.openfiles.core.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Read-only .xlsx access via Apache POI. Never write with POI in v1 (see ADR: read-only Office). */
@Singleton
class ExcelRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    data class Sheet(val name: String, val rows: List<List<String>>)

    suspend fun readWorkbook(uri: Uri): List<Sheet> = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Can't open file: $uri")
        input.use { input ->
            XSSFWorkbook(input).use { wb ->
                val fmt = DataFormatter()
                (0 until wb.numberOfSheets).map { s ->
                    val sheet = wb.getSheetAt(s)
                    val rows = (0..sheet.lastRowNum).map { r ->
                        val row = sheet.getRow(r) ?: return@map emptyList()
                        (0 until row.lastCellNum.coerceAtLeast(0)).map { c ->
                            fmt.formatCellValue(row.getCell(c))
                        }
                    }
                    Sheet(sheet.sheetName, rows)
                }
            }
        }
    }
}
