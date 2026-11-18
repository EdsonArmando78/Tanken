package mx.udg.alumnos.tanken;

import android.view.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;

import mx.udg.alumnos.tanken.databinding.ItemTimerBinding;

import java.util.Locale;
import java.util.function.IntConsumer;

final class TimerAdapter extends ListAdapter<ClassTimersViewModel.Row, TimerAdapter.Holder> {
    private final IntConsumer cancel;

    TimerAdapter(IntConsumer cancel) {
        super(
                new DiffUtil.ItemCallback<ClassTimersViewModel.Row>() {
                    @Override
                    public boolean areItemsTheSame(
                            @NonNull ClassTimersViewModel.Row a,
                            @NonNull ClassTimersViewModel.Row b) {
                        return a.id == b.id;
                    }

                    @Override
                    public boolean areContentsTheSame(
                            @NonNull ClassTimersViewModel.Row a,
                            @NonNull ClassTimersViewModel.Row b) {
                        return a.name.equals(b.name)
                                && a.status.equals(b.status)
                                && a.secondsLeft == b.secondsLeft;
                    }
                });
        this.cancel = cancel;
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemTimerBinding b;

        Holder(ItemTimerBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(
                ItemTimerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        ClassTimersViewModel.Row row = getItem(position);
        android.content.Context c = h.itemView.getContext();
        String time =
                String.format(
                        Locale.getDefault(),
                        "%02d:%02d:%02d",
                        row.secondsLeft / 3600,
                        row.secondsLeft / 60 % 60,
                        row.secondsLeft % 60);
        String status;
        switch (row.status) {
            case FINISHED:
                status = c.getString(R.string.timer_finished);
                break;
            case CANCELLED:
                status = c.getString(R.string.timer_cancelled);
                break;
            case WAITING:
                status = c.getString(R.string.timer_waiting, time);
                break;
            default:
                status = c.getString(R.string.timer_running, time);
        }
        h.b.description.setText(c.getString(R.string.timer_row, row.id, row.name, status));
        h.b.cancel.setText(c.getString(R.string.timer_cancel, row.id));
        h.b.cancel.setEnabled(
                row.status == ClassTimersViewModel.Status.RUNNING
                        || row.status == ClassTimersViewModel.Status.WAITING);
        h.b.cancel.setOnClickListener(v -> cancel.accept(row.id));
    }
}
