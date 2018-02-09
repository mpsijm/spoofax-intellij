package com.virtlink.editorservices.intellij.psi

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType

class AesiAstBuilder @Inject constructor(
        @Assisted private val elementTypeManager: AesiElementTypeManager) {

    /**
     * Factory.
     */
    interface IFactory {

        /**
         * Creates the AST builder.
         *
         * @param elementTypeManager The element type manager.
         */
        fun create(elementTypeManager: AesiElementTypeManager): AesiAstBuilder
    }

    fun build(root: IElementType, builder: PsiBuilder): ASTNode {
        val m = builder.mark()
        // Add sub-root to prevent ASTNode.firstChild from being null.
        val m2 = builder.mark()
        while (!builder.eof()) {
            val m3 = builder.mark()
            val elementType = this.elementTypeManager.getElementType(builder.tokenType as AesiTokenType)
            builder.advanceLexer()
            m3.done(elementType)
        }
        m2.done(elementTypeManager.rootElementType)
        m.done(root)

        return builder.treeBuilt
    }
}