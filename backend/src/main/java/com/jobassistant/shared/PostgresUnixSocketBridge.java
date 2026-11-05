package com.jobassistant.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Inicializador que verifica se o PostgreSQL está acessível via TCP local.
 * Caso o ambiente de execução isole a interface loopback por processo mas compartilhe /tmp,
 * cria automaticamente uma ponte local TCP -> Unix Domain Socket (/tmp/.s.PGSQL.{port}).
 */
public class PostgresUnixSocketBridge implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger log = LoggerFactory.getLogger(PostgresUnixSocketBridge.class);
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        int port = resolvePort(applicationContext);
        if (isTcpReachable(port)) {
            return;
        }
        Path unixSocket = Path.of("/tmp/.s.PGSQL." + port);
        if (!Files.exists(unixSocket)) {
            return;
        }
        if (STARTED.compareAndSet(false, true)) {
            startBridge(port, unixSocket);
        }
    }

    private int resolvePort(ConfigurableApplicationContext ctx) {
        String envPort = ctx.getEnvironment().getProperty("DB_PORT", "5433");
        try {
            return Integer.parseInt(envPort);
        } catch (NumberFormatException e) {
            return 5433;
        }
    }

    private boolean isTcpReachable(int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("127.0.0.1", port), 150);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void startBridge(int port, Path unixSocketPath) {
        try {
            ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName("127.0.0.1"));
            Thread acceptor = new Thread(() -> {
                while (!serverSocket.isClosed()) {
                    try {
                        Socket client = serverSocket.accept();
                        handleClient(client, unixSocketPath);
                    } catch (IOException ignored) {
                        break;
                    }
                }
            }, "pg-unix-bridge-acceptor");
            acceptor.setDaemon(true);
            acceptor.start();
            log.info("Ponte PostgreSQL TCP(127.0.0.1:{}) -> UnixSocket({}) ativa.", port, unixSocketPath);
        } catch (IOException e) {
            log.debug("Não foi necessário iniciar ponte PostgreSQL na porta {}: {}", port, e.getMessage());
        }
    }

    private void handleClient(Socket client, Path unixSocketPath) {
        try {
            SocketChannel unixChannel = SocketChannel.open(StandardProtocolFamily.UNIX);
            unixChannel.connect(UnixDomainSocketAddress.of(unixSocketPath));
            InputStream unixIn = Channels.newInputStream(unixChannel);
            OutputStream unixOut = Channels.newOutputStream(unixChannel);

            Thread t1 = new Thread(() -> pipe(client, unixOut, unixChannel), "pg-bridge-c2u");
            Thread t2 = new Thread(() -> pipeFromUnix(unixIn, client, unixChannel), "pg-bridge-u2c");
            t1.setDaemon(true);
            t2.setDaemon(true);
            t1.start();
            t2.start();
        } catch (IOException e) {
            try {
                client.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void pipe(Socket client, OutputStream unixOut, SocketChannel unixChannel) {
        try (InputStream in = client.getInputStream()) {
            in.transferTo(unixOut);
        } catch (IOException ignored) {
        } finally {
            closeQuietly(client, unixChannel);
        }
    }

    private void pipeFromUnix(InputStream unixIn, Socket client, SocketChannel unixChannel) {
        try (OutputStream out = client.getOutputStream()) {
            unixIn.transferTo(out);
        } catch (IOException ignored) {
        } finally {
            closeQuietly(client, unixChannel);
        }
    }

    private void closeQuietly(Socket client, SocketChannel channel) {
        try {
            client.close();
        } catch (IOException ignored) {
        }
        try {
            channel.close();
        } catch (IOException ignored) {
        }
    }
}
