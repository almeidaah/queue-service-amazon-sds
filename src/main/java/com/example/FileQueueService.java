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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;


public class FileQueueService implements QueueService {

    public File messages;
    public File tempMessages;
    private ScheduledExecutorService executorService;

    private File qFileLock;

    private ConcurrentMap<String, ScheduledFuture> temporaryMessages;


    public FileQueueService(ScheduledExecutorService executorService, String filePath, Integer queueSize) throws IOException, InterruptedException {

        this.executorService = executorService;

        //Temporary Queue Size
        temporaryMessages = new ConcurrentHashMap<>(queueSize);

        this.messages = new File(filePath);
        this.tempMessages = new File(filePath + "Temp");

        if (!Files.exists(Paths.get(messages.getPath()))) {
            this.messages.createNewFile();
        }

        if (!Files.exists(Paths.get(tempMessages.getPath()))) {
            this.tempMessages.createNewFile();
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

//            Runnable runnable = () -> {
//                pushToTemp(lastLineStr);
//            };
//            ScheduledFuture future = executorService.schedule(runnable, (long) this.DEFAULT_VISIBILITY_TIMEOUT, TimeUnit.SECONDS);

            return Optional.of(msg);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            unlock(this.getLockFile());
        }
        return Optional.empty();
    }

    private void pushToTemp(String lastLineStr) {
        try (FileWriter fw = new FileWriter(tempMessages, true)) {
            fw.write(lastLineStr);
            fw.write(System.lineSeparator());
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            unlock(this.getLockFile());
        }
    }

    @Override
    public void delete(String receiptHandle) {
        //Should Delete From Temp
        //try (RandomAccessFile messageRandomFile = new RandomAccessFile(messages, "rw")) {

        try (RandomAccessFile messageRandomFile = new RandomAccessFile(messages, "rw")) {
            this.lock(this.getLockFile());

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
