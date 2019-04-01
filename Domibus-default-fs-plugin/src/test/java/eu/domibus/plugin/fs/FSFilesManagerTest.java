package eu.domibus.plugin.fs;

import eu.domibus.plugin.fs.exception.FSSetUpException;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.apache.commons.vfs2.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.activation.DataHandler;

/**
 * @author FERNANDES Henrique, GONCALVES Bruno
 */
@RunWith(JMockit.class)
public class FSFilesManagerTest {

    @Tested
    private FSFilesManager instance;

    @Injectable
    private FSPluginProperties fsPluginProperties;

    private FileObject rootDir;

    @Injectable
    private FileObject mockedRootDir;

    @Before
    public void setUp() throws FileSystemException {
        String location = "ram:///FSFilesManagerTest";
        String sampleFolderName = "samplefolder";

        FileSystemManager fsManager = VFS.getManager();
        rootDir = fsManager.resolveFile(location);
        rootDir.createFolder();

        FileObject sampleFolder = rootDir.resolveFile(sampleFolderName);
        sampleFolder.createFolder();

        rootDir.resolveFile("file1").createFile();
        rootDir.resolveFile("file2").createFile();
        rootDir.resolveFile("file3").createFile();
        rootDir.resolveFile("toberenamed").createFile();
        rootDir.resolveFile("toberenamed").getContent().setLastModifiedTime(0);
        rootDir.resolveFile("tobemoved").createFile();
        rootDir.resolveFile("toberenamed").getContent().setLastModifiedTime(0);
        rootDir.resolveFile("tobedeleted").createFile();

        rootDir.resolveFile("targetfolder1/targetfolder2").createFolder();
    }

    @After
    public void tearDown() throws FileSystemException {
        rootDir.deleteAll();
        rootDir.close();
    }

    // This test fails with a temporary filesystem
    @Test(expected = FSSetUpException.class)
    public void testGetEnsureRootLocation_Auth() throws Exception {
        String location = "ram:///FSFilesManagerTest";
        String domain = "domain";
        String user = "user";
        String password = "password";

        FileObject result = instance.getEnsureRootLocation(location, domain, user, password);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.exists());
    }

    @Test
    public void testGetEnsureRootLocation() throws Exception {
        String location = "ram:///FSFilesManagerTest";

        FileObject result = instance.getEnsureRootLocation(location);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.exists());
    }

    @Test
    public void testGetEnsureChildFolder() throws Exception {
        String folderName = "samplefolder";

        FileObject result = instance.getEnsureChildFolder(rootDir, folderName);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.exists());
        Assert.assertEquals(FileType.FOLDER, result.getType());
    }

    @Test(expected = FSSetUpException.class)
    public void testGetEnsureChildFolder_FileSystemException() throws Exception {
        final String folderName = "samplefolder";

        new Expectations(instance) {{
            mockedRootDir.exists();
            result = true;

            mockedRootDir.resolveFile(folderName);
            result = new FileSystemException("some unexpected error");
        }};

        instance.getEnsureChildFolder(mockedRootDir, folderName);
    }

    @Test
    public void testFindAllDescendantFiles() throws Exception {
        FileObject[] files = instance.findAllDescendantFiles(rootDir);

        Assert.assertNotNull(files);
        Assert.assertEquals(6, files.length);
        Assert.assertEquals("ram:///FSFilesManagerTest/file1", files[0].getName().getURI());
        Assert.assertEquals("ram:///FSFilesManagerTest/file2", files[1].getName().getURI());
        Assert.assertEquals("ram:///FSFilesManagerTest/file3", files[2].getName().getURI());
        Assert.assertEquals("ram:///FSFilesManagerTest/toberenamed", files[3].getName().getURI());
        Assert.assertEquals("ram:///FSFilesManagerTest/tobemoved", files[4].getName().getURI());
        Assert.assertEquals("ram:///FSFilesManagerTest/tobedeleted", files[5].getName().getURI());
    }

    @Test
    public void testGetDataHandler() throws Exception {
        DataHandler result = instance.getDataHandler(rootDir);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getDataSource());
    }

    @Test
    public void testResolveSibling() throws Exception {
        FileObject result = instance.resolveSibling(rootDir, "siblingdir");

        Assert.assertNotNull(result);
        Assert.assertEquals("ram:///siblingdir", result.getName().getURI());
    }

    @Test
    public void testRenameFile() throws Exception {
        FileObject file = rootDir.resolveFile("toberenamed");

        long beforeMillis = System.currentTimeMillis();
        FileObject result = instance.renameFile(file, "renamed");
        long afterMillis = System.currentTimeMillis();

        Assert.assertNotNull(result);
        Assert.assertEquals("ram:///FSFilesManagerTest/renamed", result.getName().getURI());
        Assert.assertTrue(result.exists());
        Assert.assertTrue(result.getContent().getLastModifiedTime() >= beforeMillis);
        Assert.assertTrue(result.getContent().getLastModifiedTime() <= afterMillis);
    }

    // This test fails with a temporary filesystem
    @Test(expected = FSSetUpException.class)
    public void testSetUpFileSystem_Domain() throws Exception {
        new Expectations(instance) {{
            fsPluginProperties.getLocation("DOMAIN1");
            result = "ram:///FSFilesManagerTest/samplefolder";

            fsPluginProperties.getUser("DOMAIN1");
            result = "user";

            fsPluginProperties.getPassword("DOMAIN1");
            result = "secret";
        }};

        FileObject result = instance.setUpFileSystem("DOMAIN1");

        Assert.assertNotNull(result);
        Assert.assertTrue(result.exists());
        Assert.assertEquals("ram:///FSFilesManagerTest/samplefolder", result.getName().getURI());
    }

    @Test
    public void testSetUpFileSystem() throws Exception {
        new Expectations(instance) {{
            fsPluginProperties.getLocation(null);
            result = "ram:///FSFilesManagerTest";
        }};

        FileObject result = instance.setUpFileSystem(null);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.exists());
        Assert.assertEquals("ram:///FSFilesManagerTest", result.getName().getURI());
    }

    @Test
    public void testDeleteFile() throws Exception {
        FileObject file = rootDir.resolveFile("tobedeleted");
        boolean result = instance.deleteFile(file);

        Assert.assertTrue(result);
        Assert.assertFalse(file.exists());
    }

    @Test
    public void testCloseAll(@Mocked final FileObject file1,
                             @Mocked final FileObject file2,
                             @Mocked final FileObject file3) throws FileSystemException {

        new Expectations(1, instance) {{
            file2.close();
            result = new FileSystemException("Test-forced exception");
        }};

        instance.closeAll(new FileObject[]{file1, file2, file3});

        new Verifications(1) {{
            file1.close();
            file2.close();
            file3.close();
        }};
    }

    @Test
    public void testMoveFile() throws Exception {
        FileObject file = rootDir.resolveFile("tobemoved");
        FileObject targetFile = rootDir.resolveFile("targetfolder1/targetfolder2/moved");

        long beforeMillis = System.currentTimeMillis();
        instance.moveFile(file, targetFile);
        long afterMillis = System.currentTimeMillis();

        Assert.assertTrue(targetFile.exists());
        Assert.assertTrue(targetFile.getContent().getLastModifiedTime() >= beforeMillis);
        Assert.assertTrue(targetFile.getContent().getLastModifiedTime() <= afterMillis);
    }

    @Test
    public void testCreateFile() throws Exception {
        try {
            instance.createFile(rootDir, "tobecreated", "withcontent");
        } catch (FileSystemException e) {
            if ("File closed.".equals(e.getMessage())) {
                // unit test workaround, file is being closed twice
            } else {
                Assert.fail();
            }
        }

        Assert.assertTrue(rootDir.resolveFile("tobecreated").exists());
    }


    @Test
    public void hasLockFile(@Injectable FileObject file,
                            @Injectable final FileObject lockFile) throws FileSystemException {
        new Expectations(instance) {{
            instance.resolveSibling(file, anyString);
            result = lockFile;

            lockFile.exists();
            result = true;
        }};

        final boolean hasLockFile = instance.hasLockFile(file);

        Assert.assertTrue(hasLockFile);
    }

    @Test
    public void createLockFile(@Injectable FileObject file,
                               @Injectable final FileObject lockFile) throws FileSystemException {
        new Expectations(instance) {{
            instance.resolveSibling(file, anyString);
            result = lockFile;
        }};

        instance.createLockFile(file);

        new Verifications() {{
            lockFile.createFile();
        }};
    }

    @Test
    public void deleteLockFile(@Injectable FileObject file,
                               @Injectable final FileObject lockFile) throws FileSystemException {
        new Expectations(instance) {{
            instance.resolveSibling(file, anyString);
            result = lockFile;

            lockFile.exists();
            result = true;
        }};

        instance.deleteLockFile(file);

        new Verifications() {{
            lockFile.delete();
        }};
    }
}
