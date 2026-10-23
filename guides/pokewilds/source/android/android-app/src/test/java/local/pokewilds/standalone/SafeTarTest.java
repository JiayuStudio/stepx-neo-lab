package local.pokewilds.standalone;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.file.*;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.archivers.tar.*;

public class SafeTarTest {
    private byte[] archive(String name, String link, byte type) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(new GZIPOutputStream(bytes))) {
            TarArchiveEntry e = new TarArchiveEntry(name, type, true);
            if (link != null) e.setLinkName(link);
            else { e.setSize(4); e.setMode(0755); }
            tar.putArchiveEntry(e); if (link == null) tar.write(new byte[]{1,2,3,4}); tar.closeArchiveEntry();
        }
        return bytes.toByteArray();
    }
    @Test public void extractsExecutable() throws Exception {
        Path dir=Files.createTempDirectory("payload-test");
        try { SafeTar.extract(new ByteArrayInputStream(archive("rootfs/bin/tool",null,TarConstants.LF_NORMAL)),dir,10,10); assertTrue(Files.isExecutable(dir.resolve("rootfs/bin/tool"))); }
        finally { SafeTar.deleteTree(dir); }
    }
    @Test public void rejectsTraversalAndOversize() throws Exception {
        Path dir=Files.createTempDirectory("payload-test");
        try {
            for (String path:new String[]{"../escape","/absolute","rootfs/../../escape"}) {
                try { SafeTar.extract(new ByteArrayInputStream(archive(path,null,TarConstants.LF_NORMAL)),dir,10,10); fail(path); } catch(IOException expected) { }
            }
            try { SafeTar.extract(new ByteArrayInputStream(archive("big",null,TarConstants.LF_NORMAL)),dir,3,10); fail("size limit"); } catch(IOException expected) { }
        } finally { SafeTar.deleteTree(dir); }
    }
    @Test public void guestAbsoluteSymlinkStaysInsideRootfs() throws Exception {
        Path dir=Files.createTempDirectory("payload-test");
        try { SafeTar.extract(new ByteArrayInputStream(archive("rootfs/lib","/usr/lib",TarConstants.LF_SYMLINK)),dir,10,10); assertEquals(Paths.get("usr/lib"),Files.readSymbolicLink(dir.resolve("rootfs/lib"))); }
        finally { SafeTar.deleteTree(dir); }
    }
    @Test public void materializesHardLinksWithinBudget() throws Exception {
        Path dir=Files.createTempDirectory("payload-test");
        try {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try (TarArchiveOutputStream tar=new TarArchiveOutputStream(new GZIPOutputStream(bytes))) {
                TarArchiveEntry file=new TarArchiveEntry("rootfs/bin/tool"); file.setSize(4); file.setMode(0755); tar.putArchiveEntry(file); tar.write(new byte[]{1,2,3,4}); tar.closeArchiveEntry();
                TarArchiveEntry link=new TarArchiveEntry("rootfs/bin/alias",TarConstants.LF_LINK); link.setLinkName("rootfs/bin/tool"); tar.putArchiveEntry(link); tar.closeArchiveEntry();
            }
            SafeTar.extract(new ByteArrayInputStream(bytes.toByteArray()),dir,8,10);
            assertArrayEquals(Files.readAllBytes(dir.resolve("rootfs/bin/tool")),Files.readAllBytes(dir.resolve("rootfs/bin/alias")));
            assertTrue(Files.isExecutable(dir.resolve("rootfs/bin/alias")));
            assertFalse(Files.isSameFile(dir.resolve("rootfs/bin/tool"),dir.resolve("rootfs/bin/alias")));
        } finally { SafeTar.deleteTree(dir); }
    }
    @Test public void preservesLinkToParentDirectory() throws Exception {
        Path dir=Files.createTempDirectory("payload-test");
        try { SafeTar.extract(new ByteArrayInputStream(archive("rootfs/usr/bin/X11",".",TarConstants.LF_SYMLINK)),dir,10,10); assertEquals(Paths.get("."),Files.readSymbolicLink(dir.resolve("rootfs/usr/bin/X11"))); }
        finally { SafeTar.deleteTree(dir); }
    }
    @Test public void removesProtectedPlaceholdersWithoutFollowingLinks() throws Exception {
        Path root=Files.createTempDirectory("payload-cleanup-test");
        Path runtime=root.resolve("runtime"), placeholder=runtime.resolve("game");
        try {
            Files.createDirectories(placeholder);
            Path outside=root.resolve("preserve"); Files.createDirectories(outside);
            Files.write(outside.resolve("save"),new byte[]{42});
            Files.createSymbolicLink(runtime.resolve("outside"),outside);
            Files.setPosixFilePermissions(placeholder,java.nio.file.attribute.PosixFilePermissions.fromString("---------"));
            SafeTar.deleteTree(runtime);
            assertFalse(Files.exists(runtime));
            assertArrayEquals(new byte[]{42},Files.readAllBytes(outside.resolve("save")));
        } finally {
            if (Files.exists(placeholder)) Files.setPosixFilePermissions(placeholder,java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
            SafeTar.deleteTree(root);
        }
    }
    @Test public void rejectsEscapingSymlink() throws Exception {
        Path dir=Files.createTempDirectory("payload-test");
        try { try { SafeTar.extract(new ByteArrayInputStream(archive("rootfs/link","../../escape",TarConstants.LF_SYMLINK)),dir,10,10); fail("symlink escape"); } catch(IOException expected) { } }
        finally { SafeTar.deleteTree(dir); }
    }
}
