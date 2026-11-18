package mx.udg.alumnos.tanken;

import android.view.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;

import mx.udg.alumnos.tanken.databinding.ItemRecordBinding;

import java.util.function.Consumer;

final class RecordAdapter extends ListAdapter<HistoryRow, RecordAdapter.Holder> {
    private final Consumer<HistoryRow> open;

    RecordAdapter(Consumer<HistoryRow> open) {
        super(
                new DiffUtil.ItemCallback<HistoryRow>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull HistoryRow a, @NonNull HistoryRow b) {
                        return a.key().equals(b.key());
                    }

                    @Override
                    public boolean areContentsTheSame(
                            @NonNull HistoryRow a, @NonNull HistoryRow b) {
                        return a.at == b.at
                                && a.revision == b.revision
                                && a.kind.equals(b.kind)
                                && a.state.equals(b.state)
                                && a.origin.equals(b.origin);
                    }
                });
        this.open = open;
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemRecordBinding b;

        Holder(ItemRecordBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(
                ItemRecordBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        HistoryRow row = getItem(position);
        android.content.Context c = holder.itemView.getContext();
        holder.b.title.setText(RecordText.kind(c, row.kind));
        holder.b.summary.setText(
                c.getString(
                        R.string.record_summary,
                        RecordText.date(c, row.at),
                        RecordText.origin(c, row.origin),
                        row.state.isEmpty()
                                ? c.getString(R.string.record_open)
                                : RecordText.state(c, row.state)));
        holder.b.open.setOnClickListener(v -> open.accept(row));
    }
}
