package com.example;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;


public class InMemoryQueueTest extends AbstractTest {

    private final int MAX_TIMEOUT_IN_SECONDS = 20;
    private InMemoryQueueService queue;
    private ScheduledExecutorService scheduledExecutorService;

    @Before
    public void setUp() throws Exception {
        this.scheduledExecutorService = mock(ScheduledExecutorService.class);
        this.queue = new InMemoryQueueService(scheduledExecutorService, 5000, 5000L);
    }

    @Test
    public void should_add_to_queue() throws InterruptedException {

        ArrayList<Runnable> executions = new ArrayList<>();

        for (Integer i = 0; i < 10; i++) {
            executions.add(() -> queue.push("Message :"));
        }

        assertConcurrent(executions, MAX_TIMEOUT_IN_SECONDS);
        verify(scheduledExecutorService, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

        Assert.assertEquals(10L, queue.getMessagesSize().longValue());

    }

    @Test
    public void should_poll_queue() throws InterruptedException {
        int executionTimes = 100;
        ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
        doReturn(mockFuture).when(this.scheduledExecutorService).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

        List<String> sentMessages = Collections.synchronizedList(new ArrayList<>());

        IntStream.range(0, executionTimes).forEach(i -> {
            String message = "test message body " + i;
            // Save the sent message list unto an array
            sentMessages.add(message);
            queue.push(message);
        });

        List<Runnable> runners = new ArrayList<>();

        IntStream.range(0, executionTimes).forEach(i -> {
            runners.add(() -> {
                String messageBody = queue.pull().get().getBody();
                assertTrue(sentMessages.remove(messageBody));
            });
        });

        // Attempt polling element in the queue simultaneously
        assertConcurrent(runners, MAX_TIMEOUT_IN_SECONDS);

        // Verify the executor being called and upon poll we set it to delete the message within a certain timeout
        verify(scheduledExecutorService, times(executionTimes)).schedule(isA(Runnable.class), eq(5000L), eq(TimeUnit.MILLISECONDS));

        assertEquals(0L, queue.getMessagesSize().longValue());
        assertEquals(0, sentMessages.size());
        assertEquals(executionTimes, queue.getTemporaryMessagesSize().longValue());
    }

}
