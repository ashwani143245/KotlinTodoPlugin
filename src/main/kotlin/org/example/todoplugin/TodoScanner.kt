package org.example.todoplugin

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile

class TodoScanner {
    companion object {
        // Scan a file for TODO comments
        fun scanFile(file: VirtualFile): List<TodoItem> {
            val document = FileDocumentManager.getInstance().getDocument(file) ?: return emptyList()
            return scanDocument(document, file.path)
        }

        // Scan a document for TODO comments
        fun scanDocument(document: Document, filePath: String): List<TodoItem> {
            val todos = mutableListOf<TodoItem>()
            val text = document.text
            val lines = text.split("\n")

            for ((index, line) in lines.withIndex()) {
                // Look for TODO comments in the line
                val lineNumber = index + 1
                val todoIndex = line.indexOf("TODO", 0, true)
                
                if (todoIndex >= 0) {
                    // Check if TODO is part of a comment
                    val commentStart = line.indexOf("//", 0, true)
                    val blockCommentStart = line.indexOf("/*", 0, true)
                    
                    if ((commentStart >= 0 && todoIndex > commentStart) || 
                        (blockCommentStart >= 0 && todoIndex > blockCommentStart)) {
                        // Extract the TODO text
                        val todoText = extractTodoText(line, todoIndex)
                        todos.add(TodoItem(todoText, lineNumber, filePath))
                    }
                }
            }

            return todos
        }

        // Extract the TODO text from a line
        private fun extractTodoText(line: String, todoIndex: Int): String {
            val startIndex = todoIndex
            var endIndex = line.length
            
            // Look for the end of the comment
            val blockCommentEnd = line.indexOf("*/", todoIndex)
            if (blockCommentEnd > 0) {
                endIndex = blockCommentEnd
            }
            
            return line.substring(startIndex, endIndex).trim()
        }
    }
}