package org.jetlang.core;

import junit.framework.Assert;
import org.jetlang.channels.Channel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class FiberBaseTest extends Assert {
    public abstract ProcessFiber CreateBus();

    public abstract void DoSetup();

    public abstract void DoTearDown();

    protected ProcessFiber _bus;

    @Before
    public void Setup() {
        DoSetup();
        _bus = CreateBus();
    }

    @After
    public void TearDown() {
        if (_bus != null) {
            _bus.stop();
        }
        DoTearDown();
    }

    @Test
    public void ScheduleBeforeStart() throws InterruptedException {
        final CountDownLatch reset = new CountDownLatch(1);

        Runnable onReset = new Runnable() {
            public void run() {
                reset.countDown();
            }
        };
        _bus.schedule(onReset, 1);
        _bus.start();

        assertTrue(reset.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void ScheduleAndCancelBeforeStart() throws InterruptedException {
        final CountDownLatch reset = new CountDownLatch(1);
        final boolean[] executed = new boolean[1];
        Runnable toCancel = new Runnable() {
            public void run() {
                executed[0] = true;
            }
        };
        TimerControl control = _bus.schedule(toCancel, 0);
        Runnable toRun = new Runnable() {
            public void run() {
                reset.countDown();
            }
        };
        _bus.schedule(toRun, 0);
        control.cancel();
        _bus.start();
        assertTrue(reset.await(10, TimeUnit.SECONDS));
        assertFalse(executed[0]);
    }


    @Test
    public void ScheduleOne() throws InterruptedException {
        final CountDownLatch reset = new CountDownLatch(1);
        _bus.start();
        Runnable onReset = new Runnable() {
            public void run() {
                reset.countDown();
            }
        };
        _bus.schedule(onReset, 1);
        assertTrue(reset.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void ScheduleInterval() throws InterruptedException {
        final CountDownLatch reset = new CountDownLatch(5);
        _bus.start();
        Runnable onReset = new Runnable() {
            public void run() {
                reset.countDown();
            }
        };
        _bus.scheduleOnInterval(onReset, 15, 15);
        assertTrue(reset.await(10, TimeUnit.SECONDS));
    }


    @Test
    public void testDoubleStartResultsInException() {
        _bus.start();
        try {
            _bus.start();
            Assert.fail("Should not start");
        }
        catch (Exception e) {
        }
    }

    @Test
    public void PubSub() throws InterruptedException {
        _bus.start();
        Channel<String> channel = new Channel<String>();
        Assert.assertFalse(channel.publish("hello"));
        final List<String> received = new ArrayList<String>();
        final CountDownLatch reset = new CountDownLatch(1);
        Callback<String> onReceive = new Callback<String>() {
            public void onMessage(String data) {
                received.add(data);
                reset.countDown();
            }
        };
        channel.subscribe(_bus, onReceive);
        assertEquals(1, channel.subscriberCount());
        assertTrue(channel.publish("hello"));
        assertTrue(reset.await(10, TimeUnit.SECONDS));
        assertEquals(1, received.size());
        assertEquals("hello", received.get(0));

        channel.clearSubscribers();
        assertFalse(channel.publish("hello"));
    }

    @Test
    public void UnsubOnStop() throws InterruptedException {
        _bus.start();
        Channel<String> channel = new Channel<String>();
        Callback<String> onReceive = new Callback<String>() {
            public void onMessage(String data) {
            }
        };
        channel.subscribe(_bus, onReceive);
        assertEquals(1, channel.subscriberCount());
        _bus.stop();
        assertEquals(0, channel.subscriberCount());
    }

    @Test
    public void Unsub() throws InterruptedException {
        _bus.start();
        Channel<String> channel = new Channel<String>();
        Callback<String> onReceive = new Callback<String>() {
            public void onMessage(String data) {
            }
        };
        Unsubscriber unsub = channel.subscribe(_bus, onReceive);
        assertEquals(1, channel.subscriberCount());
        unsub.unsubscribe();
        assertEquals(0, channel.subscriberCount());
    }

//        @Test
//        public void AsyncRequestTimeout()
//        {
//            CountDownLatch reset = new CountDownLatch(1);
//            Command onTimeout = delegate { reset.Set(); };
//            _bus.start();
//            OnMessage<string> reply = delegate { Assert.Fail("Should not be called"); };
//            _bus.SendAsyncRequest(new object(), "msg", reply, onTimeout, 1);
//            Assert.IsTrue(reset.WaitOne(5000, false));
//        }

//        [Test]
//        public void AsyncRequestWithReply()
//        {
//            IProcessBus replyBus = CreateBus(_contextFactory);
//            replyBus.start();
//            string requestTopic = "request";
//            OnMessage<string> onMsg =
//                delegate(IMessageHeader header, string msg) { replyBus.Publish(header.ReplyTo, msg); };
//            replyBus.Subscribe(new TopicEquals(requestTopic), onMsg);
//            Command onTimeout = delegate { Assert.Fail("Should not timeout"); };
//            _bus.start();
//            ManualResetEvent reset = new ManualResetEvent(false);
//            OnMessage<string> reply = delegate { reset.Set(); };
//            _bus.SendAsyncRequest("request", "msg", reply, onTimeout, 100);
//            Assert.IsTrue(reset.WaitOne(5000, false));
//            replyBus.Stop();
//        }

}