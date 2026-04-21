package com.tencent.supersonic.common.outbox;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class OutboxTestListener {

    public final ConcurrentLinkedQueue<String> received = new ConcurrentLinkedQueue<>();

    @EventListener
    public void on(OutboxPublisherTest.ToyEvent e) {
        received.add(e.payload);
    }
}
