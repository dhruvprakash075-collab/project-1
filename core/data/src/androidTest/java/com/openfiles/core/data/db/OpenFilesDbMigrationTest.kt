package com.openfiles.core.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.openfiles.core.data.di.MIGRATION_3_4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class OpenFilesDbMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        OpenFilesDb::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migrate3To4CreatesDocAnnotations() {
        helper.createDatabase(TEST_DB, 3).apply {
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4).apply {
            query("SELECT documentUri, anchorIndex, sheetName, note, createdAt FROM doc_annotations LIMIT 0").close()
            close()
        }
    }

    private companion object {
        const val TEST_DB = "openfiles-migration-test"
    }
}
