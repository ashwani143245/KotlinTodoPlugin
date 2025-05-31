package org.example.todoplugin

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement

class TodoHighlightAnnotator : Annotator {
    companion object {
        // Define text attributes for TODO highlighting
        val TODO_ATTRIBUTES: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "KOTLIN_TODO_COMMENT",
            DefaultLanguageHighlighterColors.LINE_COMMENT
                .copyWithBold()
                .copyWithForeground(java.awt.Color(0, 128, 0))
        )
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // Only process comments
        if (element !is PsiComment) return

        val text = element.text
        val todoStart = text.indexOf("TODO", 0, true)
        
        if (todoStart >= 0) {
            // Create a text range for the TODO text
            val textRange = TextRange(element.textRange.startOffset + todoStart, element.textRange.startOffset + text.length)
            
            // Add the annotation
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(textRange)
                .textAttributes(TODO_ATTRIBUTES)
                .create()
        }
    }
}

// Extension functions for TextAttributesKey
fun TextAttributesKey.copyWithBold(): TextAttributesKey {
    val attributes = this.defaultAttributes.clone()
    attributes.fontType = attributes.fontType or java.awt.Font.BOLD
    return TextAttributesKey.createTextAttributesKey("${this.externalName}_BOLD", attributes)
}

fun TextAttributesKey.copyWithForeground(color: java.awt.Color): TextAttributesKey {
    val attributes = this.defaultAttributes.clone()
    attributes.foregroundColor = color
    return TextAttributesKey.createTextAttributesKey("${this.externalName}_COLORED", attributes)
}