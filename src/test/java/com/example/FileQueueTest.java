package com.example;

import com.amazonaws.services.sqs.model.Message;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class FileQueueTest extends AbstractTest {

    private final int MAX_TIMEOUT_IN_SECONDS = 20;
    private FileQueueService fileQueue;

    @Before
    public void setUp() throws Exception {
        //Remove file before test
        File file = new File("/tmp/messages2");
        file.delete();
        fileQueue = new FileQueueService(Executors.newScheduledThreadPool(100), "/tmp/messages2", 50);
    }

    @Test
    public void should_push_to_queue() throws IOException {
        String msg = "push test message";
        fileQueue.push(msg);
        fileQueue.push(msg);

        Integer totalLines = Files.readAllLines(Paths.get(fileQueue.getMessages().getPath())).size();
        assertEquals(2L, totalLines.longValue());
    }

    @Test
    public void should_pull_of_queue() throws IOException {
        String msg = "PULL MSG";

        fileQueue.push(msg);
        fileQueue.push(msg);

        Optional<Message> message = fileQueue.pull();
        assertEquals(msg, message.get().getBody());

        Integer totalLines = Files.readAllLines(Paths.get(fileQueue.getMessages().getPath())).size();
        assertEquals(2, totalLines.longValue());
    }


    @Test
    public void should_push_with_concurrency() throws IOException, InterruptedException {
        int times = 20;

        ArrayList<Runnable> executions = new ArrayList<>();

        IntStream.range(0, times).forEach(i -> {
            executions.add(() -> fileQueue.push("Message :" + i));
        });

        assertConcurrent(executions, MAX_TIMEOUT_IN_SECONDS);

        Integer totalLines = Files.readAllLines(Paths.get(fileQueue.getMessages().getPath())).size();
        Assert.assertEquals(times, totalLines.longValue());
    }


}
