package mx.udg.alumnos.tanken;

public final class RecordDetail {
    public final String title, body, comparisonId;
    public final TankenDatabase.NoticeRow notice;

    RecordDetail(String title, String body, TankenDatabase.NoticeRow notice, String comparisonId) {
        this.title = title;
        this.body = body;
        this.notice = notice;
        this.comparisonId = comparisonId;
    }
}
