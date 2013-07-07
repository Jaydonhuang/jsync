package de.tntinteractive.jsync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Bekommt vom {@link Generator} die Befehle zum Verschicken von Dateien und schickt das passende Diff
 * an den {@link Receiver}.
 */
public class Sender implements Runnable {

    private final DataInputStream source;
    private final FilePathBuffer filePaths;
    private final ReceiverCommandWriter writer;

    public Sender(InputStream source, FilePathBuffer filePaths, OutputStream target) {
        this.source = new DataInputStream(source);
        this.filePaths = filePaths;
        this.writer = new ReceiverCommandWriter(new DataOutputStream(target));
    }

    @Override
    public void run() {
        try {
            int index = -1;
            while (!Thread.interrupted()) {
                final int command = this.source.read();
                if (command < 0) {
                    return;
                }
                if (command == SenderCommand.FILE_START.getCode()) {
                    index = this.source.readInt();
                } else if (command == SenderCommand.HASH.getCode()) {
                    throw new RuntimeException();
                } else if (command == SenderCommand.FILE_END.getCode()) {
                    this.doFileHandling(index);
                } else {
                    throw new IOException("unknown command " + command);
                }
            }
        } catch (final IOException e) {
            //TODO handle
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            this.writer.close();
        }
    }

    private void doFileHandling(int index) throws IOException {
        final FilePath file = this.filePaths.get(index);
        final InputStream fileStream = file.openInputStream();
        try {
            final MD4StreamFilter md4stream = new MD4StreamFilter(fileStream);
            this.writer.writeFileStart(index);
            long remainingBytes = file.getSize();
            while (remainingBytes > 0) {
                final long inThisChunk = Math.min(remainingBytes, Integer.MAX_VALUE);
                this.writer.writeRawData((int) inThisChunk, md4stream);
                remainingBytes -= inThisChunk;
            }
            this.writer.writeFileEnd(md4stream.getDigest());
        } finally {
            fileStream.close();
        }
    }

}
