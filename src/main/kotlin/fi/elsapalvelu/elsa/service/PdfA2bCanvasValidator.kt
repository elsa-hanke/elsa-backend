package fi.elsapalvelu.elsa.service

import com.itextpdf.kernel.colors.Color
import com.itextpdf.kernel.colors.PatternColor
import com.itextpdf.kernel.pdf.PdfDictionary
import com.itextpdf.kernel.pdf.PdfLiteral
import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.kernel.pdf.PdfObject
import com.itextpdf.kernel.pdf.PdfResources
import com.itextpdf.kernel.pdf.PdfStream
import com.itextpdf.kernel.pdf.canvas.CanvasGraphicsState
import com.itextpdf.kernel.pdf.canvas.parser.EventType
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor
import com.itextpdf.kernel.pdf.canvas.parser.data.AbstractRenderInfo
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData
import com.itextpdf.kernel.pdf.canvas.parser.data.ImageRenderInfo
import com.itextpdf.kernel.pdf.canvas.parser.data.PathRenderInfo
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener
import com.itextpdf.kernel.pdf.colorspace.PdfPattern
import com.itextpdf.pdfa.checker.PdfA2Checker

/** Feeds interpreted painting operations (including nested Form XObjects) into iText's PDF/A checks. */
internal class PdfA2bCanvasValidator(
    private val checker: PdfA2Checker,
    private val contentStream: PdfStream?,
    private val activePatterns: MutableSet<PdfObject> = mutableSetOf()
) : PdfCanvasProcessor(object : IEventListener {
    override fun eventOccurred(data: IEventData?, type: EventType) = Unit
    override fun getSupportedEvents(): Set<EventType>? = null
}) {
    override fun invokeOperator(operator: PdfLiteral, operands: MutableList<PdfObject>) {
        when (operator.toString()) {
            "q", "Q" -> checker.checkCanvasStack(operator.toString().single())
            "ri" -> checker.checkRenderingIntent(operands[0] as PdfName)
        }
        super.invokeOperator(operator, operands)
        if (operator.toString() == "gs") {
            checker.checkExtGState(graphicsState, contentStream)
        }
    }

    override fun eventOccurred(data: IEventData?, type: EventType) {
        val state = (data as? AbstractRenderInfo)?.graphicsState
        if (state != null && type in PAINT_EVENTS) {
            checker.checkExtGState(state, contentStream)
            checker.checkRenderingIntent(state.renderingIntent)
        }
        when (data) {
            is PathRenderInfo -> checkColors(
                data.graphicsState,
                data.operation and PathRenderInfo.FILL != 0,
                data.operation and PathRenderInfo.STROKE != 0
            )
            is TextRenderInfo -> {
                val mode = data.textRenderMode
                checkColors(data.graphicsState, mode in FILL_TEXT_MODES, mode in STROKE_TEXT_MODES)
                checker.checkFont(data.font)
                checker.checkFontGlyphs(data.font, contentStream)
                checker.checkText(data.text, data.font)
            }
            is ImageRenderInfo -> if (data.isInline) {
                checker.checkInlineImage(data.image.pdfObject, resources.getResource(PdfName.ColorSpace))
            }
        }
        super.eventOccurred(data, type)
    }

    private fun checkColors(state: CanvasGraphicsState, fill: Boolean, stroke: Boolean) {
        val colorSpaces = resources.getResource(PdfName.ColorSpace)
        if (fill) {
            checker.checkColor(state, state.fillColor, colorSpaces, true, contentStream)
            checkPattern(state.fillColor)
        }
        if (stroke) {
            checker.checkColor(state, state.strokeColor, colorSpaces, false, contentStream)
            checkPattern(state.strokeColor)
        }
    }

    private fun checkPattern(color: Color) {
        val pattern = (color as? PatternColor)?.pattern as? PdfPattern.Tiling ?: return
        val stream = pattern.pdfObject as PdfStream
        check(activePatterns.add(stream)) { "Recursive PDF pattern" }
        try {
            PdfA2bCanvasValidator(checker, contentStream, activePatterns).processContent(
                stream.bytes, PdfResources(stream.getAsDictionary(PdfName.Resources) ?: PdfDictionary())
            )
        } finally {
            activePatterns.remove(stream)
        }
    }

    private companion object {
        val PAINT_EVENTS = setOf(EventType.RENDER_TEXT, EventType.RENDER_PATH, EventType.RENDER_IMAGE)
        val FILL_TEXT_MODES = setOf(0, 2, 4, 6)
        val STROKE_TEXT_MODES = setOf(1, 2, 5, 6)
    }
}
