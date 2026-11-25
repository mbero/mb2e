package io.github.mbero.mb2e.infrastructure.config;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * An exclusive lock on {@code <workspace>/.mb2e.lock}, held while the process runs.
 * <p>A second process, or a second holder in the same JVM, that uses the same workspace is refused,
 * because two services must never run git in the same clones.
 */
public final class WorkspaceLock implements AutoCloseable {

    static final String FILE_NAME = ".mb2e.lock";

    private final FileChannel channel;
    private final FileLock lock;

    private WorkspaceLock(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    /**
     * Creates the workspace directory if needed and takes the lock.
     *
     * @throws IllegalStateException when the lock is held elsewhere or cannot be taken; the message names the lock file
     */
    public static WorkspaceLock acquire(Path workspace) {
        Path file = workspace.resolve(FILE_NAME);
        FileChannel channel = null;
        try {
            Files.createDirectories(workspace);
            channel = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            FileLock lock = channel.tryLock();
            if (lock == null) {
                channel.close();
                throw new IllegalStateException("Another mb2e process holds the workspace lock " + file);
            }
            return new WorkspaceLock(channel, lock);
        } catch (OverlappingFileLockException held) {
            closeQuietly(channel);
            throw new IllegalStateException("Another mb2e instance holds the workspace lock " + file);
        } catch (IOException failure) {
            closeQuietly(channel);
            throw new IllegalStateException("The workspace lock " + file + " cannot be taken: " + failure.getMessage(),
                    failure);
        }
    }

    private static void closeQuietly(FileChannel channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException ignored) {
                // The lock attempt already failed; nothing more to release.
            }
        }
    }

    @Override
    public void close() {
        try {
            lock.release();
            channel.close();
        } catch (IOException failure) {
            throw new IllegalStateException("The workspace lock cannot be released: " + failure.getMessage(), failure);
        }
    }
}
