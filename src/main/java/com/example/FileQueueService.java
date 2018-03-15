package com.example;

import com.amazonaws.services.sqs.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

import static java.util.stream.Collectors.toList;

public class FileQueueService implements QueueService {

    private File messages;
    private File tempMessageFile;
    private ScheduledExecutorService executorService;
    private final Long visibilityTimeout;

    private ConcurrentMap<String, ScheduledFuture<Message>> temporarySchedulers = new ConcurrentHashMap<>();

    private File qFileLock;

    final Logger logger = LoggerFactory.getLogger(FileQueueService.class);

    public FileQueueService(ScheduledExecutorService executorService, String filePath, Integer queueSize, Long visibilityTimeout) throws IOException, InterruptedException {
        this.executorService = executorService;
        this.visibilityTimeout = visibilityTimeout;
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
        try (FileWriter fw = new FileWriter(messages, true)) {
            this.lock(this.getLockFile());

            fw.write(message);
            fw.write(System.lineSeparator());
            logger.info("===Message added===");

        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage());
        } finally {
            unlock(this.getLockFile());
        }
    }

    /*
     * Should improve this implementation using .lock by file.
     */
    @Override
    public Optional<Message> pull() {
        try {
            this.lock(this.getLockFile());

            String randomUuid = UUID.randomUUID().toString();

            List<String> lines = Files.lines(Paths.get(messages.getPath())).collect(toList());
            String lastLineStr = lines.get(lines.size() - 1);

            Runnable runnable = () -> {
                this.push(lastLineStr);
                tempMessageFile = new File("/tmp/" + randomUuid);
                tempMessageFile.delete();
                temporarySchedulers.remove(randomUuid);

                logger.info("===Pull Message : " + lastLineStr + "===");
            };
            ScheduledFuture<Message> future = (ScheduledFuture<Message>) executorService.schedule(runnable, visibilityTimeout, TimeUnit.MILLISECONDS);

            removeLastLine(new RandomAccessFile(messages, "rw"));

            pushToTemp(randomUuid, lastLineStr);
            temporarySchedulers.put(randomUuid, future);

            return Optional.of(new Message()
                    .withReceiptHandle(randomUuid)
                    .withBody(lastLineStr));

        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage());
        } finally {
            unlock(this.getLockFile());
        }
        return Optional.empty();
    }

    private void pushToTemp(String randomUuid, String lastLineStr) throws IOException {

        tempMessageFile = new File("/tmp/" + randomUuid);
        tempMessageFile.createNewFile();

        try (FileWriter fw = new FileWriter(tempMessageFile, true)) {
            fw.write(lastLineStr);
            fw.write(System.lineSeparator());
        } catch (IOException e) {
            logger.error(e.getMessage());
        } finally {
            unlock(this.getLockFile());
        }
    }

    @Override
    public void delete(String receiptHandle) {
        ScheduledFuture<Message> sFuture = temporarySchedulers.remove(receiptHandle);
        if (sFuture == null) throw new RuntimeException("Message not exist");

        sFuture.cancel(true);
        logger.info("===Message Deleted : " + receiptHandle + "===");
    }

    private void removeLastLine(RandomAccessFile file) throws IOException {
        long length = file.length() - 1;
        byte actualByte;

        do {
            length -= 1;
            file.seek(length);
            actualByte = file.readByte();
        } while (actualByte != 10);

        //truncate after find linefeed[b=10]
        file.setLength(length + 1);
    }

    public File getMessages() {
        return messages;
    }
}
