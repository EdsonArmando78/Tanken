package mx.udg.alumnos.tanken;

import android.view.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;

import mx.udg.alumnos.tanken.databinding.ItemLessonBinding;

import java.util.function.Consumer;

final class LessonAdapter extends ListAdapter<LessonGroup, LessonAdapter.Holder> {
    private final Consumer<AcademicSchedule.Lesson> edit, delete;

    LessonAdapter(
            Consumer<AcademicSchedule.Lesson> edit, Consumer<AcademicSchedule.Lesson> delete) {
        super(
                new DiffUtil.ItemCallback<LessonGroup>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull LessonGroup a, @NonNull LessonGroup b) {
                        return a.first.groupId.equals(b.first.groupId);
                    }

                    @Override
                    public boolean areContentsTheSame(
                            @NonNull LessonGroup a, @NonNull LessonGroup b) {
                        return a.first.name.equals(b.first.name)
                                && a.first.room.equals(b.first.room)
                                && a.days == b.days
                                && a.first.start.equals(b.first.start)
                                && a.first.end.equals(b.first.end);
                    }
                });
        this.edit = edit;
        this.delete = delete;
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemLessonBinding b;

        Holder(ItemLessonBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(
                ItemLessonBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        LessonGroup group = getItem(position);
        AcademicSchedule.Lesson l = group.first;
        android.content.Context c = h.itemView.getContext();
        h.b.name.setText(l.name);
        java.util.List<String> days = new java.util.ArrayList<>();
        String[] labels = c.getResources().getStringArray(R.array.schedule_days);
        for (int day = 0; day < labels.length; day++)
            if ((group.days & (1 << day)) != 0) days.add(labels[day]);
        h.b.time.setText(
                c.getString(
                        R.string.schedule_group_time,
                        String.join(", ", days),
                        l.start.toString(),
                        l.end.toString()));
        h.b.room.setText(c.getString(R.string.schedule_room, l.room));
        h.b.room.setVisibility(l.room.isEmpty() ? View.GONE : View.VISIBLE);
        h.b.edit.setOnClickListener(v -> edit.accept(l));
        h.b.delete.setOnClickListener(v -> delete.accept(l));
    }
}
