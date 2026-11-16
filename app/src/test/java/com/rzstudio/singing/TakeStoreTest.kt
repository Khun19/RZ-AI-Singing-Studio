package com.rzstudio.singing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TakeStoreTest {
    @Test
    fun deleteOnlyRemovesFilesInsideRecordingsDirectory() {
        val root = createTempDir(prefix = "rz-takes-test")
        try {
            val recordings = File(root, "recordings").apply { mkdirs() }
            val take = File(recordings, "take_test.m4a").apply { writeBytes(byteArrayOf(1, 2, 3)) }
            val outside = File(root, "keep.m4a").apply { writeBytes(byteArrayOf(4, 5, 6)) }

            assertTrue(TakeStore.delete(take))
            assertFalse(take.exists())
            assertFalse(TakeStore.delete(outside))
            assertTrue(outside.exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
