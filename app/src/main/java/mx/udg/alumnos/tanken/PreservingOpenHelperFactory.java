package mx.udg.alumnos.tanken;

import androidx.sqlite.db.*;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;

/** Android's default corruption callback deletes files. Keep them for explicit recovery. */
final class PreservingOpenHelperFactory implements SupportSQLiteOpenHelper.Factory {
    @Override
    public SupportSQLiteOpenHelper create(SupportSQLiteOpenHelper.Configuration c) {
        SupportSQLiteOpenHelper.Callback delegate = c.callback;
        return new FrameworkSQLiteOpenHelperFactory()
                .create(
                        SupportSQLiteOpenHelper.Configuration.builder(c.context)
                                .name(c.name)
                                .callback(
                                        new SupportSQLiteOpenHelper.Callback(delegate.version) {
                                            @Override
                                            public void onConfigure(SupportSQLiteDatabase d) {
                                                delegate.onConfigure(d);
                                            }

                                            @Override
                                            public void onCreate(SupportSQLiteDatabase d) {
                                                delegate.onCreate(d);
                                            }

                                            @Override
                                            public void onUpgrade(
                                                    SupportSQLiteDatabase d, int a, int b) {
                                                delegate.onUpgrade(d, a, b);
                                            }

                                            @Override
                                            public void onDowngrade(
                                                    SupportSQLiteDatabase d, int a, int b) {
                                                delegate.onDowngrade(d, a, b);
                                            }

                                            @Override
                                            public void onOpen(SupportSQLiteDatabase d) {
                                                delegate.onOpen(d);
                                            }

                                            @Override
                                            public void onCorruption(SupportSQLiteDatabase d) {
                                                throw new IllegalStateException(
                                                        "Database corrupt; files preserved");
                                            }
                                        })
                                .build());
    }
}
