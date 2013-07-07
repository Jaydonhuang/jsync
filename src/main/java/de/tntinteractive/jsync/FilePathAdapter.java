package de.tntinteractive.jsync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class FilePathAdapter implements FilePath {

    private final File file;

    public FilePathAdapter(File file) {
        this.file = file;
    }

    @Override
    public String getName() {
        return this.file.getName();
    }

    @Override
    public FilePath getParent() {
        final File p = this.file.getParentFile();
        return p == null ? null : new FilePathAdapter(p);
    }

    @Override
    public Iterable<? extends FilePath> getChildrenSorted() {
        final File[] children = this.file.listFiles();
        if (children == null) {
            return Collections.emptyList();
        }
        Arrays.sort(children, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        final ArrayList<FilePath> ret = new ArrayList<FilePath>();
        for (final File f : children) {
            ret.add(new FilePathAdapter(f));
        }
        return ret;
    }

    @Override
    public FilePath getChild(String name) {
        return new FilePathAdapter(this.createSubfile(name));
    }

    private File createSubfile(String name) {
        return new File(this.file, name);
    }

    @Override
    public boolean hasChild(String name) {
        return this.createSubfile(name).exists();
    }

    @Override
    public boolean isDirectory() {
        return this.file.isDirectory();
    }

    @Override
    public long getSize() {
        return this.file.length();
    }

    @Override
    public long getLastChange() {
        return this.file.lastModified();
    }

    @Override
    public FilePath createSubdirectory(String name) throws IOException {
        final File sf = this.createSubfile(name);
        final boolean created = sf.mkdir();
        if (!created && !sf.exists()) {
            throw new IOException("could not create " + sf);
        }
        return new FilePathAdapter(sf);
    }

    @Override
    public void delete() throws IOException {
        for (final FilePath child : this.getChildrenSorted()) {
            child.delete();
        }
        final boolean deleted = this.file.delete();
        if (!deleted) {
            throw new IOException("could not delete " + this.file);
        }
    }

    @Override
    public void renameTo(String newName) throws IOException {
        final boolean success = this.file.renameTo(new File(this.file.getParentFile(), newName));
        if (!success) {
            throw new IOException("could not rename " + this.file + " to " + newName);
        }
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return new FileInputStream(this.file);
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return new FileOutputStream(this.file);
    }

}