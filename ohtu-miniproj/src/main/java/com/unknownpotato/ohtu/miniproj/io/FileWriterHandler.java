package com.unknownpotato.ohtu.miniproj.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Handler class for writing to files.
 */
public class FileWriterHandler {

    /**
     * File being written to.
     */
    public File file;

    /**
     * Constructor that creates the file that we want to write to.
     *
     * @param file
     * @throws IOException
     */
    public FileWriterHandler(File file) throws IOException {
        this.file = file;
        this.file.delete();
        this.file.createNewFile();
    }

    /**
     * Constructor with String as parameter
     *
     * @param file filename as string
     * @throws IOException
     */
    public FileWriterHandler(String file) throws IOException {
        this(new File(file));
    }

    /**
     * Writes a single String overwriting anything in the file.
     *
     * @param toWrite String to be written
     * @throws IOException
     */
    public void writeTo(String toWrite) throws IOException {
        try (FileWriter writer = new FileWriter(file, false)) {
            writeTo(toWrite, writer);
        }
    }
    
    /**
     * Writes the given string with the given FileWriter.
     * 
     * @param toWrite string written
     * @param writer FileWriter that does the writing
     * @throws IOException 
     */
    private void writeTo(String toWrite, FileWriter writer) throws IOException {
        writer.write(toWrite);
        writer.write('\n');
    }

    /**
     * Writes all Strings given in a List to the file
     *
     * @param toWrite all Strings to be written
     * @throws IOException
     */
    public void writeTo(List<String> toWrite) throws IOException {
        try (FileWriter writer = new FileWriter(file, true)) {
            for (String line : toWrite) {
                writeTo(line, writer);
            }
        }
    }

}
