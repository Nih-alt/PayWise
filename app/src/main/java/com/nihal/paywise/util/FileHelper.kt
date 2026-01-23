package com.nihal.paywise.util

import android.content.Context
import android.net.Uri
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileHelper {

    fun copyUriToInternal(context: Context, uri: Uri, targetFile: File): Long {
        targetFile.parentFile?.mkdirs()
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                return input.copyTo(output)
            }
        }
        return 0
    }

    fun deleteFile(file: File): Boolean {
        return if (file.exists()) file.delete() else false
    }

    fun zipDirectory(sourceDir: File, outZipFile: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outZipFile))).use { zos ->
            zipFolder(sourceDir, sourceDir, zos)
        }
    }

    private fun zipFolder(root: File, source: File, zos: ZipOutputStream) {
        val files = source.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                zipFolder(root, file, zos)
            } else {
                val entryName = file.absolutePath.removePrefix(root.absolutePath + File.separator)
                val entry = ZipEntry(entryName)
                zos.putNextEntry(entry)
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    fun unzip(zipFile: File, targetDir: File) {
        targetDir.mkdirs()
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val file = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { zis.copyTo(it) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
