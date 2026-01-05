package com.bbn.takml_server;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtil {
    public static byte[] zipDirectoryToByteArray(File folder) throws IOException {
        if (!folder.isDirectory()) {
            throw new IllegalArgumentException("Input must be a directory.");
        }

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(byteStream)) {
            // Include the folder name in the zip
            zipFolderHelper(folder, folder.getParentFile().getAbsolutePath(), zipOut);
        }
        return byteStream.toByteArray();
    }

    private static void zipFolderHelper(File currentFile, String basePath, ZipOutputStream zipOut) throws IOException {
        if (currentFile.isHidden()) return;

        String relativePath = currentFile.getAbsolutePath()
                .substring(basePath.length() + 1)
                .replace("\\", "/");

        if (currentFile.isDirectory()) {
            if (!relativePath.endsWith("/")) {
                relativePath += "/";
            }
            ZipEntry dirEntry = new ZipEntry(relativePath);
            dirEntry.setTime(0L);
            zipOut.putNextEntry(dirEntry);
            zipOut.closeEntry();

            File[] children = currentFile.listFiles();
            if (children != null) {
                Arrays.sort(children, Comparator.comparing(File::getName));
                for (File child : children) {
                    zipFolderHelper(child, basePath, zipOut);
                }
            }
        } else {
            try (FileInputStream fis = new FileInputStream(currentFile)) {
                ZipEntry zipEntry = new ZipEntry(relativePath);
                zipEntry.setTime(0L);
                zipOut.putNextEntry(zipEntry);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) >= 0) {
                    zipOut.write(buffer, 0, length);
                }
                zipOut.closeEntry();
            }
        }
    }
}
