package org.project.ttokttok.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.project.ttokttok.infrastructure.email.dto.ResultMailContent;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * #435 U3. 결과 메일을 보내는 도중 앱이 종료될 때 몇 통이 나가고 몇 통이 사라지는지 잰다.
 *
 * <p>실제 {@link AsyncConfig}와 {@link EmailService}(@Async 프록시)로 작은 스프링 컨텍스트를 띄운다. 가짜 SMTP는 한 통에
 * MAIL_DELAY_MS가 걸리고, 실제 소켓처럼 스레드 인터럽트에 반응하지 않는다. 배포는 종료 신호 뒤 10초에 강제 종료하므로
 * (deploy.sh: docker compose stop, stop_grace_period 없음) 컨텍스트 종료를 시작한 순간부터 10초 안에 나간 메일만 "발송"으로 센다.
 * 결과는 TTOKTTOK_SHUTDOWN_EXPERIMENT_OUT(JSON)에 남긴다. 단언 없이 측정만 한다.
 */
@EnabledIfEnvironmentVariable(named = "TTOKTTOK_SHUTDOWN_EXPERIMENT_OUT", matches = ".+")
class MailExecutorShutdownExperimentTest {

    private static final long MAIL_DELAY_MS = 25;      // 운영 Postfix delays a 실측(20~30ms)
    private static final long KILL_AFTER_MS = 10_000;  // docker compose stop 기본 대기
    private static final int RECIPIENTS_PER_BATCH = 100;
    private static final int LATE_BATCH_RECIPIENTS = 10;
    private static final long WARM_MS = 300;           // 종료 전에 작업이 돌기 시작하도록 기다리는 시간

    @Test
    void measure() throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conditions", Map.of(
                "mailDelayMs", MAIL_DELAY_MS,
                "killAfterMs", KILL_AFTER_MS,
                "recipientsPerBatch", RECIPIENTS_PER_BATCH,
                "lateBatchRecipients", LATE_BATCH_RECIPIENTS,
                "java", System.getProperty("java.version")));
        int repetitions = Integer.parseInt(System.getenv().getOrDefault("TTOKTTOK_REPETITIONS", "3"));
        for (int batches : List.of(4, 10)) {
            List<Object> reps = new ArrayList<>();
            for (int rep = 0; rep < repetitions; rep++) {
                reps.add(run(batches));
            }
            result.put(batches + " batches (" + batches * RECIPIENTS_PER_BATCH + " mails)", reps);
        }

        File out = new File(System.getenv("TTOKTTOK_SHUTDOWN_EXPERIMENT_OUT"));
        out.getAbsoluteFile().getParentFile().mkdirs();
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(out, result);
    }

    private Map<String, Object> run(int batches) throws Exception {
        RecordingSender sender = new RecordingSender();
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("experiment", Map.of(
                "email.from.address", "noreply@example.com",
                "email.from.name", "TtokTtok",
                "email.reply-to", "noreply@example.com")));
        ctx.registerBean(JavaMailSender.class, () -> sender);
        ctx.register(AsyncConfig.class, EmailService.class);
        ctx.refresh();
        EmailService emailService = ctx.getBean(EmailService.class);
        ThreadPoolTaskExecutor pool = ctx.getBean("mailExecutor", ThreadPoolTaskExecutor.class);

        for (int b = 0; b < batches; b++) {
            emailService.sendResultMail(recipients("b" + b, RECIPIENTS_PER_BATCH), new ResultMailContent("결과", "본문"));
        }
        Thread.sleep(WARM_MS);

        Map<String, Object> atClose = new LinkedHashMap<>();
        atClose.put("active", pool.getActiveCount());
        atClose.put("queued", pool.getThreadPoolExecutor().getQueue().size());
        atClose.put("sentBeforeClose", sender.sent.size());

        long t0 = System.nanoTime();
        sender.closeStartedAt = t0;
        Thread closer = new Thread(ctx::close, "closer");
        closer.start();

        // 종료가 시작된 직후 들어온 요청을 흉내 낸다(그레이스풀 셧다운 중 처리 중이던 요청)
        Thread.sleep(200);
        String lateOutcome;
        try {
            emailService.sendResultMail(recipients("late", LATE_BATCH_RECIPIENTS), new ResultMailContent("결과", "본문"));
            lateOutcome = "accepted";
        } catch (Exception e) {
            lateOutcome = "rejected: " + e.getClass().getSimpleName();
        }

        closer.join(TimeUnit.SECONDS.toMillis(60));
        long closeMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
        // 강제 종료 시점(10초) 이후에도 JVM 안에서 계속 도는 작업이 있으면 끝날 때까지 조금 더 지켜본다
        long waitUntil = t0 + TimeUnit.SECONDS.toNanos(30);
        int expected = batches * RECIPIENTS_PER_BATCH + LATE_BATCH_RECIPIENTS;
        while (System.nanoTime() < waitUntil && sender.sent.size() < expected) {
            Thread.sleep(100);
        }

        List<long[]> sends = new ArrayList<>(sender.sent); // {msSinceClose(-면 종료 전), late(1/0)}
        long beforeKill = sends.stream().filter(s -> s[0] <= KILL_AFTER_MS).count();
        long afterKill = sends.stream().filter(s -> s[0] > KILL_AFTER_MS).count();
        long lateSent = sends.stream().filter(s -> s[1] == 1).count();
        long lateBeforeKill = sends.stream().filter(s -> s[1] == 1 && s[0] <= KILL_AFTER_MS).count();
        long lastSendMs = sends.stream().mapToLong(s -> s[0]).max().orElse(-1);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("mailsRequested", batches * RECIPIENTS_PER_BATCH);
        r.put("atClose", atClose);
        r.put("contextCloseMs", closeMs);
        r.put("sentWithinKillWindow", beforeKill);
        r.put("sentAfterKillWindow (lost in real deploy)", afterKill);
        r.put("neverSent", expected - sends.size());
        r.put("lostInRealDeploy", expected - beforeKill);
        r.put("lastSendMsSinceClose", lastSendMs);
        r.put("lateBatch", Map.of("submit", lateOutcome, "sent", lateSent, "sentWithinKillWindow", lateBeforeKill,
                "requested", LATE_BATCH_RECIPIENTS));
        return r;
    }

    private static List<String> recipients(String prefix, int n) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(prefix + "-" + i + "@example.com");
        }
        return list;
    }

    /** 한 통에 MAIL_DELAY_MS가 걸리고, 인터럽트에도 끝까지 기다리는 가짜 발송기. 보낸 시각을 기록한다. */
    private static final class RecordingSender extends JavaMailSenderImpl {
        private final ConcurrentLinkedQueue<long[]> sent = new ConcurrentLinkedQueue<>();
        private volatile long closeStartedAt = Long.MAX_VALUE;

        @Override
        public MimeMessage createMimeMessage() {
            return new MimeMessage(Session.getInstance(new Properties()));
        }

        @Override
        protected void doSend(MimeMessage[] mimeMessages, Object[] originalMessages) {
            for (MimeMessage message : mimeMessages) {
                sleepUninterruptibly(MAIL_DELAY_MS);
                long now = System.nanoTime();
                long sinceClose = closeStartedAt == Long.MAX_VALUE ? -1 : TimeUnit.NANOSECONDS.toMillis(now - closeStartedAt);
                boolean late;
                try {
                    late = message.getAllRecipients()[0].toString().startsWith("late-");
                } catch (Exception e) {
                    late = false;
                }
                sent.add(new long[]{sinceClose, late ? 1 : 0});
            }
        }

        private static void sleepUninterruptibly(long ms) {
            long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(ms);
            boolean interrupted = false;
            while (true) {
                long left = end - System.nanoTime();
                if (left <= 0) {
                    break;
                }
                try {
                    TimeUnit.NANOSECONDS.sleep(left);
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
