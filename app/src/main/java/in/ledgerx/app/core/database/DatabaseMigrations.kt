package com.ledgerx.app.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            TABLES.forEach { (name, columns, primaryKey) ->
                val pk = if (primaryKey.isBlank()) "" else ", PRIMARY KEY(${primaryKey.split(',').joinToString { "`$it`" }})"
                db.execSQL("CREATE TABLE IF NOT EXISTS `$name` ($columns$pk)")
            }
            INDICES.forEach { (table, unique, columns) ->
                val name = "index_${table}_${columns.joinToString("_")}"
                db.execSQL("CREATE ${if (unique) "UNIQUE " else ""}INDEX IF NOT EXISTS `$name` ON `$table` (${columns.joinToString { "`$it`" }})")
            }
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `categories` ADD COLUMN `parentCategoryId` TEXT")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_parentCategoryId` ON `categories` (`parentCategoryId`)")
            db.execSQL("ALTER TABLE `tax_profiles` ADD COLUMN `salaryIncomeMinor` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `tax_profiles` ADD COLUMN `interestIncomeMinor` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `tax_profiles` ADD COLUMN `dividendIncomeMinor` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `tax_profiles` ADD COLUMN `shortTermCapitalGain111AMinor` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `tax_profiles` ADD COLUMN `longTermCapitalGain112AMinor` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `deposits` ADD COLUMN `lockInEndEpochDay` INTEGER")
            db.execSQL("ALTER TABLE `deposits` ADD COLUMN `withdrawalRestrictions` TEXT")
            EXTENDED_TABLES.forEach { (name, columns, primaryKey) ->
                val pk = if (primaryKey.isBlank()) "" else ", PRIMARY KEY(${primaryKey.split(',').joinToString { "`$it`" }})"
                db.execSQL("CREATE TABLE IF NOT EXISTS `$name` ($columns$pk)")
            }
            EXTENDED_INDICES.forEach { (table, unique, columns) ->
                val name = "index_${table}_${columns.joinToString("_")}"
                db.execSQL("CREATE ${if (unique) "UNIQUE " else ""}INDEX IF NOT EXISTS `$name` ON `$table` (${columns.joinToString { "`$it`" }})")
            }
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `wealth_notes` (`id` TEXT NOT NULL, `holdingType` TEXT NOT NULL, `holdingId` TEXT NOT NULL, `title` TEXT NOT NULL, `body` TEXT NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        }
    }

    private data class Table(val name: String, val columns: String, val pk: String = "id")
    private data class DbIndex(val table: String, val unique: Boolean, val columns: List<String>)
    private fun ix(table: String, vararg columns: String, unique: Boolean = false) = DbIndex(table, unique, columns.toList())
    private val TABLES = listOf(
        Table("recurring_templates", "`id` TEXT NOT NULL, `accountId` TEXT NOT NULL, `type` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `currencyCode` TEXT NOT NULL, `categoryId` TEXT, `merchant` TEXT, `frequency` TEXT NOT NULL, `intervalCount` INTEGER NOT NULL, `nextDueEpochDay` INTEGER NOT NULL, `active` INTEGER NOT NULL"),
        Table("scheduled_occurrences", "`id` TEXT NOT NULL, `templateId` TEXT NOT NULL, `dueEpochDay` INTEGER NOT NULL, `status` TEXT NOT NULL, `transactionId` TEXT"),
        Table("bills", "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `merchant` TEXT, `amountMinor` INTEGER NOT NULL, `currencyCode` TEXT NOT NULL, `dueEpochDay` INTEGER NOT NULL, `frequency` TEXT, `status` TEXT NOT NULL, `pricePreviousMinor` INTEGER, `reminderDaysBefore` INTEGER NOT NULL"),
        Table("import_batches", "`id` TEXT NOT NULL, `accountId` TEXT NOT NULL, `fileName` TEXT NOT NULL, `sourceHash` TEXT NOT NULL, `rowCount` INTEGER NOT NULL, `acceptedCount` INTEGER NOT NULL, `rejectedCount` INTEGER NOT NULL, `status` TEXT NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL"),
        Table("imported_records", "`id` TEXT NOT NULL, `batchId` TEXT NOT NULL, `fingerprint` TEXT NOT NULL, `sourceRow` INTEGER NOT NULL, `transactionId` TEXT, `status` TEXT NOT NULL, `error` TEXT"),
        Table("reconciliations", "`id` TEXT NOT NULL, `accountId` TEXT NOT NULL, `statementEndEpochDay` INTEGER NOT NULL, `statementBalanceMinor` INTEGER NOT NULL, `ledgerBalanceMinor` INTEGER NOT NULL, `differenceMinor` INTEGER NOT NULL, `completedAtEpochMillis` INTEGER"),
        Table("investment_instruments", "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `symbol` TEXT, `exchange` TEXT, `isin` TEXT, `assetClass` TEXT NOT NULL, `currencyCode` TEXT NOT NULL"),
        Table("investment_transactions", "`id` TEXT NOT NULL, `instrumentId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `type` TEXT NOT NULL, `quantityMicros` INTEGER NOT NULL, `priceMinor` INTEGER NOT NULL, `feesMinor` INTEGER NOT NULL, `tradeEpochDay` INTEGER NOT NULL, `ledgerTransactionId` TEXT"),
        Table("valuation_snapshots", "`id` TEXT NOT NULL, `instrumentId` TEXT NOT NULL, `priceMinor` INTEGER NOT NULL, `currencyCode` TEXT NOT NULL, `priceEpochDay` INTEGER NOT NULL, `source` TEXT NOT NULL, `capturedAtEpochMillis` INTEGER NOT NULL"),
        Table("deposits", "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `principalMinor` INTEGER NOT NULL, `annualRateBasisPoints` INTEGER NOT NULL, `openedEpochDay` INTEGER NOT NULL, `maturityEpochDay` INTEGER NOT NULL, `maturityMinor` INTEGER, `accountId` TEXT"),
        Table("retirement_contributions", "`id` TEXT NOT NULL, `scheme` TEXT NOT NULL, `employeeMinor` INTEGER NOT NULL, `employerMinor` INTEGER NOT NULL, `pensionMinor` INTEGER NOT NULL, `contributionEpochDay` INTEGER NOT NULL, `financialYear` TEXT NOT NULL, `note` TEXT"),
        Table("physical_assets", "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `purchaseMinor` INTEGER NOT NULL, `purchaseEpochDay` INTEGER NOT NULL, `ownershipBasisPoints` INTEGER NOT NULL, `linkedLoanId` TEXT, `weightMilligrams` INTEGER, `purityBasisPoints` INTEGER, `transactionCostMinor` INTEGER, `archived` INTEGER NOT NULL"),
        Table("asset_valuations", "`id` TEXT NOT NULL, `assetId` TEXT NOT NULL, `estimatedMarketMinor` INTEGER NOT NULL, `estimatedResaleMinor` INTEGER, `valuationEpochDay` INTEGER NOT NULL, `source` TEXT NOT NULL, `assumptions` TEXT"),
        Table("loans", "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `originalPrincipalMinor` INTEGER NOT NULL, `outstandingPrincipalMinor` INTEGER NOT NULL, `annualRateBasisPoints` INTEGER NOT NULL, `emiMinor` INTEGER NOT NULL, `remainingMonths` INTEGER NOT NULL, `nextDueEpochDay` INTEGER, `active` INTEGER NOT NULL"),
        Table("loan_events", "`id` TEXT NOT NULL, `loanId` TEXT NOT NULL, `type` TEXT NOT NULL, `principalMinor` INTEGER NOT NULL, `interestMinor` INTEGER NOT NULL, `feeMinor` INTEGER NOT NULL, `annualRateBasisPoints` INTEGER, `eventEpochDay` INTEGER NOT NULL, `ledgerTransactionId` TEXT"),
        Table("insurance_policies", "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `insurer` TEXT NOT NULL, `coverageMinor` INTEGER NOT NULL, `premiumMinor` INTEGER NOT NULL, `deductibleMinor` INTEGER, `renewalEpochDay` INTEGER NOT NULL, `nominees` TEXT, `exclusions` TEXT, `documentUri` TEXT, `active` INTEGER NOT NULL"),
        Table("goals", "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `targetMinor` INTEGER NOT NULL, `targetEpochDay` INTEGER NOT NULL, `monthlyContributionMinor` INTEGER NOT NULL, `status` TEXT NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL"),
        Table("goal_allocations", "`goalId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `allocatedMinor` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL", "goalId,accountId"),
        Table("tax_profiles", "`id` TEXT NOT NULL, `financialYear` TEXT NOT NULL, `preferredRegime` TEXT, `grossIncomeMinor` INTEGER NOT NULL, `eligibleDeductionsMinor` INTEGER NOT NULL, `tdsMinor` INTEGER NOT NULL, `advanceTaxMinor` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL"),
        Table("tax_rules", "`id` TEXT NOT NULL, `financialYear` TEXT NOT NULL, `regime` TEXT NOT NULL, `version` INTEGER NOT NULL, `effectiveEpochDay` INTEGER NOT NULL, `ruleJson` TEXT NOT NULL, `verifiedSource` TEXT NOT NULL, `verifiedAtEpochMillis` INTEGER NOT NULL"),
        Table("tax_payments", "`id` TEXT NOT NULL, `financialYear` TEXT NOT NULL, `type` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `paidEpochDay` INTEGER NOT NULL, `reference` TEXT"),
        Table("health_snapshots", "`id` TEXT NOT NULL, `score` INTEGER, `completenessPercent` INTEGER NOT NULL, `dimensionsJson` TEXT NOT NULL, `warningsJson` TEXT NOT NULL, `calculatedAtEpochMillis` INTEGER NOT NULL"),
        Table("financial_insights", "`id` TEXT NOT NULL, `type` TEXT NOT NULL, `title` TEXT NOT NULL, `evidence` TEXT NOT NULL, `impactMinor` INTEGER, `confidence` TEXT NOT NULL, `suggestedAction` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL"),
        Table("forecast_results", "`id` TEXT NOT NULL, `scenario` TEXT NOT NULL, `horizonMonths` INTEGER NOT NULL, `assumptionsJson` TEXT NOT NULL, `pointsJson` TEXT NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL"),
        Table("notification_schedules", "`id` TEXT NOT NULL, `sourceType` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `title` TEXT NOT NULL, `dueAtEpochMillis` INTEGER NOT NULL, `channel` TEXT NOT NULL, `enabled` INTEGER NOT NULL"),
        Table("backup_metadata", "`id` TEXT NOT NULL, `formatVersion` INTEGER NOT NULL, `fileName` TEXT NOT NULL, `checksumSha256` TEXT NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, `verifiedAtEpochMillis` INTEGER, `recordCount` INTEGER NOT NULL"),
    )
    private val INDICES = listOf(
        ix("recurring_templates","nextDueEpochDay"),ix("recurring_templates","active"), ix("scheduled_occurrences","templateId"),ix("scheduled_occurrences","dueEpochDay"),ix("scheduled_occurrences","status"),
        ix("bills","dueEpochDay"),ix("bills","status"),ix("bills","merchant"), ix("import_batches","sourceHash"),ix("import_batches","createdAtEpochMillis"), ix("imported_records","fingerprint",unique=true),ix("imported_records","batchId"),
        ix("reconciliations","accountId"),ix("reconciliations","statementEndEpochDay"), ix("investment_instruments","symbol","exchange",unique=true),ix("investment_instruments","assetClass"),
        ix("investment_transactions","instrumentId"),ix("investment_transactions","accountId"),ix("investment_transactions","tradeEpochDay"), ix("valuation_snapshots","instrumentId","priceEpochDay",unique=true),
        ix("deposits","maturityEpochDay"),ix("deposits","type"),ix("retirement_contributions","scheme"),ix("retirement_contributions","contributionEpochDay"), ix("physical_assets","type"),ix("physical_assets","archived"),
        ix("asset_valuations","assetId","valuationEpochDay",unique=true),ix("loans","type"),ix("loans","active"),ix("loan_events","loanId"),ix("loan_events","eventEpochDay"),
        ix("insurance_policies","type"),ix("insurance_policies","renewalEpochDay"),ix("insurance_policies","active"),ix("goals","targetEpochDay"),ix("goals","status"),ix("goal_allocations","accountId"),
        ix("tax_profiles","financialYear",unique=true),ix("tax_rules","financialYear","regime","version",unique=true),ix("tax_payments","financialYear"),ix("tax_payments","paidEpochDay"),
        ix("health_snapshots","calculatedAtEpochMillis"),ix("financial_insights","status"),ix("financial_insights","createdAtEpochMillis"),ix("financial_insights","type"),
        ix("forecast_results","scenario"),ix("forecast_results","createdAtEpochMillis"),ix("notification_schedules","dueAtEpochMillis"),ix("notification_schedules","enabled"),ix("backup_metadata","createdAtEpochMillis"),
    )

    private val EXTENDED_TABLES = listOf(
        Table("local_profiles", "`id` TEXT NOT NULL, `displayName` TEXT NOT NULL, `baseCurrencyCode` TEXT NOT NULL, `localeTag` TEXT NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL"),
        Table("merchants", "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `normalizedName` TEXT NOT NULL, `defaultCategoryId` TEXT, `usageCount` INTEGER NOT NULL, `lastUsedAtEpochMillis` INTEGER NOT NULL"),
        Table("tags", "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `colorArgb` INTEGER"),
        Table("transaction_tags", "`transactionId` TEXT NOT NULL, `tagId` TEXT NOT NULL", "transactionId,tagId"),
        Table("receipt_attachments", "`id` TEXT NOT NULL, `transactionId` TEXT NOT NULL, `displayName` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `storedFileName` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL, `sha256` TEXT NOT NULL, `content` BLOB NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL"),
        Table("investment_lots", "`id` TEXT NOT NULL, `instrumentId` TEXT NOT NULL, `openedByTransactionId` TEXT NOT NULL, `openedEpochDay` INTEGER NOT NULL, `originalQuantityMicros` INTEGER NOT NULL, `remainingQuantityMicros` INTEGER NOT NULL, `totalCostMinor` INTEGER NOT NULL, `remainingCostMinor` INTEGER NOT NULL, `realizedProceedsMinor` INTEGER NOT NULL, `realizedGainMinor` INTEGER NOT NULL, `closedEpochDay` INTEGER"),
        Table("dividend_records", "`id` TEXT NOT NULL, `instrumentId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `grossMinor` INTEGER NOT NULL, `taxWithheldMinor` INTEGER NOT NULL, `receivedEpochDay` INTEGER NOT NULL, `ledgerTransactionId` TEXT NOT NULL"),
        Table("sip_schedules", "`id` TEXT NOT NULL, `instrumentId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `frequency` TEXT NOT NULL, `intervalCount` INTEGER NOT NULL, `nextDueEpochDay` INTEGER NOT NULL, `active` INTEGER NOT NULL"),
        Table("deposit_contributions", "`id` TEXT NOT NULL, `depositId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `contributionEpochDay` INTEGER NOT NULL, `ledgerTransactionId` TEXT NOT NULL"),
        Table("retirement_events", "`id` TEXT NOT NULL, `scheme` TEXT NOT NULL, `type` TEXT NOT NULL, `employerName` TEXT, `amountMinor` INTEGER, `eventEpochDay` INTEGER NOT NULL, `note` TEXT"),
        Table("asset_maintenance", "`id` TEXT NOT NULL, `assetId` TEXT NOT NULL, `description` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `occurredEpochDay` INTEGER NOT NULL, `ledgerTransactionId` TEXT"),
        Table("loan_schedule", "`id` TEXT NOT NULL, `loanId` TEXT NOT NULL, `installmentNumber` INTEGER NOT NULL, `dueEpochDay` INTEGER NOT NULL, `openingMinor` INTEGER NOT NULL, `emiMinor` INTEGER NOT NULL, `principalMinor` INTEGER NOT NULL, `interestMinor` INTEGER NOT NULL, `closingMinor` INTEGER NOT NULL, `status` TEXT NOT NULL"),
        Table("credit_card_statements", "`id` TEXT NOT NULL, `accountId` TEXT NOT NULL, `statementEpochDay` INTEGER NOT NULL, `dueEpochDay` INTEGER NOT NULL, `statementBalanceMinor` INTEGER NOT NULL, `minimumDueMinor` INTEGER NOT NULL, `paidMinor` INTEGER NOT NULL, `status` TEXT NOT NULL"),
        Table("dashboard_widgets", "`profileId` TEXT NOT NULL, `widgetKey` TEXT NOT NULL, `position` INTEGER NOT NULL, `visible` INTEGER NOT NULL", "profileId,widgetKey"),
        Table("bill_price_history", "`id` TEXT NOT NULL, `billId` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `effectiveEpochDay` INTEGER NOT NULL"),
        Table("policy_documents", "`id` TEXT NOT NULL, `policyId` TEXT NOT NULL, `displayName` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL, `sha256` TEXT NOT NULL, `content` BLOB NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL"),
        Table("planning_assumptions", "`id` TEXT NOT NULL, `essentialMonthlyExpenseMinor` INTEGER NOT NULL, `statedInsuranceNeedMinor` INTEGER NOT NULL, `currentAge` INTEGER, `retirementAge` INTEGER, `retirementTargetMinor` INTEGER, `inflationBasisPoints` INTEGER NOT NULL, `returnBasisPoints` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL"),
        Table("income_breakdowns", "`id` TEXT NOT NULL, `transactionId` TEXT NOT NULL, `grossMinor` INTEGER NOT NULL, `takeHomeMinor` INTEGER NOT NULL, `deductionsMinor` INTEGER NOT NULL, `taxWithheldMinor` INTEGER NOT NULL, `occurredAtEpochMillis` INTEGER NOT NULL"),
    )

    private val EXTENDED_INDICES = listOf(
        ix("merchants", "normalizedName", unique = true), ix("tags", "name", unique = true), ix("transaction_tags", "tagId"), ix("receipt_attachments", "transactionId"),
        ix("investment_lots", "instrumentId"), ix("investment_lots", "openedByTransactionId"), ix("investment_lots", "closedEpochDay"),
        ix("dividend_records", "instrumentId"), ix("dividend_records", "receivedEpochDay"), ix("sip_schedules", "instrumentId"), ix("sip_schedules", "nextDueEpochDay"), ix("sip_schedules", "active"),
        ix("deposit_contributions", "depositId"), ix("deposit_contributions", "contributionEpochDay"), ix("retirement_events", "scheme"), ix("retirement_events", "eventEpochDay"),
        ix("asset_maintenance", "assetId"), ix("asset_maintenance", "occurredEpochDay"), ix("loan_schedule", "loanId"), ix("loan_schedule", "loanId", "installmentNumber", unique = true), ix("loan_schedule", "dueEpochDay"),
        ix("credit_card_statements", "accountId"), ix("credit_card_statements", "accountId", "statementEpochDay", unique = true), ix("credit_card_statements", "dueEpochDay"), ix("dashboard_widgets", "position"),
        ix("bill_price_history", "billId"), ix("bill_price_history", "effectiveEpochDay"),
        ix("policy_documents", "policyId"),
        ix("income_breakdowns", "transactionId"),
    )
}
