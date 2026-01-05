package com.bbn.takml_server;
import org.springframework.test.context.ActiveProfiles;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

@ActiveProfiles("test")
public class BaseTest {
    protected File createRandomFile(final String filename, final long sizeInBytes) throws IOException {
        File tempFile = File.createTempFile(filename, "");
        tempFile.createNewFile();
        RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
        raf.setLength(sizeInBytes);
        raf.close();
        return tempFile;
    }
}
