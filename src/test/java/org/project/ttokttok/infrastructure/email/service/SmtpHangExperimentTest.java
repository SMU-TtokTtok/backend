package org.project.ttokttok.infrastructure.email.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.project.ttokttok.infrastructure.email.config.EmailConfig;
import org.project.ttokttok.infrastructure.email.dto.ResultMailContent;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * #435 U2. 연결은 받지만 응답하지 않는 SMTP 서버에 실제 {@link EmailConfig} 발송기로 보낼 때, 발송 스레드가 얼마나 묶이는지 잰다.
 *
 * <p>Postfix가 멈춘 상황을 흉내 낸다. 테스트 안의 {@link ServerSocket}이 연결을 받아 두기만 하고 인사말(220)을 보내지 않는다.
 * 실제 메일은 어디로도 나가지 않는다. 결과는 TTOKTTOK_SMTP_EXPERIMENT_OUT(JSON)에 남긴다. 단언 없이 측정만 한다.
 */
@EnabledIfEnvironmentVariable(named = "TTOKTTOK_SMTP_EXPERIMENT_OUT", matches = ".+")
class SmtpHangExperimentTest {

    private static final long LIMIT_SECONDS = Long.parseLong(System.getenv().getOrDefault("TTOKTTOK_SMTP_LIMIT_SECONDS", "60"));
    private static final int BATCH_RECIPIENTS = 3;

    @Test
    void measure() throws Exception {
        try (HungSmtpServer server = new HungSmtpServer()) {
            JavaMailSenderImpl sender = sender(server.port());
            EmailService emailService = emailService(sender);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("conditions", Map.of(
                    "limitSeconds", LIMIT_SECONDS,
                    "batchRecipients", BATCH_RECIPIENTS,
                    "smtpProperties", timeoutProperties(sender),
                    "java", System.getProperty("java.version")));

            // 한 통: 인사말을 기다리다 멈추는지, 몇 초 뒤 풀리는지
            result.put("singleMail", timed(() -> {
                var message = sender.createMimeMessage();
                message.setFrom("noreply@example.com");
                message.setRecipients(jakarta.mail.Message.RecipientType.TO, "a@example.com");
                message.setSubject("hang");
                message.setText("hang");
                sender.send(message);
                return null;
            }));

            // 결과 메일 한 묶음: 수신자마다 한 통씩 순서대로 보내므로 묶음 전체가 스레드를 얼마나 잡는지
            List<String> recipients = new ArrayList<>();
            for (int i = 0; i < BATCH_RECIPIENTS; i++) {
                recipients.add("r" + i + "@example.com");
            }
            result.put("resultMailBatch", timed(() -> {
                ResultMailContent content = new ResultMailContent("결과", "본문");
                for (String recipient : recipients) {
                    try {
                        emailService.sendResultEmail(recipient, content);
                    } catch (RuntimeException e) {
                        // 이전 묶음 발송처럼 실패해도 다음 수신자를 이어서 보낸다
                    }
                }
                return null;
            }));
            result.put("connectionsAccepted", server.accepted());

            File out = new File(System.getenv("TTOKTTOK_SMTP_EXPERIMENT_OUT"));
            out.getAbsoluteFile().getParentFile().mkdirs();
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(out, result);
        }
    }

    /** 별도 스레드에서 실행하고 LIMIT_SECONDS까지만 기다린다. 끝나지 않으면 "아직 막힘"으로 기록하고 스레드를 끊는다. */
    private Map<String, Object> timed(java.util.concurrent.Callable<Void> action) throws InterruptedException {
        ExecutorService runner = Executors.newSingleThreadExecutor();
        long start = System.nanoTime();
        Future<Void> future = runner.submit(action);
        Map<String, Object> r = new LinkedHashMap<>();
        try {
            future.get(LIMIT_SECONDS, TimeUnit.SECONDS);
            r.put("outcome", "returned");
        } catch (TimeoutException e) {
            r.put("outcome", "still blocked at limit");
            future.cancel(true);
        } catch (java.util.concurrent.ExecutionException e) {
            r.put("outcome", "failed");
            r.put("exception", e.getCause().getClass().getName());
            Throwable root = e.getCause();
            while (root.getCause() != null) {
                root = root.getCause();
            }
            r.put("rootCause", root.getClass().getName());
        }
        r.put("elapsedMs", Math.round((System.nanoTime() - start) / 1_000_000.0));
        runner.shutdownNow();
        runner.awaitTermination(5, TimeUnit.SECONDS);
        return r;
    }

    private static JavaMailSenderImpl sender(int port) {
        EmailConfig config = new EmailConfig();
        ReflectionTestUtils.setField(config, "host", "127.0.0.1");
        ReflectionTestUtils.setField(config, "port", port);
        ReflectionTestUtils.setField(config, "username", "");
        ReflectionTestUtils.setField(config, "password", "");
        return (JavaMailSenderImpl) config.javaMailSender();
    }

    private static EmailService emailService(JavaMailSenderImpl sender) {
        EmailService service = new EmailService(sender);
        ReflectionTestUtils.setField(service, "fromAddress", "noreply@example.com");
        ReflectionTestUtils.setField(service, "fromName", "TtokTtok");
        ReflectionTestUtils.setField(service, "replyTo", "noreply@example.com");
        return service;
    }

    private static Map<String, String> timeoutProperties(JavaMailSenderImpl sender) {
        Properties p = sender.getJavaMailProperties();
        Map<String, String> m = new LinkedHashMap<>();
        for (String key : List.of("mail.smtp.connectiontimeout", "mail.smtp.timeout", "mail.smtp.writetimeout")) {
            m.put(key, p.getProperty(key, "(unset)"));
        }
        return m;
    }

    /** 연결을 받아 열어 두기만 하고 아무 응답도 보내지 않는 서버. */
    private static final class HungSmtpServer implements AutoCloseable {
        private final ServerSocket socket = new ServerSocket(0);
        private final List<Socket> held = new CopyOnWriteArrayList<>();
        private final Thread acceptor;

        HungSmtpServer() throws IOException {
            acceptor = new Thread(() -> {
                while (!socket.isClosed()) {
                    try {
                        held.add(socket.accept());
                    } catch (IOException e) {
                        return;
                    }
                }
            }, "hung-smtp");
            acceptor.setDaemon(true);
            acceptor.start();
        }

        int port() {
            return socket.getLocalPort();
        }

        int accepted() {
            return held.size();
        }

        @Override
        public void close() throws IOException {
            socket.close();
            for (Socket s : held) {
                s.close();
            }
        }
    }
}
