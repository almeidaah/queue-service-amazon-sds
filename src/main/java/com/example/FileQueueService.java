package com.example;

import com.amazonaws.services.sqs.model.Message;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;


public class FileQueueService implements QueueService {

    //
    // Task 3: Implement me if you have time.
    //

    public File messages;
    private File qFileLock;

    private final Integer QUEUE_SIZE;

    public FileQueueService(String filePath, Integer queueSize) throws IOException, InterruptedException {
        this.QUEUE_SIZE = queueSize;

        this.messages = new File(filePath);

        if (!Files.exists(Paths.get(messages.getPath()))) {
            this.messages.createNewFile();
        }

        this.qFileLock = new File("/tmp/qFileLock");

    }

    private void lock(File lock) throws InterruptedException, IOException {
        while (!this.getLockFile().createNewFile()) {
            Thread.sleep(50);
        }
    }

    private void unlock(File lock) {
        lock.delete();
    }

    private File getLockFile() {
        return this.qFileLock;
    }

    @Override
    public void push(String message) {
        try {
            this.lock(this.getLockFile());

            FileWriter fw = new FileWriter(messages, true);
            fw.write(message);
            fw.write(System.lineSeparator());
            fw.flush();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            unlock(this.getLockFile());
        }
    }

    @Override
    public Optional<Message> pull() {
        try {
            this.lock(this.getLockFile());

            List<String> allLines = Files.readAllLines(Paths.get(messages.getPath()));
            String lastLineStr = allLines.stream().skip(allLines.size() - 1).findFirst().get();

            Message msg = new Message().withBody(lastLineStr);

            return Optional.of(msg);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            unlock(this.getLockFile());
        }
        return Optional.empty();
    }

    @Override
    public void delete(String receiptHandle) {
        try {
            this.lock(this.getLockFile());

            RandomAccessFile messageRandomFile = new RandomAccessFile(messages, "rw");

            //remove last line
            long length = messageRandomFile.length() - 1;
            byte actualByte;
            do {
                length -= 1;
                messageRandomFile.seek(length);
                actualByte = messageRandomFile.readByte();
            } while (actualByte != 10);

            //truncate after find linefeed[b=10]
            messageRandomFile.setLength(length + 1);
            messageRandomFile.close();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            unlock(this.getLockFile());
        }

    }

    public File getMessages() {
        return messages;
    }
}
