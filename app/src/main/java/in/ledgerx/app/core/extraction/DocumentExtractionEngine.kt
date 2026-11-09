package com.ledgerx.app.core.extraction

/**
 * A provider-neutral contract. OCR text is not interpreted as verified financial truth.
 * Empty fields remain empty; users must review before saving.
 */
data class ExtractedField(val value: String, val evidence: String)
data class ExtractionResult(
    val category: String = "Other",
    val institution: ExtractedField? = null,
    val principal: ExtractedField? = null,
    val maturityAmount: ExtractedField? = null,
    val rate: ExtractedField? = null,
    val openingDate: ExtractedField? = null,
    val maturityDate: ExtractedField? = null,
    val reference: ExtractedField? = null,
    val engine: String
)
interface DocumentExtractionEngine {
    val name: String
    fun extract(ocrText: String): ExtractionResult
}

/**
 * Local, format-independent label matcher. This is a deterministic baseline, NOT AI.
 * To benchmark an AI provider later, implement DocumentExtractionEngine and run
 * the same fixtures, redacting sensitive documents before external transmission.
 */
class SemanticLabelEngine : DocumentExtractionEngine {
    override val name = "semantic-label-v1"
    private val money = Regex("""(?<![A-Za-z0-9])(?:INR|Rs\\.?|₹)?\\s*([0-9]{1,3}(?:,[0-9]{2,3})+(?:\\.[0-9]{1,2})?|[0-9]{1,12}(?:\\.[0-9]{1,2})?)(?![0-9])""",RegexOption.IGNORE_CASE)
    private val date = Regex("""\\b(?:\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4}|\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2})\\b""")
    private val banks = listOf("HDFC BANK","ICICI BANK","STATE BANK OF INDIA","SBI","AXIS BANK","KOTAK","CANARA BANK","BANK OF BARODA","IDFC FIRST","PUNJAB NATIONAL BANK","PNB")
    private val principalLabels = listOf("deposit amount","amount invested","principal amount","initial deposit","original principal","amount deposited","investment amount","booking amount","principal","deposit value")
    private val maturityLabels = listOf("maturity proceeds","maturity amount","maturity value","redemption value","redemption proceeds","payable on maturity","amount on maturity")
    private val openingLabels = listOf("opening date","date of deposit","deposit date","start date","booking date","value date","investment date")
    private val maturityDateLabels = listOf("maturity date","due date","redemption date","matures on","date of maturity")
    private val rateLabels = listOf("rate of interest","interest rate","rate p.a.","rate pa","roi","int. rate","interest p.a.")
    private fun field(lines: List<String>, labels: List<String>, pattern: Regex): ExtractedField? {
        for ((i,line) in lines.withIndex()) {
            val label = labels.firstOrNull { line.contains(it, ignoreCase=true) } ?: continue
            val pos=line.indexOf(label,ignoreCase=true)
            val rest=line.substring(pos+label.length).trimStart(' ',':','-','=', '₹')
            val found=pattern.find(rest)
            if(found!=null) return ExtractedField(found.groups[1]?.value ?: found.value, line)
            // For table-like layouts where value is on the following line
            if(rest.isBlank() && i+1<lines.size) {
                val next=lines[i+1]
                val candidate=pattern.find(next)
                if(candidate!=null) return ExtractedField(candidate.groups[1]?.value ?: candidate.value,next)
            }
        }
        return null
    }
    override fun extract(ocrText: String): ExtractionResult {
        val lines=ocrText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val lower=ocrText.lowercase()
        val category=when {
            listOf("recurring deposit","monthly installment","rd account").any(lower::contains) -> "Recurring Deposit"
            listOf("fixed deposit","term deposit","deposit receipt","fd advice").any(lower::contains) -> "Fixed Deposit"
            listOf("mutual fund","folio","nav","scheme name").count(lower::contains)>=2 -> "Mutual Fund"
            listOf("demat","equity shares","holdings statement").any(lower::contains) -> "Stock"
            listOf("gold","bullion").any(lower::contains) -> "Gold"
            listOf("provident fund","epfo","ppf","nps").any(lower::contains) -> "EPF / PPF / NPS"
            listOf("outstanding loan","loan statement","emi").any(lower::contains) -> "Liability"
            else -> "Other"
        }
        val bankLine=lines.firstOrNull { line -> banks.any { line.contains(it,true) } }
        val institution=bankLine?.let { line -> ExtractedField(banks.first { line.contains(it,true) },line) }
        val ratePattern=Regex("""(?i)\\b(\\d{1,2}(?:\\.\\d{1,3})?)\\s*%?""")
        val referencePattern=Regex("""(?i)(?:no\\.?|number|id|#)\\s*[:#-]?\\s*([A-Z0-9X*\\-]{4,})""")
        return ExtractionResult(
            category=category,institution=institution,
            principal=field(lines,principalLabels,money)?.let { it.copy(value=it.value.replace(",","")) },
            maturityAmount=field(lines,maturityLabels,money)?.let { it.copy(value=it.value.replace(",","")) },
            rate=field(lines,rateLabels,ratePattern),
            openingDate=field(lines,openingLabels,date),
            maturityDate=field(lines,maturityDateLabels,date),
            reference=field(lines,listOf("deposit reference","fd number","receipt number","deposit no","account number","folio number"),referencePattern),
            engine=name
        )
    }
}
