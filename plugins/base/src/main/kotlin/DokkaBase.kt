package org.jetbrains.dokka.base

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.transformers.descriptors.DefaultDescriptorToDocumentationTranslator
import org.jetbrains.dokka.base.transformers.documentables.DefaultDocumentableMerger
import org.jetbrains.dokka.base.transformers.documentables.DefaultDocumentablesToPageTranslator
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.transformers.pages.comments.DocTagToContentConverter
import org.jetbrains.dokka.base.transformers.pages.merger.FallbackPageMergerStrategy
import org.jetbrains.dokka.base.transformers.pages.merger.PageMergerStrategy
import org.jetbrains.dokka.base.transformers.pages.merger.PageNodeMerger
import org.jetbrains.dokka.base.transformers.pages.merger.SameMethodNamePageMergerStrategy
import org.jetbrains.dokka.base.transformers.psi.DefaultPsiToDocumentationTranslator
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.renderers.html.HtmlRenderer

class DokkaBase : DokkaPlugin() {
    val pageMergerStrategy by extensionPoint<PageMergerStrategy>()
    val commentsToContentConverter by extensionPoint<CommentsToContentConverter>()

    val descriptorToDocumentationTranslator by extending(isFallback = true) {
        CoreExtensions.descriptorToDocumentationTranslator providing ::DefaultDescriptorToDocumentationTranslator
    }

    val psiToDocumentationTranslator by extending(isFallback = true) {
        CoreExtensions.psiToDocumentationTranslator with DefaultPsiToDocumentationTranslator
    }

    val documentableMerger by extending(isFallback = true) {
        CoreExtensions.documentableMerger with DefaultDocumentableMerger
    }

    val documentablesToPageTranslator by extending(isFallback = true) {
        CoreExtensions.documentablesToPageTranslator providing { ctx ->
            DefaultDocumentablesToPageTranslator(ctx.single(commentsToContentConverter), ctx.logger)
        }
    }

    val docTagToContentConverter by extending(isFallback = true) {
        commentsToContentConverter with DocTagToContentConverter
    }

    val pageMerger by extending {
        CoreExtensions.pageTransformer providing { ctx -> PageNodeMerger(ctx[pageMergerStrategy]) }
    }

    val fallbackMerger by extending {
        pageMergerStrategy providing { ctx -> FallbackPageMergerStrategy(ctx.logger) }
    }

    val sameMethodNameMerger by extending {
        pageMergerStrategy with SameMethodNamePageMergerStrategy order {
            before(fallbackMerger)
        }
    }

    val htmlRenderer by extending {
        CoreExtensions.renderer providing ::HtmlRenderer applyIf { format == "html" }
    }
}