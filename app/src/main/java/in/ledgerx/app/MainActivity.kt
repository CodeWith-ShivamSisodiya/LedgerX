package com.ledgerx.app

import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

private data class WealthAsset(
    val id: String = UUID.randomUUID().toString(),
    val category: String = "Fixed Deposit",
    val institution: String = "",
    val reference: String = "",
    val principal: String = "",
    val currentValue: String = "",
    val rate: String = "",
    val opened: String = "",
    val maturity: String = "",
    val notes: String = ""
) {
    fun json() = JSONObject().apply {
        put("id",id); put("category",category); put("institution",institution)
        put("reference",reference); put("principal",principal); put("currentValue",currentValue)
        put("rate",rate); put("opened",opened); put("maturity",maturity); put("notes",notes)
    }
    companion object {
        fun from(o: JSONObject) = WealthAsset(
            id=o.optString("id",UUID.randomUUID().toString()),
            category=o.optString("category","Other"),
            institution=o.optString("institution"),
            reference=o.optString("reference"),
            principal=o.optString("principal"),
            currentValue=o.optString("currentValue"),
            rate=o.optString("rate"),
            opened=o.optString("opened"),
            maturity=o.optString("maturity"),
            notes=o.optString("notes")
        )
    }
}
private val categories = listOf("Fixed Deposit","Recurring Deposit","Mutual Fund","Stock","Gold","Property","EPF / PPF / NPS","Savings","Other","Liability")
private fun amount(s: String) = s.replace(",","").replace("₹","").trim().toDoubleOrNull() ?: 0.0
private fun rupees(v: Double) = NumberFormat.getCurrencyInstance(Locale("en","IN")).format(v)

private object WealthRules {
    private val money = """(?:₹|Rs\.?|INR)?\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]{4,}(?:\.[0-9]{1,2})?)""".toRegex(RegexOption.IGNORE_CASE)
    private val date = """\b(\d{1,2}[-/.]\d{1,2}[-/.]\d{2,4}|\d{4}[-/.]\d{1,2}[-/.]\d{1,2})\b""".toRegex()
    private fun valueAfter(text: String, vararg labels: String): String {
        for (line in text.lines()) {
            if (labels.any { line.contains(it,ignoreCase=true) }) {
                val colon = line.indexOf(':')
                val part = if (colon >= 0) line.substring(colon+1) else line.replace(Regex("(?i)^.*?("+labels.joinToString("|") { Regex.escape(it) }+")"),"")
                val match = money.find(part)
                if (match != null) return match.groupValues[1].replace(",","")
            }
        }
        return ""
    }
    private fun dateAfter(text: String, vararg labels: String): String {
        for (line in text.lines()) if (labels.any { line.contains(it,true) }) date.find(line)?.let { return it.value }
        return ""
    }
    fun extract(text: String): WealthAsset {
        val lower=text.lowercase()
        val category= when {
            listOf("recurring deposit","rd account","monthly installment").any(lower::contains) -> "Recurring Deposit"
            listOf("fixed deposit","term deposit","fd receipt","deposit advice").any(lower::contains) -> "Fixed Deposit"
            listOf("mutual fund","folio","nav","scheme name").count(lower::contains)>=2 -> "Mutual Fund"
            listOf("demat","holdings statement","isin","equity shares").any(lower::contains) -> "Stock"
            listOf("gold","24k","22k","bullion").any(lower::contains) -> "Gold"
            listOf("provident fund","uan","epfo","nps","ppf").any(lower::contains) -> "EPF / PPF / NPS"
            listOf("loan statement","outstanding principal","emi").any(lower::contains) -> "Liability"
            else -> "Other"
        }
        val bank=listOf("HDFC","ICICI","SBI","STATE BANK","AXIS","KOTAK","PNB","BANK OF BARODA","CANARA","IDFC","INDUSIND").firstOrNull { lower.contains(it.lowercase()) }.orEmpty()
        val principal=valueAfter(text,"principal amount","deposit amount","initial deposit","amount deposited","investment amount","purchase value","principal")
        val mature=valueAfter(text,"maturity amount","maturity value","maturity proceeds","total maturity")
        val value= if(mature.isNotBlank()) mature else valueAfter(text,"current value","market value","closing balance","available balance","outstanding balance")
        val rate=Regex("""(?i)(?:interest rate|rate of interest|roi|int\. rate)[^\d]{0,15}(\d{1,2}(?:\.\d{1,3})?)\s*%?""").find(text)?.groupValues?.getOrNull(1).orEmpty()
        val ref=Regex("""(?i)(?:deposit|receipt|account|folio|reference)\s*(?:no|number|id|#)\s*[:#-]?\s*([A-Z0-9*X-]{4,})""").find(text)?.groupValues?.getOrNull(1).orEmpty()
        return WealthAsset(category=category,institution=bank,reference=ref,principal=principal,
            currentValue=value,rate=rate,opened=dateAfter(text,"opening date","start date","deposit date","booking date"),
            maturity=dateAfter(text,"maturity date","due date"),notes="")
    }
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF166A55),background=Color(0xFFF6F8F7))) {
                WealthHome()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WealthHome() {
    val context=LocalContext.current
    val prefs=remember { context.getSharedPreferences("wealth_mvp",android.content.Context.MODE_PRIVATE) }
    val assets=remember {
        mutableStateListOf<WealthAsset>().apply {
            runCatching {
                val array=JSONArray(prefs.getString("assets","[]"))
                repeat(array.length()){ add(WealthAsset.from(array.getJSONObject(it))) }
            }
        }
    }
    fun save() { prefs.edit().putString("assets", JSONArray().apply { assets.forEach { put(it.json()) } }.toString()).apply() }
    var draft by remember { mutableStateOf<WealthAsset?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf("All") }
    val recognizer=remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    DisposableEffect(recognizer) { onDispose { recognizer.close() } }
    fun analyze(image:InputImage) {
        scanning=true;error=""
        recognizer.process(image).addOnSuccessListener {
            scanning=false
            if(it.text.isBlank()) error="No readable text found. Try a clearer image or add manually."
            else draft=WealthRules.extract(it.text)
        }.addOnFailureListener {
            scanning=false;error="OCR could not read this image. Try another picture or add manually."
        }
    }
    val pick=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){ uri:Uri? ->
        if(uri!=null) runCatching { InputImage.fromFilePath(context,uri) }.onSuccess(::analyze).onFailure { error="Unable to open image" }
    }
    val photo=rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()){ bitmap ->
        if(bitmap!=null) analyze(InputImage.fromBitmap(bitmap,0))
    }
    val visible=assets.filter { selected=="All" || it.category==selected }
    val invested=assets.filter { it.category!="Liability" }.sumOf { amount(it.principal) }
    val value=assets.filter { it.category!="Liability" }.sumOf { amount(it.currentValue.ifBlank { it.principal }) }
    val debts=assets.filter { it.category=="Liability" }.sumOf { amount(it.currentValue.ifBlank { it.principal }) }
    Scaffold(topBar={ TopAppBar(title={Text("WealthVault",fontWeight=FontWeight.Bold)},actions={
        TextButton(onClick={draft=WealthAsset()}){Text("+ Add")}
    }) },floatingActionButton={
        ExtendedFloatingActionButton(onClick={pick.launch("image/*")},text={Text("Scan asset")})
    }) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text("Your wealth, organized",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
            Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF166A55)),modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("ESTIMATED NET WORTH",color=Color.White.copy(alpha=.8f))
                    Text(rupees(value-debts),style=MaterialTheme.typography.headlineMedium,color=Color.White,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text("Assets  ${rupees(value)}",color=Color.White)
                    Text("Liabilities  ${rupees(debts)}",color=Color.White)
                    Text("Recorded principal  ${rupees(invested)}",color=Color.White.copy(alpha=.8f))
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick={pick.launch("image/*")},modifier=Modifier.weight(1f)){Text("Upload image")}
                OutlinedButton(onClick={photo.launch(null)},modifier=Modifier.weight(1f)){Text("Camera")}
            }
            Text("Images are read on your phone. No AI API, account, or server.",style=MaterialTheme.typography.bodySmall)
            if(scanning) LinearProgressIndicator(Modifier.fillMaxWidth())
            if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error)
            Text("Asset stacks",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
            val existing=assets.map { it.category }.distinct()
            val filters=listOf("All")+existing
            Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                TextButton(onClick={expanded=true}){Text(selected+" ▾")}
                DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                    filters.forEach { item -> DropdownMenuItem(text={Text(item)},onClick={selected=item;expanded=false}) }
                }
            }
            if(visible.isEmpty()) Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp)) {
                    Text("No assets recorded yet",fontWeight=FontWeight.Bold)
                    Text("Upload an FD screenshot or add your first asset manually.")
                }
            }
            visible.groupBy { it.category }.forEach { (category,items) ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(category,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                        Text("${items.size} records • ${rupees(items.sumOf{amount(it.currentValue.ifBlank{it.principal})})})")
                        items.forEach { asset ->
                            HorizontalDivider()
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f).clickable { draft=asset }) {
                                    Text(asset.institution.ifBlank { "Unnamed asset" },fontWeight=FontWeight.SemiBold)
                                    Text(asset.reference.ifBlank { asset.maturity.ifBlank { "Tap to edit" } },style=MaterialTheme.typography.bodySmall)
                                }
                                Text(rupees(amount(asset.currentValue.ifBlank { asset.principal })),fontWeight=FontWeight.Medium)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
    if(draft!=null) {
        val current=draft!!
        var cat by remember(current.id) { mutableStateOf(current.category) }
        var institution by remember(current.id) { mutableStateOf(current.institution) }
        var reference by remember(current.id) { mutableStateOf(current.reference) }
        var principal by remember(current.id) { mutableStateOf(current.principal) }
        var currentValue by remember(current.id) { mutableStateOf(current.currentValue) }
        var rate by remember(current.id) { mutableStateOf(current.rate) }
        var opened by remember(current.id) { mutableStateOf(current.opened) }
        var maturity by remember(current.id) { mutableStateOf(current.maturity) }
        var notes by remember(current.id) { mutableStateOf(current.notes) }
        var categoryOpen by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest={draft=null},title={Text("Review asset details")},text={
            Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Confirm every value before saving. Unrecognized fields stay blank.",style=MaterialTheme.typography.bodySmall)
                Box {
                    OutlinedButton(onClick={categoryOpen=true}) { Text("Category: $cat ▾") }
                    DropdownMenu(expanded=categoryOpen,onDismissRequest={categoryOpen=false}) {
                        categories.forEach { option -> DropdownMenuItem(text={Text(option)},onClick={cat=option;categoryOpen=false}) }
                    }
                }
                @Composable fun Field(label:String,value:String,onValue:(String)->Unit) {
                    OutlinedTextField(value=value,onValueChange=onValue,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth())
                }
                Field("Bank / Institution",institution){institution=it}
                Field("Reference (optional)",reference){reference=it}
                Field("Principal / invested amount (₹)",principal){principal=it}
                Field("Current value / maturity (₹)",currentValue){currentValue=it}
                Field("Interest rate (%)",rate){rate=it}
                Field("Opening date",opened){opened=it}
                Field("Maturity date",maturity){maturity=it}
                Field("Notes",notes){notes=it}
                if(assets.any { it.id!=current.id && reference.isNotBlank() && it.reference.equals(reference,true) && it.institution.equals(institution,true) }) {
                    Text("Possible duplicate: same institution and reference.",color=MaterialTheme.colorScheme.error)
                }
            }
        },confirmButton={
            Button(enabled=institution.isNotBlank() || principal.isNotBlank() || currentValue.isNotBlank(),onClick={
                val record=current.copy(category=cat,institution=institution,reference=reference,principal=principal,currentValue=currentValue,rate=rate,opened=opened,maturity=maturity,notes=notes)
                val index=assets.indexOfFirst { it.id==record.id }
                if(index>=0) assets[index]=record else assets.add(record)
                save(); draft=null
            }){Text("Confirm & save")}
        },dismissButton={
            TextButton(onClick={draft=null}){Text("Cancel")}
        })
    }
}
