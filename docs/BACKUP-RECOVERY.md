# Backup and recovery

1. Open **Health → Backup and data**.
2. Enter a unique password of at least 12 characters.
3. Choose **Create backup**, select a local removable/private destination, and retain the resulting `.lxb` file.
4. Keep the password separately; it is not stored in the backup and cannot be reset.

To recover on another Android device, install LedgerX, open **Health → Backup and data**, enter the original password, choose **Restore**, and select the `.lxb` file. LedgerX authenticates and parses the complete file before beginning one database transaction. A wrong password, damaged file, missing table, unsupported format, or record-count mismatch leaves the existing database unchanged. Format v3 includes the complete v3 schema, bill price history, tags, investment lots, schedules, card statements, and encrypted receipt/document bytes; v1 and v2 backups remain readable and restore absent newer tables as empty.

After recovery, compare account balances, latest transactions, investments, liabilities, and the backup verification message. A verified restore event is written to backup metadata. Plain CSV exports are reports, not restorable backups.
