package com.ledgerx.app.core.reporting

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import com.ledgerx.app.core.database.LedgerDao
import com.ledgerx.app.core.database.PlanningDao
import com.ledgerx.app.core.database.ExtendedDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ledgerDao: LedgerDao,
    private val planningDao: PlanningDao,
    private val extendedDao: ExtendedDao,
) {
    suspend fun exportTransactions(uri: Uri): Int = withContext(Dispatchers.IO) {
        val rows = ledgerDao.allTransactionsForExport()
        val csv = buildString {
            appendLine("date,type,amount,currency,account,category,merchant,note")
            rows.forEach { row ->
                val date = Instant.ofEpochMilli(row.occurredAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                appendLine(listOf(date, row.type, major(row.amountMinor), row.currencyCode, row.accountName, row.categoryId, row.merchant, row.note).joinToString(",") { csv(it?.toString().orEmpty()) })
            }
        }
        write(uri, csv)
        rows.size
    }

    suspend fun exportCaReport(uri: Uri, financialYear: String): Int = withContext(Dispatchers.IO) {
        val profile = requireNotNull(planningDao.taxProfile(financialYear)) { "Save the tax profile before exporting a CA report" }
        val payments = planningDao.taxPayments(financialYear)
        val startYear = financialYear.substringBefore('-').toInt()
        val start = LocalDate.of(startYear, 4, 1)
        val end = start.plusYears(1)
        val transactions = ledgerDao.allTransactionsForExport().filter { row ->
            val date = Instant.ofEpochMilli(row.occurredAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            !date.isBefore(start) && date.isBefore(end)
        }
        val csv = buildString {
            appendLine("LedgerX CA working export")
            appendLine("financial_year,${csv(financialYear)}")
            appendLine("generated_on,${LocalDate.now()}")
            appendLine("warning,${csv("User-entered offline working data; verify source documents and current law with a qualified professional.")}")
            appendLine()
            appendLine("profile_item,amount_inr")
            appendLine("gross_income,${major(profile.grossIncomeMinor)}")
            appendLine("salary_or_pension,${major(profile.salaryIncomeMinor)}")
            appendLine("interest_income,${major(profile.interestIncomeMinor)}")
            appendLine("dividend_income,${major(profile.dividendIncomeMinor)}")
            appendLine("stcg_section_111a,${major(profile.shortTermCapitalGain111AMinor)}")
            appendLine("ltcg_section_112a,${major(profile.longTermCapitalGain112AMinor)}")
            appendLine("eligible_deductions,${major(profile.eligibleDeductionsMinor)}")
            appendLine("tds,${major(profile.tdsMinor)}")
            appendLine("advance_tax,${major(profile.advanceTaxMinor)}")
            appendLine("preferred_regime,${csv(profile.preferredRegime.orEmpty())}")
            appendLine("scope_warning,${csv("Capital-gain classification, holding period, STT eligibility, loss set-off, grandfathering, surcharge and marginal relief require independent verification.")}")
            appendLine()
            appendLine("payment_date,type,amount_inr,reference")
            payments.forEach { appendLine("${LocalDate.ofEpochDay(it.paidEpochDay)},${csv(it.type)},${major(it.amountMinor)},${csv(it.reference.orEmpty())}") }
            appendLine()
            appendLine("ledger_date,type,amount_inr,account,category,party,note")
            transactions.forEach { row ->
                val date = Instant.ofEpochMilli(row.occurredAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                appendLine(listOf(date,row.type,major(row.amountMinor),row.accountName,row.categoryId,row.merchant,row.note).joinToString(","){csv(it?.toString().orEmpty())})
            }
        }
        write(uri, csv)
        payments.size + transactions.size + 1
    }

    suspend fun exportFinancialReport(uri: Uri): Int = withContext(Dispatchers.IO) {
        val accounts=ledgerDao.observeAccountBalances().first(); val bills=planningDao.observeBills().first(); val loans=planningDao.observeLoans().first(); val policies=planningDao.observePolicies().first(); val goals=planningDao.observeGoals().first(); val allocations=planningDao.observeGoalAllocations().first()
        val instruments=planningDao.observeInstruments().first(); val trades=planningDao.observeInvestmentTransactions().first(); val valuations=planningDao.observeValuations().first(); val deposits=planningDao.observeDeposits().first(); val retirement=planningDao.observeRetirementContributions().first(); val assets=planningDao.observeAssets().first(); val assetValues=planningDao.observeAssetValuations().first(); val health=planningDao.observeLatestHealth().first(); val retirementEvents=extendedDao.observeRetirementEvents().first()
        val investmentValue=instruments.sumOf { instrument -> val quantity=trades.filter{it.instrumentId==instrument.id}.sumOf{if(it.type=="SELL")-it.quantityMicros else it.quantityMicros}; val price=valuations.filter{it.instrumentId==instrument.id}.maxByOrNull{it.priceEpochDay}?.priceMinor ?: trades.filter{it.instrumentId==instrument.id}.maxByOrNull{it.tradeEpochDay}?.priceMinor ?: 0L; BigDecimal(quantity).multiply(BigDecimal(price)).divide(BigDecimal(1_000_000),0,java.math.RoundingMode.HALF_UP).longValueExact() }
        val retirementValue=(retirement.filter{it.scheme!="EPS"}.sumOf{it.employeeMinor+it.employerMinor}-retirementEvents.filter{it.type=="WITHDRAWAL"}.sumOf{it.amountMinor?:0}).coerceAtLeast(0)
        val physicalValue=assets.sumOf { asset -> val latest=assetValues.filter{it.assetId==asset.id}.maxByOrNull{it.valuationEpochDay}?.estimatedMarketMinor?:asset.purchaseMinor; BigDecimal(latest).multiply(BigDecimal(asset.ownershipBasisPoints)).divide(BigDecimal(10_000),0,java.math.RoundingMode.HALF_UP).longValueExact() }
        val netWorth=accounts.sumOf{it.balanceMinor}+investmentValue+deposits.sumOf{it.principalMinor}+retirementValue+physicalValue-loans.sumOf{it.outstandingPrincipalMinor}
        val report=buildString {
            appendLine("LedgerX advanced financial report"); appendLine("generated_on,${LocalDate.now()}"); appendLine("scope,${csv("Offline user-entered records and dated manual valuations; verify before making financial decisions.")}"); appendLine()
            appendLine("summary_item,amount_inr"); appendLine("net_worth,${major(netWorth)}"); appendLine("accounts_net,${major(accounts.sumOf{it.balanceMinor})}"); appendLine("investments,${major(investmentValue)}"); appendLine("deposits,${major(deposits.sumOf{it.principalMinor})}"); appendLine("retirement_excluding_eps,${major(retirementValue)}"); appendLine("physical_assets_owned_share,${major(physicalValue)}"); appendLine("loans_outstanding,${major(loans.sumOf{it.outstandingPrincipalMinor})}"); appendLine("health_score,${health?.score?.toString().orEmpty()}"); appendLine("health_completeness_percent,${health?.completenessPercent?:0}"); appendLine()
            appendLine("account,type,balance,currency"); accounts.forEach{appendLine(listOf(it.name,it.kind,major(it.balanceMinor),it.currencyCode).joinToString(","){v->csv(v)})}; appendLine()
            appendLine("investment,asset_class,quantity,current_value,last_price_date,source"); instruments.forEach { instrument -> val quantity=trades.filter{it.instrumentId==instrument.id}.sumOf{if(it.type=="SELL")-it.quantityMicros else it.quantityMicros}; val value=valuations.filter{it.instrumentId==instrument.id}.maxByOrNull{it.priceEpochDay}; val current=value?.let{BigDecimal(quantity).multiply(BigDecimal(it.priceMinor)).divide(BigDecimal(1_000_000),0,java.math.RoundingMode.HALF_UP).longValueExact()}; appendLine(listOf(instrument.name,instrument.assetClass,BigDecimal.valueOf(quantity,6).stripTrailingZeros().toPlainString(),current?.let(::major).orEmpty(),value?.let{LocalDate.ofEpochDay(it.priceEpochDay).toString()}.orEmpty(),value?.source.orEmpty()).joinToString(","){csv(it)}) }; appendLine()
            appendLine("loan,type,outstanding,rate_percent,emi,remaining_months"); loans.forEach{appendLine(listOf(it.name,it.type,major(it.outstandingPrincipalMinor),BigDecimal.valueOf(it.annualRateBasisPoints.toLong(),2).toPlainString(),major(it.emiMinor),it.remainingMonths.toString()).joinToString(","){v->csv(v)})}; appendLine()
            appendLine("goal,target,allocated,target_date,monthly_contribution"); goals.forEach{goal->appendLine(listOf(goal.name,major(goal.targetMinor),major(allocations.filter{it.goalId==goal.id}.sumOf{it.allocatedMinor}),LocalDate.ofEpochDay(goal.targetEpochDay).toString(),major(goal.monthlyContributionMinor)).joinToString(","){csv(it)})}; appendLine()
            appendLine("policy,type,insurer,coverage,premium,renewal"); policies.forEach{appendLine(listOf(it.name,it.type,it.insurer,major(it.coverageMinor),major(it.premiumMinor),LocalDate.ofEpochDay(it.renewalEpochDay).toString()).joinToString(","){v->csv(v)})}; appendLine()
            appendLine("upcoming_bill,amount,due,status"); bills.forEach{appendLine(listOf(it.name,major(it.amountMinor),LocalDate.ofEpochDay(it.dueEpochDay).toString(),it.status).joinToString(","){v->csv(v)})}
        }
        write(uri,report)
        accounts.size+instruments.size+loans.size+goals.size+policies.size+bills.size+1
    }

    private fun write(uri: Uri, text: String) {
        requireNotNull(context.contentResolver.openOutputStream(uri, "w")) { "Cannot open export destination" }.bufferedWriter().use { it.write(text) }
    }

    private fun major(minor: Long) = BigDecimal.valueOf(minor, 2).setScale(2).toPlainString()
    private fun csv(value: String) = "\"${value.replace("\"", "\"\"")}\""
}
