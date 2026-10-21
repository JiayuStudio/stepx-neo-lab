package com.amoo.backscreenfun;

import android.content.Context;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Loopback-only, paired, bounded status server. */
public final class StatusServer {
    public static final int PORT = 47321;
    private static StatusServer instance;
    private final Context context;
    private final Set<Socket> clients = Collections.synchronizedSet(new HashSet<Socket>());
    private final ThreadPoolExecutor workers;
    private final SecureHttp protocol;
    private volatile boolean running;
    private ServerSocket listener;
    private long window = System.nanoTime();
    private int accepted;

    private StatusServer(Context context) {
        this.context = context.getApplicationContext();
        this.workers = new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(4), new ThreadFactory() {
                    public Thread newThread(Runnable runnable) {
                        Thread t = new Thread(runnable, "bsf-secure-client");
                        t.setDaemon(true);
                        return t;
                    }
                });
        this.protocol = new SecureHttp(new SecureHttp.Backend() {
            public boolean authorized(String token) { return Pairing.matches(StatusServer.this.context, token); }
            public String status() { return PlaybackService.statusLine(); }
            public String alive() {
                String kind = PlaybackService.kind();
                if (kind == null || !kind.matches("[a-z_]{0,24}")) kind = "other";
                return "ticks=" + PlaybackService.ticks() + " playing=" + PlaybackService.isPlaying() + " kind=" + kind;
            }
            public void update(String tool, String state, String message, boolean show) {
                PlaybackService.pushStatus(StatusServer.this.context, tool, state, message);
                if (show) PlaybackService.showStatus(StatusServer.this.context);
            }
        });
    }

    public static synchronized void start(Context context) {
        if (instance != null && instance.running) return;
        StatusServer next = new StatusServer(context);
        if (next.open()) instance = next;
        else next.close();
    }

    public static synchronized void stop() {
        if (instance != null) instance.close();
        instance = null;
    }

    private boolean open() {
        try {
            Pairing.token(context);
            listener = new ServerSocket();
            listener.setReuseAddress(true);
            listener.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT), 4);
            workers.prestartAllCoreThreads();
            running = true;
            Thread thread = new Thread(new Runnable() { public void run() { acceptLoop(); } }, "bsf-secure-listen");
            thread.setDaemon(true);
            thread.start();
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    private void acceptLoop() {
        while (running) {
            Socket client = null;
            try {
                client = listener.accept();
                long now = System.nanoTime();
                if (now - window >= TimeUnit.SECONDS.toNanos(1)) { window = now; accepted = 0; }
                if (++accepted > 24) { replyAndClose(client, 429); continue; }
                client.setSoTimeout(1200);
                client.setTcpNoDelay(true);
                clients.add(client);
                final Socket active = client;
                final long queuedAt = System.nanoTime();
                workers.execute(new Runnable() {
                    public void run() {
                        try {
                            SecureHttp.Response response;
                            if (System.nanoTime() - queuedAt > TimeUnit.MILLISECONDS.toNanos(1200))
                                response = new SecureHttp.Response(408, "Request Timeout");
                            else response = protocol.handle(new BufferedInputStream(active.getInputStream(), 1024));
                            active.getOutputStream().write(response.bytes());
                            active.getOutputStream().flush();
                        } catch (IOException | RuntimeException e) {
                            // Deliberately no request, endpoint, token or exception logging.
                        } finally { clients.remove(active); closeSocket(active); }
                    }
                });
            } catch (RejectedExecutionException e) {
                if (client != null) { clients.remove(client); replyAndClose(client, 503); }
            } catch (IOException e) {
                if (client != null) { clients.remove(client); closeSocket(client); }
                if (!running) break;
            }
        }
    }

    private static void replyAndClose(Socket socket, int code) {
        try { socket.getOutputStream().write(new SecureHttp.Response(code,
                code == 429 ? "Too Many Requests" : "Service Unavailable").bytes()); }
        catch (IOException e) { }
        finally { closeSocket(socket); }
    }

    private void close() {
        running = false;
        try { if (listener != null) listener.close(); } catch (IOException e) { }
        synchronized (clients) { for (Socket socket : clients) closeSocket(socket); clients.clear(); }
        workers.shutdownNow();
    }

    private static void closeSocket(Socket socket) {
        try { socket.close(); } catch (IOException e) { }
    }
}
