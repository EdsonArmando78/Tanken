package mx.udg.alumnos.tanken;

/** Database half of reset: records, catalog and recovery markers commit together. */
final class ResetData {
    private ResetData() {}

    static void clear(TankenDatabase db, Catalog bundled, long clearedAt) {
        db.runInTransaction(
                () -> {
                    db.records().deleteAllLessons();
                    db.records().deleteAllNotices();
                    db.records().deleteMotion(Long.MIN_VALUE, Long.MAX_VALUE);
                    db.records().deleteComparisons(Long.MIN_VALUE, Long.MAX_VALUE);
                    db.records().putMetadata(new TankenDatabase.Metadata("scheduleImported", "1"));
                    db.records().putMetadata(new TankenDatabase.Metadata("importWarning", ""));
                    db.records()
                            .putMetadata(
                                    new TankenDatabase.Metadata(
                                            "historyClearedAt", Long.toString(clearedAt)));
                    db.records().putMetadata(new TankenDatabase.Metadata("remindersDisabled", "1"));
                    CatalogRepository.persist(db, bundled, false, 0);
                    db.records().putMetadata(new TankenDatabase.Metadata("resetPending", "0"));
                });
    }
}
