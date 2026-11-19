package com.rzstudio.singing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TakeStoreTest {
    @Test
    fun deleteOnlyRemovesFilesInsideActualRecordingsDirectory() {
        val root = createTempDir(prefix = "rz-takes-test")
        try {
            val recordings = File(root, "recordings").apply { mkdirs() }
            val take = File(recordings, "take_test.m4a").apply { writeBytes(byteArrayOf(1, 2, 3)) }
            val outside = File(root, "keep.m4a").apply { writeBytes(byteArrayOf(4, 5, 6)) }
            val unrelatedRecordings = File(root, "other/recordings").apply { mkdirs() }
            val unrelatedTake = File(unrelatedRecordings, "do-not-delete.m4a")
                .apply { writeBytes(byteArrayOf(7, 8, 9)) }
            val traversal = File(recordings, "../keep.m4a")

            assertTrue(TakeStore.delete(take, recordings))
            assertFalse(take.exists())
            assertFalse(TakeStore.delete(outside, recordings))
            assertTrue(outside.exists())
            assertFalse(TakeStore.delete(unrelatedTake, recordings))
            assertTrue(unrelatedTake.exists())
            assertFalse(TakeStore.delete(traversal, recordings))
            assertTrue(outside.exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
