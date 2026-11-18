package mx.udg.alumnos.tanken;

/** Small list projection; comparison payloads and transition chains are loaded only on demand. */
public final class HistoryRow {
    public String id, category, kind, state, origin;
    public long at, revision;

    public String key() {
        return category + ":" + id;
    }
}
