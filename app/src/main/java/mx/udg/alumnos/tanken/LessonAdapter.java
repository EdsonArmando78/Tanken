package mx.udg.alumnos.tanken;

import android.view.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;

import mx.udg.alumnos.tanken.databinding.ItemLessonBinding;

import java.util.function.Consumer;

final class LessonAdapter extends ListAdapter<AcademicSchedule.Lesson, LessonAdapter.Holder> {
    private final Consumer<AcademicSchedule.Lesson> edit, delete;

    LessonAdapter(
            Consumer<AcademicSchedule.Lesson> edit, Consumer<AcademicSchedule.Lesson> delete) {
        super(
                new DiffUtil.ItemCallback<AcademicSchedule.Lesson>() {
                    @Override
                    public boolean areItemsTheSame(
                            @NonNull AcademicSchedule.Lesson a,
                            @NonNull AcademicSchedule.Lesson b) {
                        return a.id.equals(b.id);
                    }

                    @Override
                    public boolean areContentsTheSame(
                            @NonNull AcademicSchedule.Lesson a,
                            @NonNull AcademicSchedule.Lesson b) {
                        return a.name.equals(b.name)
                                && a.day.equals(b.day)
                                && a.start.equals(b.start)
                                && a.end.equals(b.end);
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
        AcademicSchedule.Lesson l = getItem(position);
        android.content.Context c = h.itemView.getContext();
        h.b.name.setText(l.name);
        h.b.time.setText(
                c.getString(
                        R.string.schedule_row,
                        l.name,
                        c.getResources()
                                .getStringArray(R.array.schedule_days)[l.day.getValue() - 1],
                        l.start.toString(),
                        l.end.toString()));
        h.b.edit.setOnClickListener(v -> edit.accept(l));
        h.b.delete.setOnClickListener(v -> delete.accept(l));
    }
}
