package ru.fizteh.fivt.students.egor_belikov.Parallel.UnitTests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.students.egor_belikov.Parallel.MyStoreable;
import ru.fizteh.fivt.students.egor_belikov.Parallel.MyTable;
import ru.fizteh.fivt.students.egor_belikov.Parallel.MyTableProvider;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class UnitTestsForMyTable {

    private final String testDir = System.getProperty("java.io.tmpdir") + File.separator + "MyTableTest";
    private final String testKey1 = "ключ тестирования";
    private final String testKey2 = "тестирования ключ";
    private Table test;
    private Storeable correctStoreable;


    @Before
    public void setUp() {
        File dir = new File(testDir);
        dir.mkdir();
        String tableName = "table1";
        String tableDirectoryPath = testDir + File.separator + tableName;
        File tableDir = new File(tableDirectoryPath);
        tableDir.mkdir();
        List<Class<?>> sig = new ArrayList<>();
        sig.add(Integer.class);
        sig.add(String.class);
        List<Object> obj = new ArrayList<>();
        obj.add(1);
        obj.add("1");
        correctStoreable = new MyStoreable(obj, sig);
        obj.add(true);
        test = new MyTable(tableName, testDir, sig);
        ((MyTable) test).myTableProvider = new MyTableProvider(testDir);
    }

    @Test
    public final void testGetReturnsNullIfKeyIsNotFound() throws Exception {
        assertNull(test.get(testKey1));
    }

    @Test(expected = IllegalArgumentException.class)
    public final void testGetThrowsIllegalArgumentExceptionCalledForNullKey() throws Exception {
        test.get(null);
    }

    @Test
    public final void testGetCalledForNonComittedKey() throws Exception {
        assertNull(test.put(testKey1, correctStoreable));
        assertEquals(correctStoreable, test.get(testKey1));
    }

    @Test(expected = IllegalArgumentException.class)
    public final void testGetCalledForNonComittedKeyNull() throws Exception {
        assertNull(test.put(testKey1, null));
        assertEquals(null, test.get(testKey1));
    }

    @Test
    public final void testGetCalledForDeletedKeyBeforeCommit() throws Exception {
        assertNull(test.put(testKey1, correctStoreable));
        assertEquals(correctStoreable, test.remove(testKey1));
        assertNull(test.get(testKey1));
    }

    @Test(expected = IllegalArgumentException.class)
    public final void testPutThrowsIllegalArgumentExceptionCalledForNullKey() throws Exception {
        test.put(null, correctStoreable);
    }

    @Test(expected = IllegalArgumentException.class)
    public final void testPutThrowsExceptionCalledForNullValue() throws Exception {
        test.put(testKey1, null);
    }

    @Test
    public final void testPutReturnsNullIfKeyHasNotBeenWrittenYet() throws Exception {
        assertNull(test.put(testKey1, correctStoreable));
    }

    @Test
    public final void testPutReturnsOldValueIfKeyExists() throws Exception {
        test.put(testKey1, correctStoreable);
        assertEquals(correctStoreable, test.put(testKey1, correctStoreable));
    }

    @Test(expected = IllegalArgumentException.class)
    public final void testRemoveThrowsExceptionCalledForNullKey() throws Exception {
        test.remove(null);
    }

    @Test
    public final void testRemoveReturnsNullIfKeyIsNotFound() throws Exception {
        assertNull(test.remove(testKey1));
    }

    @Test
    public final void testRemoveCalledForDeletedKeyBeforeCommit() throws Exception {
        assertNull(test.put(testKey1, correctStoreable));
        assertEquals(correctStoreable, test.remove(testKey1));
        assertNull(test.remove(testKey1));
    }

    @Test
    public final void testRemoveCalledForDeletedKeyAfterCommit() throws Exception {
        assertNull(test.put(testKey1, correctStoreable));
        assertEquals(correctStoreable, test.remove(testKey1));
        test.commit();
        assertNull(test.remove(testKey1));
    }

    @Test
    public final void testCommitCreatesRealFileOnTheDisk()
            throws Exception {
        assertNull(test.put(testKey1, correctStoreable));
        test.commit();
        String subdirectoryName = Math.abs(testKey1.getBytes("UTF-8")[0]
                % 16) + ".dir";
        String fileName = Math.abs((testKey1.getBytes("UTF-8")[0]
                / 16) % 16)
                + ".dat";
        Path filePath = Paths.get(testDir,
                test.getName(),
                subdirectoryName, fileName);
        assertTrue(filePath.toFile().exists());
    }

    @Test
    public final void testCommitReturnsNonZeroChangesPuttingNewOStoreable() throws Exception {
        assertNull(test.put(testKey1, correctStoreable));
        assertEquals(1, test.commit());
    }

    @Test
    public final void testCommitReturnsNotZeroChangesRewriting() throws Exception {
        assertNull(test.put(testKey1, correctStoreable));
        assertEquals(correctStoreable, test.put(testKey1, correctStoreable));
        assertEquals(1, test.commit());
    }

    @Test
    public final void testCommitNoChanges() throws Exception {
        assertNull(test.put(testKey1, correctStoreable));
        assertEquals(1, test.commit());
        assertEquals(0, test.commit());
    }

    //RollbackTests.
    @Test
    public final void testRollbackAfterPuttingNewKey() throws Exception {
        assertEquals(0, test.size());
        assertNull(test.put(testKey1, correctStoreable));
        assertEquals(1, test.size());
        assertEquals(1, test.rollback());
        assertEquals(0, test.size());
        assertNull(test.get(testKey1));
    }

    @Test
    public final void testRollbackNoChanges() throws Exception {
        assertNull(test.put(testKey1, correctStoreable));
        test.rollback();
        assertEquals(0, test.size());
        assertEquals(0, test.rollback());
    }

    @Test
    public final void testListCalledForEmptyTable() throws Exception {
        assertTrue(test.list().isEmpty());
    }

    @Test
    public final void testListCalledForNonEmptyNewTable() throws Exception {
        assertNull(test.put(testKey1, correctStoreable));
        assertNull(test.put(testKey2, correctStoreable));
        Set<String> expectedKeySet = new HashSet<>();
        expectedKeySet.add(testKey1);
        expectedKeySet.add(testKey2);
        Set<String> tableKeySet = new HashSet<>();
        tableKeySet.addAll(test.list());
        assertEquals(expectedKeySet, tableKeySet);
    }

    @Test
    public final void testListCalledForNonEmptyCommitedTable() throws Exception {
        assertNull(test.put(testKey1, correctStoreable));
        assertNull(test.put(testKey2, correctStoreable));
        test.commit();
        assertEquals(correctStoreable, test.remove(testKey2));
        Set<String> expectedKeySet = new HashSet<>();
        expectedKeySet.add(testKey1);
        Set<String> tableKeySet = new HashSet<>();
        tableKeySet.addAll(test.list());
        assertEquals(expectedKeySet, tableKeySet);
    }

    //Size tests.
    @Test
    public final void testGetReturnsLatestValueForUncommittedKey() throws Exception {
        assertNull(test.put(testKey1, correctStoreable));
        assertNull(test.put(testKey2, correctStoreable));
        assertEquals(correctStoreable, test.remove(testKey2));
        assertEquals(1, test.size());
    }

    @Test
    public final void testGetUncommittedKey() throws Exception {
        assertNull(test.put(testKey1, correctStoreable));
        assertNull(test.put(testKey2, correctStoreable));
        assertEquals(2, test.getNumberOfUncommittedChanges());
    }

    @Test
    public final void testSizeCalledForNonEmptyCommitedTable() throws Exception {
        assertNull(test.put(testKey1, correctStoreable));
        assertNull(test.put(testKey2, correctStoreable));
        test.commit();
        assertEquals(correctStoreable, test.remove(testKey2));
        assertEquals(1, test.size());
    }

    @After
    public void tearDown() {
        File dir = new File(testDir);
        for (File currentTableDirectory : dir.listFiles()) {
            if (currentTableDirectory.isDirectory()) {
                for (File tableSubDirectory
                        :currentTableDirectory.listFiles()) {
                    if (tableSubDirectory.isDirectory()) {
                        for (File tableFile: tableSubDirectory.listFiles()) {
                            tableFile.delete();
                        }
                    }
                    tableSubDirectory.delete();
                }
            }
            currentTableDirectory.delete();
        }
        dir.delete();
    }
}
